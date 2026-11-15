package elexis.modules.impl.render.visual;

import elexis.client.events.impl.entity.EventAttack;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.*;
import elexis.modules.api.settings.impl.*;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import java.util.*;

public final class ModelVisuals {
    private static final Map<LivingEntityRenderState,Integer> TINTS=new WeakHashMap<>();
    private ModelVisuals() {}

    @FunctionAdd(name="ImpactNova",alias="Impact Nova",category=Category.Render,description="Цветовой импульс на модели после удара")
    public static final class ImpactNova extends Function {
        private final Map<UUID,Long> hits=new HashMap<>();
        public final SliderSetting duration=new SliderSetting("Duration (ms)",1000f,200f,3000f,100f);
        public final ColorSetting color=new ColorSetting("Color",0xFFFF8BDA);
        public ImpactNova() { addSettings(duration,color); }
        @EventHandler public void hit(EventAttack event) {
            if(!nullCheck()) hits.put(event.getTarget().getUuid(),System.nanoTime());
        }
        @EventHandler public void update(EventUpdate.Slow event) { hits.values().removeIf(t->System.nanoTime()-t>duration.get()*1_000_000d); }
        public float intensity(Entity entity) {
            Long t=hits.get(entity.getUuid());
            return t==null?0:Math.max(0,1f-(System.nanoTime()-t)/(duration.get()*1_000_000f));
        }
        @Override public void onDisable() { hits.clear(); super.onDisable(); }
    }

    @FunctionAdd(name="KillSpray",alias="Kill Spray",category=Category.Render,description="Логотип в месте смерти атакованной цели")
    public static final class KillSpray extends Function {
        private record Candidate(LivingEntity entity,long time) {}
        private record Spray(Vec3d position,long time) {}
        private final Map<UUID,Candidate> targets=new HashMap<>();
        private final List<Spray> sprays=new ArrayList<>();
        private net.minecraft.client.world.ClientWorld world;
        private final SliderSetting duration=new SliderSetting("Duration (s)",6f,1f,20f,1f);
        public KillSpray() { addSettings(duration); }
        @EventHandler public void hit(EventAttack event) {
            if(!(event.getTarget() instanceof LivingEntity entity) || nullCheck()) return;
            sync(); targets.put(entity.getUuid(),new Candidate(entity,System.nanoTime()));
        }
        private void sync() { if(world!=mc.world) { targets.clear(); sprays.clear(); world=mc.world; } }
        @EventHandler public void update(EventUpdate.Slow event) {
            sync(); if(nullCheck()) return;
            long now=System.nanoTime();
            targets.values().removeIf(t->{
                if(t.entity.isDead()) {
                    if(sprays.size()>=24) sprays.remove(0);
                    sprays.add(new Spray(t.entity.getEntityPos().add(0,0.5,0),now)); return true;
                }
                return t.entity.isRemoved() || now-t.time>10_000_000_000L;
            });
            sprays.removeIf(s->now-s.time>duration.get()*1_000_000_000d);
        }
        @EventHandler public void render(EventRender.Screen.Hud event) {
            if(nullCheck()) return; sync();
            for(Spray s:sprays) OverlayVisuals.drawImage(event,Identifier.of("dimasik","images/killspray/logo.png"),s.position,80);
        }
        @Override public void onDisable() { targets.clear(); sprays.clear(); world=null; super.onDisable(); }
    }

    public static void update(LivingEntity entity,LivingEntityRenderState state) {
        TINTS.remove(state);
        int tint=0xFFFFFFFF;
        boolean tinted=false;
        ImpactNova nova=SceneVisuals.active("ImpactNova",ImpactNova.class);
        float intensity=nova==null?0:nova.intensity(entity);
        if(intensity>0) {
            int c=nova.color.get(); float weight=intensity*0.8f;
            int r=(int)(255+( (c>>16&255)-255)*weight),g=(int)(255+((c>>8&255)-255)*weight),b=(int)(255+((c&255)-255)*weight);
            tint=0xFF000000|r<<16|g<<8|b; tinted=true;
        }
        if(tinted) TINTS.put(state,tint);
    }
    public static int tint(LivingEntityRenderState state,int original) {
        Integer tint=TINTS.get(state);
        return tint==null?original:(original&0xFF000000)|(tint&0xFFFFFF);
    }
}
