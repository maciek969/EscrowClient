package elexis.modules.impl.render.visual;

import elexis.client.utils.client.IMinecraft;
import elexis.client.utils.render.main.newrender.gl.GlState;
import elexis.client.utils.render.main.newrender.gl.ShaderProgram;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;

/** Sky precedes terrain. Depth effects share one optional scene copy and draw. */
public final class VisualPostProcessor implements IMinecraft {
    private static ShaderProgram program,skyProgram;
    private static int sceneTexture,sceneFbo,outputFbo,vao,width,height,depthSampler;
    private static final Matrix4f vp=new Matrix4f(),inverse=new Matrix4f(),previousVP=new Matrix4f();
    private static final GlState.Snapshot state=new GlState.Snapshot();
    private static final int[] textures=new int[2],samplers=new int[2];
    private static Vec3d previousCamera;
    private static ClientWorld world;
    private static boolean failed,frameReady;
    private static long failureTime,lastUse;
    private static final long START=System.nanoTime();
    private static float skyRed=0.25f,skyGreen=0.35f,skyBlue=0.45f;
    public static long sceneDraws,skyDraws;
    private VisualPostProcessor() {}
    public static void beginFrame(Matrix4f view,Matrix4f projection,Vector4f fogColor) {
        if(mc.world==null) {frameReady=false;return;}
        vp.set(projection).mul(view);inverse.set(vp).invert();frameReady=true;
        skyRed=Math.max(0,fogColor.x);skyGreen=Math.max(0,fogColor.y);skyBlue=Math.max(0,fogColor.z);
    }
    private static boolean available() {
        if(!frameReady || mc.world==null || mc.player==null) return false;
        if(failed && System.nanoTime()-failureTime<5_000_000_000L) return false;
        if(failed) {release();failed=false;}
        return true;
    }
    /** Invoked after vanilla top sky, before sun, clouds, weather and terrain. */
    public static void renderSky() {
        var mist=SceneVisuals.active("ShaderMist",SceneVisuals.ShaderMist.class);
        if(mist==null || mist.intensity.get()<=0 || !available()) return;
        if(!(mc.getFramebuffer().getColorAttachment() instanceof GlTexture color)) return;
        var snapshot=GlState.push(state);
        try {
            ensureCommon();
            if(skyProgram==null) skyProgram=ShaderProgram.fromResources("assets/elexis/shaders/visual/scene.vert","assets/elexis/shaders/visual/sky.frag");
            target(color,mc.getFramebuffer().textureWidth,mc.getFramebuffer().textureHeight);
            GL11.glEnable(GL11.GL_BLEND);GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO);
            skyProgram.use();matrix(skyProgram,"inverseVP",inverse);
            int c=mist.color.get();GL20.glUniform3f(skyProgram.getUniformLocation("mistColor"),((c>>16)&255)/255f,((c>>8)&255)/255f,(c&255)/255f);
            GL20.glUniform3f(skyProgram.getUniformLocation("mist"),mist.intensity.get(),mist.speed.get(),time());
            GL30.glBindVertexArray(vao);GL11.glDrawArrays(GL11.GL_TRIANGLES,0,3);skyDraws++;lastUse=System.nanoTime();
        } catch(RuntimeException error) {failure(error);} finally {GlState.pop(snapshot);}
    }
    public static void render() {
        if(!available()) return;
        var grade=SceneVisuals.active("Saturation",SceneVisuals.Saturation.class);
        var motion=SceneVisuals.active("MotionBlur",SceneVisuals.MotionBlur.class);
        var fog=SceneVisuals.active("FogAtmosphere",SceneVisuals.FogAtmosphere.class);
        var rain=SceneVisuals.active("Rain",SceneVisuals.Rain.class);
        var beautiful=SceneVisuals.active("Beautiful",SceneVisuals.Beautiful.class);
        float gradient=mc.world.getRainGradient(mc.getRenderTickCounter().getTickProgress(true));
        float wet=rain==null?0:rain.wetness.get()*(rain.rainOnly.get()?gradient:1);
        if(beautiful!=null) wet=Math.max(wet,beautiful.reflections.get()*gradient);
        int count=SceneShockwaves.prune();
        if(grade==null && motion==null && fog==null && wet<=0 && count==0) {previousCamera=null;trimIfIdle();return;}
        var fb=mc.getFramebuffer();
        if(!(fb.getColorAttachment() instanceof GlTexture color) || !(fb.getDepthAttachment() instanceof GlTexture depth)) return;
        int w=fb.textureWidth,h=fb.textureHeight;if(w<=0 || h<=0) return;
        var snapshot=GlState.push(state);int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        for(int i=0;i<2;i++) {GL13.glActiveTexture(GL13.GL_TEXTURE0+i);textures[i]=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);samplers[i]=GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);}
        try {
            ensure(w,h);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,outputFbo);GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,color.getGlId(),0);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,sceneFbo);GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);GL30.glBlitFramebuffer(0,0,w,h,0,0,w,h,GL11.GL_COLOR_BUFFER_BIT,GL11.GL_NEAREST);
            target(color,w,h);
            Vec3d position=mc.gameRenderer.getCamera().getCameraPos();
            boolean reset=world!=mc.world || previousCamera==null || position.squaredDistanceTo(previousCamera)>64;
            if(reset) {previousVP.set(vp);previousCamera=position;world=mc.world;}
            program.use();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,sceneTexture);GL33.glBindSampler(0,0);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);GL11.glBindTexture(GL11.GL_TEXTURE_2D,depth.getGlId());GL33.glBindSampler(1,depthSampler);
            GL20.glUniform1i(location("scene"),0);GL20.glUniform1i(location("depth"),1);
            matrix(program,"inverseVP",inverse);matrix(program,"previousVP",previousVP);matrix(program,"currentVP",vp);
            vector("cameraDelta",(float)(position.x-previousCamera.x),(float)(position.y-previousCamera.y),(float)(position.z-previousCamera.z));
            vector("camera",(float)(position.x%4096),(float)position.y,(float)(position.z%4096));
            GL20.glUniform2f(location("resolution"),w,h);
            GL20.glUniform4f(location("grade"),grade==null?0:grade.exposure.get(),grade==null?1:grade.contrast.get(),grade==null?1:grade.saturation.get(),grade==null?0:grade.strength.get());
            GL20.glUniform4f(location("tone"),grade==null?1:grade.gamma.get(),grade==null?0:grade.temperature.get(),grade==null?0:grade.vignette.get(),time());
            GL20.glUniform2f(location("fog"),fog==null?0:fog.density.get(),fog==null?0:fog.height.get());color("fogColor",fog==null?0:fog.color.get());
            GL20.glUniform4f(location("effects"),motion==null || reset?0:motion.strength.get(),wet,rain==null?0.4f:rain.ripples.get(),rain==null?0.35f:rain.strength.get());
            GL20.glUniform1f(location("puddleAmount"),rain==null?0.75f:rain.puddles.get()?rain.puddleAmount.get():0);
            vector("skyReflection",skyRed,skyGreen,skyBlue);
            int steps=rain==null?8:rain.quality.is("High")?16:rain.quality.is("Fast")?4:8;
            if(elexis.modules.impl.utils.Optimizer.fastRender()) steps=Math.min(steps,4);
            GL20.glUniform1i(location("reflectionSteps"),steps);GL20.glUniform1i(location("waveCount"),count);
            long now=System.nanoTime();
            for(int i=0;i<count;i++) {
                var wave=SceneShockwaves.get(i);float progress=wave.progress(now);
                GL20.glUniform4f(location("wavePositionRadius["+i+"]"),(float)(wave.center().x-position.x),(float)(wave.center().y-position.y),(float)(wave.center().z-position.z),Math.max(0.05f,wave.radius()*progress));
                GL20.glUniform4f(location("waveParams["+i+"]"),progress,wave.strength(),wave.kind(),wave.radius());color("waveColors["+i+"]",wave.color());
            }
            GL30.glBindVertexArray(vao);GL11.glDrawArrays(GL11.GL_TRIANGLES,0,3);sceneDraws++;lastUse=now;
            previousVP.set(vp);previousCamera=position;
        } catch(RuntimeException error) {failure(error);} finally {
            for(int i=0;i<2;i++) {GL13.glActiveTexture(GL13.GL_TEXTURE0+i);GL11.glBindTexture(GL11.GL_TEXTURE_2D,textures[i]);GL33.glBindSampler(i,samplers[i]);}
            GL13.glActiveTexture(active);GlState.pop(snapshot);
        }
    }
    private static void target(GlTexture color,int w,int h) {
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,outputFbo);GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,color.getGlId(),0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);GL11.glViewport(0,0,w,h);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);GL11.glDepthMask(false);GL11.glColorMask(true,true,true,true);
    }
    private static void ensureCommon() {if(vao==0) vao=GL30.glGenVertexArrays();if(outputFbo==0) outputFbo=GL30.glGenFramebuffers();}
    private static void ensure(int w,int h) {
        ensureCommon();
        if(program==null) program=ShaderProgram.fromResources("assets/elexis/shaders/visual/scene.vert","assets/elexis/shaders/visual/scene.frag");
        if(depthSampler==0) {
            depthSampler=GL33.glGenSamplers();GL33.glSamplerParameteri(depthSampler,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);GL33.glSamplerParameteri(depthSampler,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            GL33.glSamplerParameteri(depthSampler,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);GL33.glSamplerParameteri(depthSampler,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
        }
        if(width==w && height==h && sceneTexture!=0) return;
        if(sceneTexture!=0) GL11.glDeleteTextures(sceneTexture);if(sceneFbo!=0) GL30.glDeleteFramebuffers(sceneFbo);
        sceneTexture=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,sceneTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,w,h,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,0L);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
        sceneFbo=GL30.glGenFramebuffers();GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,sceneFbo);GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,sceneTexture,0);
        if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE) throw new IllegalStateException("Incomplete Visual framebuffer");
        width=w;height=h;previousCamera=null;
    }
    private static void failure(RuntimeException error) {failed=true;failureTime=System.nanoTime();System.err.println("[Elexis Visual] Shader failure: "+error.getMessage());}
    private static float time() {return (System.nanoTime()-START)/1_000_000_000f;}
    private static int location(String name) {return program.getUniformLocation(name);}
    private static void vector(String name,float x,float y,float z) {GL20.glUniform3f(location(name),x,y,z);}
    private static void color(String name,int c) {vector(name,((c>>16)&255)/255f,((c>>8)&255)/255f,(c&255)/255f);}
    private static void matrix(ShaderProgram shader,String name,Matrix4f value) {try(var stack=MemoryStack.stackPush()) {GL20.glUniformMatrix4fv(shader.getUniformLocation(name),false,value.get(stack.mallocFloat(16)));}}
    public static void trimIfIdle() {if(vao!=0 && System.nanoTime()-lastUse>5_000_000_000L) release();}
    public static void release() {
        if(program!=null) {program.delete();program=null;}if(skyProgram!=null) {skyProgram.delete();skyProgram=null;}
        if(sceneTexture!=0) GL11.glDeleteTextures(sceneTexture);if(sceneFbo!=0) GL30.glDeleteFramebuffers(sceneFbo);if(outputFbo!=0) GL30.glDeleteFramebuffers(outputFbo);if(vao!=0) GL30.glDeleteVertexArrays(vao);if(depthSampler!=0) GL33.glDeleteSamplers(depthSampler);
        sceneTexture=sceneFbo=outputFbo=vao=width=height=depthSampler=0;previousCamera=null;world=null;
    }
}
