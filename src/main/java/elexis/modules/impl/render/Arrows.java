package elexis.modules.impl.render;

import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.client.utils.render.main.newrender.core.Renderer2D;
import elexis.modules.api.*;
import elexis.modules.api.settings.impl.*;
import elexis.modules.impl.render.visual.VisualGeometry;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.GlTextureView;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import java.util.*;

/** Javelin's Dart, Heads and Minimal markers, including frame-rate independent smoothing. */
@FunctionAdd(name="Arrows",alias="Arrows",category=Category.Render,description="Javelin arrows with heads and smooth animation")
public final class Arrows extends Function {
    public final ModeSetting mode = new ModeSetting("Mode","Dart","Dart","Heads","Minimal");
    public final SliderSetting distance = new SliderSetting("Distance",55f,25f,140f,1f);
    public final SliderSetting smoothing = new SliderSetting("Smoothness",.6f,0f,1f,.05f);
    public final ModeSetting colorMode = new ModeSetting("Color","Theme","Theme","Custom");
    public final ColorSetting customColor = new ColorSetting("Custom color",-7504649);
    private final Map<Integer,Marker> states = new HashMap<>();
    private Object world;
    private float radius;
    private long previousFrame;
    private static final Identifier TRIANGLE = Identifier.of("javelin","textures/hud/triangle.png");
    private static final Identifier DART = Identifier.of("javelin","textures/hud/arrow.png");
    private static final class Marker {
        float angle,presence;
        int color,skin;
        boolean alive;
        Marker(float angle) { this.angle=angle; }
    }
    public Arrows() {
        customColor.setVisible(()->colorMode.is("Custom"));
        addSettings(mode,distance,smoothing,colorMode,customColor);
    }
    @EventHandler public void render(EventRender.Screen.Hud event) {
        if (nullCheck() || mc.options.hudHidden || !mc.options.getPerspective().isFirstPerson()) {
            previousFrame=0;
            if (world!=mc.world) clear();
            return;
        }
        if (world!=mc.world) { clear(); world=mc.world; }
        float scale=(float)mc.getWindow().getScaleFactor();
        float cx=event.getViewportWidth()/2f,cy=event.getViewportHeight()/2f;
        float desired=distance.get();
        if (mc.currentScreen instanceof InventoryScreen) desired+=20;
        if (mc.player.isSneaking()) desired-=8;
        if (mc.player.getVelocity().horizontalLengthSquared()>.0036) desired+=5;
        desired*=scale;
        long now=System.nanoTime();
        float dt=previousFrame==0?0:Math.min(.1f,(now-previousFrame)*1e-9f);
        previousFrame=now;
        float ease=dt<=0?1:1-(float)Math.exp(-MathHelper.lerp(smoothing.get(),22f,1.5f)*dt);
        radius=radius<=0?desired:MathHelper.lerp(ease,radius,desired);
        float partial=mc.getRenderTickCounter().getTickProgress(true);
        var self=mc.player.getLerpedPos(partial);
        float yaw=mc.player.getYaw(partial);
        int base=colorMode.is("Custom")?customColor.get():ThemeColorUtils.getIconColor(0,0);
        for (var player:mc.world.getPlayers()) {
            if (player==mc.player || player.isRemoved()) continue;
            var pos=player.getLerpedPos(partial);
            float angle=(float)Math.toRadians(MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(pos.z-self.z,pos.x-self.x))-90-yaw));
            Marker marker=states.computeIfAbsent(player.getId(),key->new Marker(angle));
            marker.angle+=(float)Math.atan2(Math.sin(angle-marker.angle),Math.cos(angle-marker.angle))*ease;
            marker.color=VisualGeometry.friend(player)?-10616966:base;
            if (mode.is("Heads") && player instanceof AbstractClientPlayerEntity client) marker.skin=texture(client.getSkin().body().texturePath());
            marker.alive=true;
        }
        float appear=dt<=0?1:1-(float)Math.exp(-10*dt),clock=(now%100_000_000_000L)*1e-9f;
        var renderer=event.getRenderer();
        int arrow=texture(mode.is("Dart")?TRIANGLE:DART);
        var iterator=states.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry=iterator.next();
            Marker marker=entry.getValue();
            marker.presence=MathHelper.lerp(appear,marker.presence,marker.alive?1:0);
            if (!marker.alive && marker.presence<.03f) { iterator.remove(); continue; }
            marker.alive=false;
            float presence=marker.presence;
            float pop=(.7f+.3f*presence)*(1+.05f*MathHelper.sin(clock*4.5f+entry.getKey()*1.7f));
            float x=cx+MathHelper.sin(marker.angle)*radius,y=cy-MathHelper.cos(marker.angle)*radius;
            if (mode.is("Minimal")) {
                float r=1.15f*scale*pop;
                for (int arm=-1;arm<=1;arm+=2) for (int point=0;point<=5;point++) {
                    if (arm==1 && point==0) continue;
                    float p=point/5f,lx=arm*3.35f*scale*pop*p,ly=MathHelper.lerp(p,-3.45f,2.25f)*scale*pop;
                    renderer.circle(x+lx*MathHelper.cos(marker.angle)-ly*MathHelper.sin(marker.angle),
                            y+lx*MathHelper.sin(marker.angle)+ly*MathHelper.cos(marker.angle),r,0,1,VisualGeometry.alpha(marker.color,presence*.84f));
                }
            } else if (mode.is("Heads")) {
                float size=11*scale*pop;
                sprite(renderer,arrow,x,y,marker.angle,-(size/2+5.5f*scale),10*scale*pop,marker.color,presence);
                renderer.circle(x,y,size/2+2.4f*scale,0,1,VisualGeometry.alpha(0xFF08080E,presence*.47f));
                renderer.circle(x,y,size/2+1.1f*scale,0,1,VisualGeometry.alpha(marker.color,presence));
                renderer.circle(x,y,size/2,0,1,VisualGeometry.alpha(0xFF181524,presence));
                if (marker.skin>0) {
                    int tint=VisualGeometry.alpha(0xFFFFFFFF,presence);
                    renderer.drawRgbaTextureRegionRounded(marker.skin,x-size/2,y-size/2,size,size,.125f,.125f,.25f,.25f,size/2,tint);
                    renderer.drawRgbaTextureRegionRounded(marker.skin,x-size/2,y-size/2,size,size,.625f,.125f,.75f,.25f,size/2,tint);
                }
            } else sprite(renderer,arrow,x,y,marker.angle,0,14*scale*pop,marker.color,presence);
        }
    }
    private static void sprite(Renderer2D renderer,int texture,float x,float y,float angle,float offset,float size,int color,float alpha) {
        if (texture<=0) return;
        renderer.pushTranslation(x,y);
        renderer.pushRotation((float)Math.toDegrees(angle));
        try { renderer.drawRgbaTexture(texture,-size/2,-size/2+offset,size,size,VisualGeometry.alpha(color,alpha),false); }
        finally { renderer.popRotation(); renderer.popTransform(); }
    }
    private static int texture(Identifier id) {
        var view=mc.getTextureManager().getTexture(id).getGlTextureView();
        return view instanceof GlTextureView gl?gl.texture().getGlId():0;
    }
    private void clear() { states.clear(); previousFrame=0; radius=0; world=null; }
    @Override public void onDisable() { clear(); super.onDisable(); }
}

