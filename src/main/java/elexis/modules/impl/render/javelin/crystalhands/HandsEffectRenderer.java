package elexis.modules.impl.render.javelin.crystalhands;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import elexis.client.utils.client.IMinecraft;
import elexis.client.utils.render.main.newrender.gl.GlState;
import elexis.client.utils.render.main.newrender.gl.ResourceUtils;
import elexis.client.utils.render.main.newrender.gl.ShaderProgram;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.modules.impl.render.Hands;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.item.Item;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.*;
import java.util.*;
import java.util.regex.*;

/**
 * Native framebuffer adapter for Javelin's hand shaders.
 * The hand commands write their silhouette into a private depth attachment.
 * Scene colour is only sampled for the actual hand pixels and mirror effects.
 */
public final class HandsEffectRenderer implements IMinecraft {
    private static final String ROOT="assets/javelin/shaders/core/";
    private static final String VERTEX="""
            #version 330
            out vec2 texCoord;
            out vec2 TexCoord;
            void main() {
                vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);
                texCoord=p; TexCoord=p;
                gl_Position=vec4(p*2.0-1.0,0,1);
            }
            """;
    private static final String EXTRACT="""
            #version 330
            in vec2 texCoord; out vec4 fragColor;
            uniform sampler2D After,HandDepth;
            uniform int MaskOnly;
            void main() {
                ivec2 pixel=ivec2(gl_FragCoord.xy);
                float depth=texelFetch(HandDepth,pixel,0).r;
                float mask=depth>=0.0 && depth<1.0?1.0:0.0;
                vec3 source=texelFetch(After,pixel,0).rgb;
                fragColor=MaskOnly==1?vec4(mask):vec4(source*mask,mask);
            }
            """;
    private static final String BLUR="""
            #version 330
            in vec2 texCoord; out vec4 fragColor;
            uniform sampler2D Source;
            uniform vec2 Direction;
            void main() {
                vec4 c=texture(Source,texCoord)*0.227027;
                c+=(texture(Source,texCoord+Direction*1.384615)+texture(Source,texCoord-Direction*1.384615))*0.316216;
                c+=(texture(Source,texCoord+Direction*3.230769)+texture(Source,texCoord-Direction*3.230769))*0.070270;
                fragColor=c;
            }
            """;
    private static final String EMIT="""
            #version 330
            in vec2 texCoord; out vec4 fragColor;
            uniform sampler2D Source;
            uniform vec4 Color1,Color2;
            uniform float Strength,ItemColor;
            void main() {
                vec4 b=texture(Source,texCoord);
                vec3 colour=mix(Color1.rgb,Color2.rgb,texCoord.y);
                if(ItemColor>0.5) colour=b.rgb;
                float a=clamp(b.a*Strength,0.0,0.95);
                fragColor=vec4(colour*a,a);
            }
            """;
    private static final Map<String,ShaderProgram> PROGRAMS=new HashMap<>();
    private static final List<Integer> TEXTURES=new ArrayList<>();
    private static final GlState.Snapshot STATE=new GlState.Snapshot();
    private static final int[] BINDINGS=new int[8],SAMPLERS=new int[8];
    private static final float[] TRANSPARENT={0,0,0,0};
    private static int before,after,hand,mask,bloom,work,history,next,fireGlow,sceneBlur;
    private static GpuTexture handDepth;
    private static GpuTextureView handColourView,handDepthView;
    private static int readFbo,drawFbo,vao,sampler,width,height;
    private static Object world;
    private static Hands owner;
    private static Item mainItem,offItem;
    private static String effectKey;
    private static boolean failed;
    private static long previousFrame,failureAt;
    private static float lastYaw,lastPitch,hitFade;
    private static boolean wasSwinging;
    private HandsEffectRenderer() {}

