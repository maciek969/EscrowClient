package elexis.modules.impl.render.visual;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.*;
import elexis.modules.api.settings.impl.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.*;
import net.minecraft.client.world.ClientWorld;
import java.util.*;

public final class EntityVisuals {
    private EntityVisuals() {}

    public abstract static class Nearby extends Function {
        protected final SliderSetting range = new SliderSetting("Range", 64f, 8f, 192f, 4f);
        protected final BooleanSetting friends = new BooleanSetting("Show friends", true);
        protected Nearby() { addSettings(range, friends); }
        protected boolean accepts(Entity e) {
            return e != mc.player && !e.isRemoved() && e.isAlive() && mc.player.squaredDistanceTo(e) <= range.get()*range.get()
                    && (friends.get() || !VisualGeometry.friend(e));
        }
    }

    @FunctionAdd(name="Tracers", alias="Tracers", category=Category.Render, description="Lines to players and items")
    public static final class Tracers extends Nearby {
        private final BooleanSetting items=new BooleanSetting("Items", false);
        public Tracers() { addSettings(items); }
        @EventHandler public void render(EventRender.World event) {
            if(nullCheck()) return;
            Vec3d start=mc.gameRenderer.getCamera().getCameraPos().add(mc.player.getRotationVec(event.getTicks()).multiply(0.5));
            try(var frame=new VisualGeometry.Frame(event)) {
                for(Entity e:mc.world.getEntities()) if(accepts(e) && (e instanceof PlayerEntity || items.get() && e instanceof ItemEntity))
                    frame.line(start,VisualGeometry.position(e,event.getTicks()).add(0,e.getHeight()*0.5,0),VisualGeometry.color(e));
            }
        }
    }

    @FunctionAdd(name="SkeletonESP", alias="Skeleton ESP", category=Category.Render, description="Player skeletons as lines")
    public static final class SkeletonESP extends Nearby {
        @EventHandler public void render(EventRender.World event) {
            if(nullCheck()) return;
            try(var frame=new VisualGeometry.Frame(event)) {
                for(PlayerEntity e:mc.world.getPlayers()) if(accepts(e)) {
                    Vec3d p=VisualGeometry.position(e,event.getTicks());
                    double yaw=Math.toRadians(e.bodyYaw), height=e.getHeight(), stride=Math.sin(e.limbAnimator.getAnimationProgress(event.getTicks()))*0.22;
                    Vec3d side=new Vec3d(Math.cos(yaw),0,Math.sin(yaw));
                    Vec3d forward=new Vec3d(-Math.sin(yaw),0,Math.cos(yaw));
                    Vec3d hip=p.add(0,height*0.48,0), shoulder=p.add(0,height*0.78,0);
                    int c=VisualGeometry.color(e);
                    frame.line(hip,shoulder,c); frame.line(shoulder,p.add(0,height*0.95,0),c);
                    for(int sign:new int[]{-1,1}) {
                        Vec3d arm=shoulder.add(side.multiply(0.25*sign)), elbow=arm.add(0,-height*0.18,0).add(forward.multiply(stride*sign));
                        frame.line(shoulder,arm,c); frame.line(arm,elbow,c); frame.line(elbow,elbow.add(0,-height*0.15,0),c);
                        Vec3d leg=hip.add(side.multiply(0.12*sign)), knee=p.add(side.multiply(0.12*sign)).add(0,height*0.24,0).add(forward.multiply(-stride*sign));
                        frame.line(hip,leg,c); frame.line(leg,knee,c); frame.line(knee,p.add(side.multiply(0.12*sign)).add(forward.multiply(stride*sign)),c);
                    }
                }
            }
        }
    }

    @FunctionAdd(name="ArmorDurability", alias="Armor Durability", category=Category.Render, description="Armor durability of the player and targets")
    public static final class ArmorDurability extends Nearby {
        private final BooleanSetting self=new BooleanSetting("Self",true), others=new BooleanSetting("Others",true);
        public ArmorDurability() { addSettings(self,others); }
        @EventHandler public void render(EventRender.Screen.Hud event) {
            if(nullCheck() || mc.options.hudHidden) return;
            if(self.get()) {
                String text=armor(mc.player);
                if(!text.isEmpty()) {
                    var r=event.getRenderer();
                    r.text(elexis.client.utils.render.main.newrender.text.FontRegistry.SF_MEDIUM,16,event.getViewportHeight()-60,14,text,0xFFFFFFFF);
                }
            }
            if(others.get()) for(PlayerEntity e:mc.world.getPlayers()) if(accepts(e)) {
                String text=armor(e);
                if(!text.isEmpty()) VisualGeometry.label(event,VisualGeometry.position(e,mc.getRenderTickCounter().getTickProgress(true)).add(0,e.getHeight()+0.45,0),text,VisualGeometry.color(e));
            }
        }
        private String armor(PlayerEntity p) {
            StringJoiner out=new StringJoiner("  ");
            for(EquipmentSlot slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}) {
                var stack=p.getEquippedStack(slot);
                if(stack.isDamageable()) out.add(slot.getName()+" "+Math.round((1f-(float)stack.getDamage()/stack.getMaxDamage())*100)+"%");
            }
            return out.toString();
        }
    }

    @FunctionAdd(name="TNTTimer", alias="TNT Timer", category=Category.Render, description="Dynamite explosion timer")
    public static final class TNTTimer extends Nearby {
        @EventHandler public void render(EventRender.Screen.Hud event) {
            if(nullCheck()) return;
            for(Entity e:mc.world.getEntities()) if(e instanceof TntEntity tnt && accepts(e))
                VisualGeometry.label(event,VisualGeometry.position(e,mc.getRenderTickCounter().getTickProgress(true)).add(0,1.2,0),
                        String.format(Locale.US,"TNT %.1f s",tnt.getFuse()/20f),tnt.getFuse()<20?0xFFFF6F7D:0xFFFFD37B);
        }
    }

    @FunctionAdd(name="FireworkESP", alias="Firework ESP", category=Category.Render, description="Firework launch markers")
    public static final class FireworkESP extends Nearby {
        private record Launch(Vec3d pos,long created) {}
        private final Map<UUID,Launch> launches=new HashMap<>();
        private ClientWorld world;
        @EventHandler public void update(EventUpdate.Slow event) {
            if(nullCheck() || world!=mc.world) { launches.clear(); world=mc.world; }
            if(nullCheck()) return;
            long now=System.nanoTime();
            launches.values().removeIf(l->now-l.created>5_000_000_000L);
            for(Entity e:mc.world.getEntities()) if(e instanceof FireworkRocketEntity && accepts(e))
                launches.putIfAbsent(e.getUuid(),new Launch(e.getEntityPos(),now));
        }
        @EventHandler public void render(EventRender.Screen.Hud event) {
            if(nullCheck() || world!=mc.world) return;
            long now=System.nanoTime();
            for(Launch l:launches.values()) VisualGeometry.label(event,l.pos.add(0,0.5,0),String.format(Locale.US,"Firework %.1f s",(now-l.created)/1e9),0xFFAEB9FF);
        }
        @Override public void onDisable() { launches.clear(); world=null; super.onDisable(); }
    }

}
