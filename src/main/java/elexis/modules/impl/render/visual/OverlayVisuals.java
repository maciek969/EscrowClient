package elexis.modules.impl.render.visual;

import elexis.Elexis;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.*;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.math.Mathf;
import elexis.modules.api.*;
import elexis.modules.api.settings.impl.*;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.texture.GlTextureView;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import java.util.UUID;

public final class OverlayVisuals {
    private OverlayVisuals() {}

    @FunctionAdd(name="AspectRatio",alias="Aspect Ratio",category=Category.Render,description="Соотношение сторон экрана")
    public static final class AspectRatio extends Function {
        private final SliderSetting ratio=new SliderSetting("Ratio",1.78f,0.8f,3f,0.01f);
        public AspectRatio() { addSettings(ratio); }
        @EventHandler public void aspect(EventAspectRatio event) { event.setValue(ratio.get()); event.cancel(); }
    }

    @FunctionAdd(name="PlayerAlert",alias="Player Alert",category=Category.Render,description="Дуга урона и лечения вокруг прицела")
    public static final class PlayerAlert extends Function {
        private float previousHealth=Float.NaN;
        private UUID player;
        private long start;
        private int color;
        private final SliderSetting radius=new SliderSetting("Radius",46f,20f,120f,2f);
        public PlayerAlert() { addSettings(radius); }
        @EventHandler public void update(EventUpdate.Slow event) {
            if(nullCheck()) { previousHealth=Float.NaN; player=null; return; }
            float health=mc.player.getHealth()+mc.player.getAbsorptionAmount();
            if(!mc.player.getUuid().equals(player)) { previousHealth=health; player=mc.player.getUuid(); }
            if(!Float.isNaN(previousHealth) && health!=previousHealth) {
                color=health<previousHealth?0xFFFF647E:0xFF72EDB0; start=System.nanoTime();
            }
            previousHealth=health;
        }
        @EventHandler public void render(EventRender.Screen.Hud event) {
            if(nullCheck() || mc.options.hudHidden) return;
            float age=(System.nanoTime()-start)/900_000_000f;
            if(age<0 || age>=1) return;
            float x=event.getViewportWidth()/2f,y=event.getViewportHeight()/2f;
            var r=event.getRenderer();
            for(int i=0;i<36;i++) {
                double a=Math.toRadians(215+i*4);
                r.circle(x+(float)Math.cos(a)*radius.get(),y+(float)Math.sin(a)*radius.get(),1.8f,0,1,VisualGeometry.alpha(color,1-age));
            }
        }
        @Override public void onDisable() { previousHealth=Float.NaN; player=null; start=0; super.onDisable(); }
    }

    @FunctionAdd(name="Picture",alias="Picture",category=Category.Render,description="Картинка над головой игрока")
    public static final class Picture extends Function {
        private final ModeSetting image=new ModeSetting("Image","Soca","Soca","Eagle");
        private final BooleanSetting others=new BooleanSetting("Others",false);
        private final SliderSetting size=new SliderSetting("Size",70f,24f,160f,2f);
        public Picture() { addSettings(image,others,size); }
        @EventHandler public void render(EventRender.Screen.Hud event) {
            if(nullCheck()) return;
            Identifier id=Identifier.of("dimasik",image.is("Eagle")?"textures/picture/mighty_eagle.png":"textures/test/soca.png");
            for(PlayerEntity p:mc.world.getPlayers()) {
                if(p==mc.player && mc.options.getPerspective().isFirstPerson() || p!=mc.player && !others.get()) continue;
                drawImage(event,id,VisualGeometry.position(p,mc.getRenderTickCounter().getTickProgress(true)).add(0,p.getHeight()+0.7,0),size.get());
            }
        }
    }