    public static void render(Hands module, Item main, Item off, Runnable drawHands) {
        if (!capture(module,main,off)) {
            drawHands.run();
            return;
        }
        var previousColour=RenderSystem.outputColorTextureOverride;
        var previousDepth=RenderSystem.outputDepthTextureOverride;
        RenderSystem.outputColorTextureOverride=handColourView;
        RenderSystem.outputDepthTextureOverride=handDepthView;
        try {
            drawHands.run();
        } finally {
            RenderSystem.outputColorTextureOverride=previousColour;
            RenderSystem.outputDepthTextureOverride=previousDepth;
        }
        var framebuffer=mc.getFramebuffer();
        if (framebuffer.textureWidth!=width || framebuffer.textureHeight!=height) return;
        try {
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(handDepth,framebuffer.getDepthAttachment(),
                    0,0,0,0,0,width,height);
            composite(module);
        } catch (RuntimeException error) { fail(error); }
    }

    private static boolean capture(Hands module, Item main, Item off) {
        if (mc.world==null || mc.player==null || !mc.options.getPerspective().isFirstPerson()) return false;
        if (!module.isGlassMode() && !module.needsCapture()) return false;
        if (RenderSystem.outputDepthTextureOverride!=null || RenderSystem.outputColorTextureOverride!=null) return false;
        if (failed && System.nanoTime()-failureAt<5_000_000_000L) return false;
        var framebuffer=mc.getFramebuffer();
        if (!(framebuffer.getColorAttachment() instanceof GlTexture colour) || !(framebuffer.getDepthAttachment() instanceof GlTexture depth)) return false;
        try {
            String key=module.effect.get()+"/"+module.mode.get()+"/"+module.trailMode.get();
            if (owner!=module || world!=mc.world || !Objects.equals(effectKey,key)) {
                release();
                owner=module; world=mc.world; effectKey=key;
            }
            ensureAttachments(colour,depth.getFormat(),framebuffer.textureWidth,framebuffer.textureHeight);
            var state=save();
            try {
                ensure(framebuffer.textureWidth,framebuffer.textureHeight);
                if (mainItem!=main || offItem!=off) {
                    clearHistory();
                    mainItem=main;
                    offItem=off;
                }
                copyColour(colour.getGlId(),before);
            } finally { restore(state); }
            // This call changes the depth-write state through Minecraft's cache.
            // Run it after restoring raw GL state so the native hand draw sees the same state.
            RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(handDepth,1);
            failed=false;
            return true;
        } catch (RuntimeException error) { fail(error); return false; }
    }

    private static void composite(Hands module) {
        var framebuffer=mc.getFramebuffer();
        if (!(framebuffer.getColorAttachment() instanceof GlTexture colour)) return;
        var state=save();
        boolean replaced=false;
        try {
            copyColour(colour.getGlId(),after);

            ShaderProgram extraction=program("extract",EXTRACT);
            extraction.use();
            bind(extraction,"After",after,0);
            bind(extraction,"HandDepth",((GlTexture)handDepth).getGlId(),1);
            GL11.glDisable(GL11.GL_BLEND);
            integer(extraction,"MaskOnly",0); target(hand); draw();
            integer(extraction,"MaskOnly",1); target(mask); draw();

            blur(hand,bloom,Math.max(1,module.getGlowBlur())*1.5f,2);
            float time=module.getAnimationTime();
            long now=System.nanoTime();
            float dt=previousFrame==0?1/60f:Math.min(.1f,(now-previousFrame)*1e-9f);
            previousFrame=now;

            if (module.isGlassMode()) {
                glassPrepare(module);
            } else if (module.isMirrorMode()) {
                blur(before,sceneBlur,module.getMirrorBlur()*3.8f,2);
            }
            if (!module.isGlassMode() && module.isTrailEnabled()) updateTrail(module,time,dt);

            // Preserve vanilla pixels from layers that don't write depth.
            replaced=true;
            target(colour.getGlId());
            translucent();

            if (module.isGlassMode()) renderGlass(module,time,dt);
            else {
                if (module.isTrailEnabled()) {
                    ShaderProgram blit=original("dimas_hands/hands_trail_blit");
                    blit.use(); bind(blit,"TrailSampler",history,0); draw();
                }
                renderEdges(module);
                ShaderProgram composite=original("dimas_hands/hands_composite");
                composite.use();
                handsData(composite,module);
                bind(composite,"HandSampler",hand,0);
                bind(composite,"HandDepthSampler",((GlTexture)handDepth).getGlId(),1);
                bind(composite,"HandMaskSampler",mask,2);
                bind(composite,"HandSupportSampler",mask,3);
                bind(composite,"BlurredSceneSampler",sceneBlur,4);
                bind(composite,"BloomSampler",bloom,5);
                translucent(); draw();
            }
        } catch (RuntimeException error) {
            if (replaced) copyColour(after,colour.getGlId());
            fail(error);
        } finally { restore(state); }
    }

    private static void handsData(ShaderProgram p,Hands m) {
        vector(p,"Params0",m.getRenderMode(),m.getShaderStyle(),m.getAnimationTime(),m.getShaderSpeed());
        vector(p,"Params1",m.getShaderScale(),m.getShaderGlow(),m.getShaderAlpha(),m.getShaderShadow());
        vector(p,"Params2",m.hasStars()?1:0,m.isShaderShading()?1:0,m.isRainbow()?1:0,m.isFillShading()?1:0);
        vector(p,"Params3",m.getFillAlpha(),m.getFillShadow(),m.getRainbowSpeed(),m.getRainbowScale());
        vector(p,"Params4",m.getMirrorMix(),m.getMirrorDistortion(),m.isGlowEnabled()?1:0,m.getGlowBlur());
        vector(p,"Params5",m.getGlowBrightness(),m.isItemColor()?1:0,0,m.getInnerGlow());
        vector(p,"Params6",(float)width/height,(float)Math.tan(Math.toRadians(mc.options.getFov().getValue())*.5),m.getQualitySamples(),m.getMirrorBlur());
        color(p,"ShaderColor1",m.getShaderColor()); color(p,"ShaderColor2",m.getShaderColor2());
        color(p,"FillColor",m.getFillColor()); color(p,"MirrorColor",m.getMirrorColor());
        color(p,"GlowColor1",m.getGlowColor()); color(p,"GlowColor2",m.getGlowColor2());
        pair(p,"Size",width,height);
    }