    @FunctionAdd(name="Cursor",alias="Cursor",category=Category.Render,description="Курсор из Visual в меню")
    public static final class Cursor extends Function {
        private final SliderSetting size=new SliderSetting("Size",24f,12f,48f,1f);
        private final NativeVisualCursor nativeCursor=new NativeVisualCursor();
        public long vanillaOverrides;
        public Cursor() { addSettings(size); }
        @Override public void onEnable() { super.onEnable();nativeCursor.apply(size.get().intValue()); }
        @EventHandler public void update(EventUpdate.Fast event) { nativeCursor.apply(size.get().intValue()); }
        @Override public void onDisable() {nativeCursor.close();super.onDisable();}
        public long cursorHandle() {return nativeCursor.handle();}
        public void refreshNativeCursor() {nativeCursor.apply(size.get().intValue());}
    }

    @FunctionAdd(name="CatPet",alias="Cat Pet",category=Category.Render,description="Локальный кот-компаньон")
    public static final class CatPet extends Function {
        private CatEntity cat;
        private int spawnDelay;
        private final SliderSetting distance=new SliderSetting("Follow distance",1.5f,1f,4f,0.1f);
        public CatPet() { addSettings(distance); }
        @EventHandler public void update(EventUpdate.Slow event) {
            if(nullCheck()) { remove(); return; }
            if(cat!=null && cat.getEntityWorld()!=mc.world) remove();
            if(cat==null || cat.isRemoved()) {
                if(spawnDelay-->0) return;
                var spot=ClientCompanionCat.findGround(mc.world,mc.player.getEntityPos(),mc.player.getYaw());
                if(spot==null) {spawnDelay=20;return;}
                cat=new ClientCompanionCat(mc.world);
                int id=-875221;
                while(mc.world.getEntityById(id)!=null) id--;
                cat.setId(id);cat.setPosition(spot);mc.world.addEntity(cat);
            }
            ((ClientCompanionCat)cat).followDistance(distance.get());
        }
        private void remove() { if(cat!=null) cat.discard(); cat=null;spawnDelay=0; }
        public void releaseWorld() {remove();}
        @Override public void onDisable() { remove(); super.onDisable(); }
    }

    @FunctionAdd(name="AntiAim",alias="Anti Aim",category=Category.Render,description="Визуальное вращение модели в третьем лице")
    public static final class AntiAim extends Function {
        private final ModeSetting mode=new ModeSetting("Mode","Spin","Spin","Jitter","Static");
        private final SliderSetting speed=new SliderSetting("Speed",90f,10f,720f,10f);
        private final SliderSetting pitch=new SliderSetting("Pitch",0f,-90f,90f,1f);
        public AntiAim() { addSettings(mode,speed,pitch); }
        public void apply(PlayerEntityRenderState state) {
            double time=System.nanoTime()/1e9;
            state.bodyYaw=mode.is("Spin")?(float)((time*speed.get())%360):mode.is("Jitter")?((long)(time*5)%2==0?90:-90):180;
            state.relativeHeadYaw=0; state.pitch=pitch.get();
        }
    }

    public static void applyAntiAim(PlayerEntityRenderState state) {
        if(Elexis.getFunctionManager()==null) return;
        var module=Elexis.getFunctionManager().getFunctionByName("AntiAim");
        if(module instanceof AntiAim anti && anti.isState()) anti.apply(state);
    }

    public static void drawImage(EventRender.Screen.Hud event,Identifier id,Vec3d world,float size) {
        Vec3d screen=Mathf.worldSpaceToScreenSpace(world);
        if(screen==null || !Double.isFinite(screen.x) || !Double.isFinite(screen.y) || screen.z<0 || screen.z>=1) return;
        float scale=(float)elexis.client.utils.client.IMinecraft.mc.getWindow().getScaleFactor();
        drawTexture(event.getRenderer(),id,(float)screen.x*scale-size/2,(float)screen.y*scale-size/2,size);
    }

    private static void drawTexture(elexis.client.utils.render.main.newrender.core.Renderer2D r,Identifier id,float x,float y,float size) {
        var texture=elexis.client.utils.client.IMinecraft.mc.getTextureManager().getTexture(id);
        if(texture.getGlTextureView() instanceof GlTextureView view)
            r.drawRgbaTexture(view.texture().getGlId(),x,y,size,size,0xFFFFFFFF,false);
    }
}