    private static void renderEdges(Hands m) {
        if (m.isGlowEnabled()) edgePass("dimas_hands/hands_outer_glow",m,m.getGlowBrightness(),m.getOutlineColor(),0);
        if (m.isOutlineEnabled()) edgePass("dimas_hands/hands_outline",m,m.getOutlineThickness(),m.getOutlineColor(),1);
    }
    private static void edgePass(String name,Hands m,float strength,int outline,int mode) {
        ShaderProgram p=original(name); p.use();
        bind(p,"BloomSampler",bloom,0); bind(p,"HandMaskSampler",mask,1); bind(p,"HandSampler",hand,2);
        vector(p,"Params",strength,m.isItemColor()?1:0,0,0);
        color(p,"GlowColor1",m.getGlowColor()); color(p,"GlowColor2",m.getGlowColor2()); color(p,"OutlineColor",outline);
        vector(p,"Effects",width,height,m.getOutlineThickness(),mode);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE,GL11.GL_ZERO,GL11.GL_ONE);
        draw(); translucent();
    }

    private static void clearHistory() {
        if (history == 0) return;
        target(history);
        GL30.glClearBufferfv(GL11.GL_COLOR,0,TRANSPARENT);
        target(next);
        GL30.glClearBufferfv(GL11.GL_COLOR,0,TRANSPARENT);
        hitFade = 0;
    }

    private static void updateTrail(Hands m,float time,float dt) {
        boolean energy=m.isEnergyTrail(),ribbon=m.isRibbonTrail();
        float lifetime=ribbon?m.getRibbonLifetime():energy?m.getEnergyLifetime():1f;
        float strength=1;
        if (ribbon && mc.player.handSwinging && m.isRibbonSwingResponse()) {
            lifetime=m.getRibbonSwingLifetime()>0?m.getRibbonSwingLifetime():lifetime;
            strength=m.getRibbonSwingPower();
        } else if (ribbon && mc.player.isUsingItem() && m.isRibbonEatingResponse()) {
            lifetime=m.getRibbonEatingLifetime(); strength=m.getRibbonEatingPower();
        }
        boolean swinging=mc.player.handSwinging;
        if (!wasSwinging && swinging && m.isHitFade()) hitFade=1;
        wasSwinging=swinging;

        float fade=(ribbon?.045f/Math.max(.15f,lifetime)
                :energy?.048f/Math.max(.4f,lifetime)
                :Math.max(0.06f, m.getNormalFade()))
                +hitFade*m.getHitFadeMul()*.012f;

        hitFade=Math.max(0,hitFade-dt/.45f);

        float rise=ribbon?m.getRibbonRise():energy?.035f*m.getEnergyTurbulence():m.getNormalRise();
        float sway=ribbon?0:energy?.018f*m.getEnergyTurbulence():m.getNormalSway();
        float inertia=ribbon?m.getRibbonInertia():energy?m.getEnergyInertia():m.getNormalInertia();

        float yaw=mc.gameRenderer.getCamera().getYaw(),pitch=mc.gameRenderer.getCamera().getPitch();
        float dYaw=MathHelper.wrapDegrees(yaw-lastYaw);
        float dPitch=pitch-lastPitch;

        // Clear history on camera teleport (protection against stretching)
        if (Math.abs(dYaw)>60f || Math.abs(dPitch)>60f || mc.world==null) {
            clearHistory();
            lastYaw=yaw; lastPitch=pitch;
            dYaw=0; dPitch=0;
        } else {
            lastYaw=yaw; lastPitch=pitch;
        }

        float dx=MathHelper.clamp(-dYaw/mc.options.getFov().getValue()*inertia,-.02f,.02f);
        float dy=MathHelper.clamp(dPitch/mc.options.getFov().getValue()*inertia,-.02f,.02f);

        ShaderProgram displacement=original("dimas_hands/hands_trail_"+(ribbon?"ribbon_":energy?"energy_":"")+"displace");
        displacement.use(); bind(displacement,"TrailSampler",history,0);
        pair(displacement,"Offset",dx+(float)Math.sin(time*5.0265)*sway*dt,dy+rise*dt);
        pair(displacement,"TextureSize",width,height);
        vector(displacement,"Params",fade,time,dt,ribbon?m.getRibbonWobble():energy?.32f*m.getEnergyTurbulence():m.getNormalTurbulence());
        scalar(displacement,"Flicker",m.getNormalFlicker());
        vector(displacement,"MotionParams",m.getRibbonSpeed(),1,0,0);
        target(next); GL11.glDisable(GL11.GL_BLEND); draw();

        ShaderProgram emission;
        if (energy || ribbon) {
            emission=original("dimas_hands/hands_trail_"+(ribbon?"ribbon":"energy"));
            emission.use(); bind(emission,"BloomSampler",bloom,0);
            vector(emission,"Params",m.getGlowBrightness()*strength*(ribbon?m.getRibbonIntensity():1),ribbon?time:m.isItemColor()?1:0,dt,m.getRibbonWidth());
            vector(emission,"RibbonParams",m.getRibbonWobble(),(float)width/height,m.isItemColor()?1:0,0);
            vector(emission,"EnergyParams",time,m.getEnergyThickness(),m.getEnergySmoothness(),0);
            color(emission,"GlowColor1",m.getGlowColor());color(emission,"GlowColor2",m.getGlowColor2());
        } else {
            emission=program("emit",EMIT); emission.use(); bind(emission,"Source",bloom,0);
            color(emission,"Color1",m.getGlowColor());color(emission,"Color2",m.getGlowColor2());
            scalar(emission,"Strength",Math.min(1,dt*6)*m.getGlowBrightness());scalar(emission,"ItemColor",m.isItemColor()?1:0);
        }
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
        draw();
        int swap=history;history=next;next=swap;
    }

    private static void glassPrepare(Hands m) {
        HandsGlassSettings g=m.glass;
        if (g.enableBlur.get()) blur(before,sceneBlur,g.blurRadius.get(),g.blurIterations.get().intValue());
        else copyColour(before,sceneBlur);
    }
    private static void renderGlass(Hands m,float time,float dt) {
        HandsGlassSettings g=m.glass;
        ShaderProgram p=original("bagaruki_glass_composite");p.use();
        bind(p,"SceneSampler",before,0);bind(p,"BlurSampler",sceneBlur,1);bind(p,"MaskSampler",hand,2);
        vector(p,"ResolutionSatReflect",width,height,g.saturation.get(),1);
        color(p,"TintColor",g.colorMode.is("Custom")?g.tintColor.get():m.getMirrorColor());
        vector(p,"Settings",g.tintIntensity.get(),g.enableEdgeGlow.get()?g.edgeGlowIntensity.get():0,g.edgeSoftness.get(),0);
        vector(p,"IceParams",time,g.enableIce.get()?g.iceIntensity.get():0,g.refractReach.get(),g.enableIce.get()?.4f:0);
        vector(p,"SmokeParams",0,1,1,0);
        translucent();draw();
        if (g.enableEdgeGlow.get()) {
            int edge=g.edgeGlowColorMode.is("Custom")?g.edgeGlowCustom.get():m.getMirrorColor();
            float shimmer=g.shimmer.get()?Math.max(0,1-Math.abs((time%g.shimmerPeriod.get())/g.shimmerPeriod.get()-.5f)/g.shimmerWidth.get()):0;
            edgePass("dimas_hands/hands_outline",m,g.edgeGlowIntensity.get()*(1+shimmer),edge,1);
        }
        if (g.enableFire.get()) renderGlassFire(m,time,dt);
    }
    private static void renderGlassFire(Hands m,float time,float dt) {
        HandsGlassSettings g=m.glass;
        int colour=g.fireColorMode.is("Custom")?g.fireCustomColor.get():m.getMirrorColor();
        ShaderProgram glow=original("bagaruki_burn_glow");
        glow.use();bind(glow,"HandSampler",hand,0);bind(glow,"MaskSampler",hand,1);
        burnData(glow,g,colour,time);target(fireGlow);GL11.glDisable(GL11.GL_BLEND);draw();
        ShaderProgram trail=original("bagaruki_burn_trail");
        trail.use();bind(trail,"TrailSampler",history,0);bind(trail,"GlowSampler",fireGlow,1);bind(trail,"HandSampler",hand,2);bind(trail,"MaskSampler",hand,3);
        burnData(trail,g,colour,time);target(next);draw();
        int swap=history;history=next;next=swap;
        ShaderProgram finish=original("bagaruki_burn_composite");finish.use();
        bind(finish,"MaskSampler",hand,0);bind(finish,"GlowSampler",fireGlow,1);bind(finish,"TrailSampler",history,2);
        burnData(finish,g,colour,time);
        target(((GlTexture)mc.getFramebuffer().getColorAttachment()).getGlId());
        GL11.glEnable(GL11.GL_BLEND);GL14.glBlendFuncSeparate(GL11.GL_ONE,GL11.GL_ONE,GL11.GL_ZERO,GL11.GL_ONE);draw();translucent();
    }
    private static void burnData(ShaderProgram p,HandsGlassSettings g,int colour,float time) {
        vector(p,"ResolutionTime",width,height,time,g.fireBurnThrough.get()?1:0);
        vector(p,"FireParams",g.fireRadius.get(),g.fireIntensity.get(),g.fireSpeed.get(),g.fireColorMode.is("Item")?0:1);
        vector(p,"TrailParams",.965f,g.fireHeight.get(),g.fireSpeed.get(),g.fireTrail.get());
        vector(p,"FireColor",(colour>>16&255)/255f,(colour>>8&255)/255f,(colour&255)/255f,g.fireIntensity.get());
    }

    private static void ensureAttachments(GpuTexture colour,TextureFormat depthFormat,int w,int h) {
        if (w<=0 || h<=0) throw new IllegalStateException("Empty hands framebuffer");
        if (handColourView!=null && handColourView.texture()==colour && handDepthView!=null
                && handDepth.getWidth(0)==w && handDepth.getHeight(0)==h && handDepth.getFormat()==depthFormat) return;
        releaseAttachments();
        var device=RenderSystem.getDevice();
        handDepth=device.createTexture("Elexis hand depth",GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING
                | GpuTexture.USAGE_COPY_SRC,depthFormat,w,h,1,1);
        handDepthView=device.createTextureView(handDepth);
        // Own the framebuffer cache for this colour/depth pair. Recreating a depth texture
        // can reuse its GL id, which would otherwise hit an old entry in the game's colour view.
        handColourView=device.createTextureView(colour);
    }

    private static void ensure(int w,int h) {
        if (width==w && height==h && mask!=0) return;
        releaseTextures();
        width=w;
        height=h;
        readFbo=GL30.glGenFramebuffers();drawFbo=GL30.glGenFramebuffers();vao=GL30.glGenVertexArrays();
        sampler=GL33.glGenSamplers();
        GL33.glSamplerParameteri(sampler,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);
        GL33.glSamplerParameteri(sampler,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
        GL33.glSamplerParameteri(sampler,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);
        GL33.glSamplerParameteri(sampler,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
        GL33.glSamplerParameteri(sampler,GL14.GL_TEXTURE_COMPARE_MODE,GL11.GL_NONE);
        before=texture();after=texture();
        hand=texture();mask=texture();bloom=texture();work=texture();
        history=texture();next=texture();fireGlow=texture();sceneBlur=texture();
        clearHistory();
        lastYaw=mc.player.getYaw();lastPitch=mc.player.getPitch();
    }
    private static int texture() {
        int id=GL11.glGenTextures();TEXTURES.add(id);GL11.glBindTexture(GL11.GL_TEXTURE_2D,id);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,width,height,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,0L);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
        return id;
    }
    private static void copyColour(int source,int destination) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,readFbo);
        GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,source,0);
        GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,drawFbo);
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,destination,0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL30.glBlitFramebuffer(0,0,width,height,0,0,width,height,GL11.GL_COLOR_BUFFER_BIT,GL11.GL_NEAREST);
    }
    private static void target(int texture) {
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,drawFbo);
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER,GL30.GL_DEPTH_ATTACHMENT,GL11.GL_TEXTURE_2D,0,0);
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,texture,0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);GL11.glViewport(0,0,width,height);
        GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);GL11.glColorMask(true,true,true,true);GL11.glDepthMask(false);
    }
    private static void draw() { GL30.glBindVertexArray(vao);GL11.glDrawArrays(GL11.GL_TRIANGLES,0,3); }
    private static void translucent() { GL11.glEnable(GL11.GL_BLEND);GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA); }
    private static void blur(int source,int destination,float radius,int iterations) {
        ShaderProgram p=program("blur",BLUR);p.use();GL11.glDisable(GL11.GL_BLEND);
        int input=source;
        for (int i=0;i<Math.max(1,Math.min(8,iterations));i++) {
            bind(p,"Source",input,0);pair(p,"Direction",radius/width,0);target(work);draw();
            bind(p,"Source",work,0);pair(p,"Direction",0,radius/height);target(destination);draw();input=destination;
        }
    }
    private static ShaderProgram original(String name) {
        return PROGRAMS.computeIfAbsent(name,key->new ShaderProgram(VERTEX,adapt(ResourceUtils.readText(ROOT+name+".fsh"))));
    }
    private static String adapt(String shader) {
        Matcher imports=Pattern.compile("#moj_import\\s+<javelin:([^>]+)>").matcher(shader);
        StringBuffer resolved=new StringBuffer();
        while(imports.find()) imports.appendReplacement(resolved,Matcher.quoteReplacement(ResourceUtils.readText("assets/javelin/shaders/include/"+imports.group(1))));
        imports.appendTail(resolved);
        Matcher blocks=Pattern.compile("layout\\s*\\(std140\\)\\s*uniform\\s+\\w+\\s*\\{(.*?)\\};",Pattern.DOTALL).matcher(resolved.toString());
        StringBuffer result=new StringBuffer();
        while(blocks.find()) {
            String body=blocks.group(1).replaceAll("//[^\\n]*","");
            StringBuilder uniforms=new StringBuilder();
            for(String member:body.split(";")) if(!member.isBlank()) uniforms.append("uniform ").append(member.strip()).append(";\n");
            blocks.appendReplacement(result,Matcher.quoteReplacement(uniforms.toString()));
        }
        blocks.appendTail(result);
        return result.toString().replace("#version 150","#version 330");
    }
    private static ShaderProgram program(String key,String fragment) { return PROGRAMS.computeIfAbsent(key,k->new ShaderProgram(VERTEX,fragment)); }
    private static void bind(ShaderProgram p,String name,int texture,int unit) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0+unit);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);GL33.glBindSampler(unit,sampler);integer(p,name,unit);
    }
    private static void scalar(ShaderProgram p,String n,float x) { GL20.glUniform1f(p.getUniformLocation(n),x); }
    private static void integer(ShaderProgram p,String n,int x) { GL20.glUniform1i(p.getUniformLocation(n),x); }
    private static void pair(ShaderProgram p,String n,float x,float y) { GL20.glUniform2f(p.getUniformLocation(n),x,y); }
    private static void vector(ShaderProgram p,String n,float x,float y,float z,float w) { GL20.glUniform4f(p.getUniformLocation(n),x,y,z,w); }
    private static void color(ShaderProgram p,String n,int c) { vector(p,n,(c>>16&255)/255f,(c>>8&255)/255f,(c&255)/255f,(c>>>24)/255f); }
    private static GlState.Snapshot save() {
        var state=GlState.push(STATE);
        for(int i=0;i<BINDINGS.length;i++) { GL13.glActiveTexture(GL13.GL_TEXTURE0+i);BINDINGS[i]=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);SAMPLERS[i]=GL11.glGetInteger(GL33.GL_SAMPLER_BINDING); }
        return state;
    }
    private static void restore(GlState.Snapshot state) {
        for(int i=0;i<BINDINGS.length;i++) { GL13.glActiveTexture(GL13.GL_TEXTURE0+i);GL11.glBindTexture(GL11.GL_TEXTURE_2D,BINDINGS[i]);GL33.glBindSampler(i,SAMPLERS[i]); }
        GlState.pop(state);
    }
    private static void fail(RuntimeException error) {
        failed=true;failureAt=System.nanoTime();
        System.err.println("[Elexis Javelin Hands] "+error.getMessage());
    }
    private static void releaseAttachments() {
        if (handColourView!=null) handColourView.close();
        if (handDepthView!=null) handDepthView.close();
        if (handDepth!=null) handDepth.close();
        handColourView=null;
        handDepthView=null;
        handDepth=null;
    }
    private static void releaseTextures() {
        for(int id:TEXTURES) GL11.glDeleteTextures(id);TEXTURES.clear();
        if(readFbo!=0)GL30.glDeleteFramebuffers(readFbo);
        if(drawFbo!=0)GL30.glDeleteFramebuffers(drawFbo);
        if(vao!=0)GL30.glDeleteVertexArrays(vao);
        if(sampler!=0)GL33.glDeleteSamplers(sampler);
        readFbo=drawFbo=vao=sampler=width=height=mask=0;
        previousFrame=0;hitFade=0;wasSwinging=false;
    }
    public static void release() {
        releaseAttachments();
        releaseTextures();
        for(var p:PROGRAMS.values())p.delete();PROGRAMS.clear();
        world=null;owner=null;effectKey=null;mainItem=offItem=null;failed=false;
    }
}
