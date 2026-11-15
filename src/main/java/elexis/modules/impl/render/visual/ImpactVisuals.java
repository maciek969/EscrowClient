package elexis.modules.impl.render.visual;

import elexis.client.events.impl.entity.*;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.*;
import elexis.modules.api.settings.impl.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;
import java.util.*;

public final class ImpactVisuals {
    private ImpactVisuals() {}
    protected record Pulse(Vec3d position, Box box, String name, long start) {}

    public abstract static class Pulses extends Function {
        protected final SliderSetting duration=new SliderSetting("Duration (ms)",1200f,200f,4000f,100f);
        protected final ColorSetting color=new ColorSetting("Color",0xFFAE93FF);
        protected final List<Pulse> pulses=new ArrayList<>();
        private ClientWorld world;
        protected Pulses() { addSettings(duration,color); }
        protected void add(Entity entity) {
            add(entity.getEntityPos(),entity.getBoundingBox(),entity.getName().getString());
        }
        protected void add(Vec3d pos,Box box,String name) {
            syncWorld();
            if(pulses.size()>=64) pulses.remove(0);
            pulses.add(new Pulse(pos,box,name,System.nanoTime()));
        }
        protected void syncWorld() {
            if(world!=mc.world) { pulses.clear(); world=mc.world; }
        }
        protected float progress(Pulse p) { return (System.nanoTime()-p.start)/(duration.get()*1_000_000f); }
        @EventHandler public void update(EventUpdate.Slow event) {
            syncWorld();
            pulses.removeIf(p->progress(p)>=1f);
        }
        @EventHandler public void render(EventRender.World event) {
            syncWorld();
            if(nullCheck() || pulses.isEmpty() || !hasGeometry()) return;
            try(var frame=new VisualGeometry.Frame(event)) {
                for(Pulse p:pulses) {
                    float t=progress(p);
                    if(t>=0 && t<1) draw(frame,p,t,VisualGeometry.alpha(color.get(),1-t));
                }
            }
        }
        protected abstract void draw(VisualGeometry.Frame frame,Pulse p,float t,int color);
        protected boolean hasGeometry() {return true;}
        @Override public void onDisable() { pulses.clear(); world=null; super.onDisable(); }
    }

    public abstract static class OnHit extends Pulses {
        @EventHandler public void hit(EventAttack event) {
            if(!nullCheck() && event.getTarget() instanceof LivingEntity) impact(event.getTarget());
        }
        protected void impact(Entity entity) {add(entity);}
    }

    @FunctionAdd(name="HitEffect",alias="Hit Effect",category=Category.Render,description="Sphere and ring at the hit spot")
    public static final class HitEffect extends OnHit {
        private final ModeSetting mode=new ModeSetting("Mode","Sphere","Sphere","Ring");
        public HitEffect() { addSettings(mode); }
        @Override protected void draw(VisualGeometry.Frame f,Pulse p,float t,int c) {
            if(mode.is("Ring")) f.ring(p.position.add(0,0.05,0),0.2+t*2,c);
            else f.sphere(p.position.add(0,p.box.getLengthY()*0.5,0),0.2+t*0.9,c);
        }
    }

    @FunctionAdd(name="HitWave",alias="Hit Wave",category=Category.Render,description="Light wave from a hit")
    public static final class HitWave extends OnHit {
        private final SliderSetting radius=new SliderSetting("Radius",8f,2f,24f,1f);
        private final SliderSetting strength=new SliderSetting("Shader strength",1f,0.2f,2f,0.1f);
        private final BooleanSetting geometry=new BooleanSetting("Outline ring",false);
        public HitWave() { addSettings(radius,strength,geometry); }
        @Override protected void impact(Entity entity) {
            super.impact(entity);
            SceneShockwaves.emit(SceneShockwaves.HIT,entity.getEntityPos(),radius.get(),duration.get(),strength.get(),color.get());
        }
        @Override protected boolean hasGeometry() {return geometry.get();}
        @Override protected void draw(VisualGeometry.Frame f,Pulse p,float t,int c) {
            f.ring(p.position.add(0,0.05,0),t*radius.get(),c);
            f.ring(p.position.add(0,0.08,0),Math.max(0,t*radius.get()-0.25),VisualGeometry.alpha(c,(1-t)*0.4f));
        }
        @Override public void onDisable() {SceneShockwaves.clearKind(SceneShockwaves.HIT);super.onDisable();}
    }

    @FunctionAdd(name="GhostHit",alias="Ghost Hit",category=Category.Render,description="Rising target outline after a hit")
    public static final class GhostHit extends OnHit {
        private final SliderSetting lift=new SliderSetting("Lift",1.2f,0f,4f,0.1f);
        public GhostHit() { addSettings(lift); }
        @Override protected void draw(VisualGeometry.Frame f,Pulse p,float t,int c) { f.box(p.box.offset(0,t*lift.get(),0),c); }
    }

    @FunctionAdd(name="HitMarkers",alias="Hit Markers",category=Category.Render,description="Markers and actual damage after a hit")
    public static final class HitMarkers extends Function {
        private record Watched(LivingEntity entity,float health,long expiry) {}
        private record Damage(Vec3d pos,float value,long start) {}
        private final Map<UUID,Watched> watched=new HashMap<>();
        private final List<Damage> numbers=new ArrayList<>();
        private ClientWorld world;
        @EventHandler public void hit(EventAttack event) {
            if(nullCheck() || !(event.getTarget() instanceof LivingEntity target)) return;
            if(world!=mc.world) { clear(); world=mc.world; }
            watched.put(target.getUuid(),new Watched(target,target.getHealth()+target.getAbsorptionAmount(),System.nanoTime()+1_000_000_000L));
        }
        @EventHandler public void update(EventUpdate.Slow event) {
            if(nullCheck() || world!=mc.world) { clear(); world=mc.world; return; }
            long now=System.nanoTime();
            var it=watched.values().iterator();
            while(it.hasNext()) {
                Watched w=it.next();
                float actual=w.entity.getHealth()+w.entity.getAbsorptionAmount();
                if(actual!=w.health) {
                    if(numbers.size()>=64) numbers.remove(0);
                    numbers.add(new Damage(w.entity.getEntityPos().add(0,w.entity.getHeight(),0),w.health-actual,now));
                    it.remove();
                } else if(w.expiry<now || w.entity.isRemoved()) it.remove();
            }
            numbers.removeIf(n->now-n.start>1_200_000_000L);
        }
        @EventHandler public void render(EventRender.Screen.Hud event) {
            if(nullCheck() || world!=mc.world) return;
            long now=System.nanoTime();
            for(Damage n:numbers) {
                float age=(now-n.start)/1_200_000_000f;
                VisualGeometry.label(event,n.pos.add(0,age,0),String.format(Locale.US,"%s%.1f",n.value>=0?"-":"+",Math.abs(n.value)),
                        VisualGeometry.alpha(n.value>=0?0xFFFF8C9D:0xFF83EFBA,1-age));
            }
        }
        private void clear() { watched.clear(); numbers.clear(); }
        @Override public void onDisable() { clear(); world=null; super.onDisable(); }
    }

    @FunctionAdd(name="DeathManager",alias="Death Manager",category=Category.Render,description="Player death spot markers")
    public static final class DeathManager extends Pulses {
        private final Set<UUID> dead=new HashSet<>();
        public DeathManager() { duration.set(4000f); }
        @EventHandler public void deaths(EventUpdate.Slow event) {
            if(nullCheck()) { dead.clear(); return; }
            Set<UUID> present=new HashSet<>();
            for(PlayerEntity p:mc.world.getPlayers()) {
                present.add(p.getUuid());
                if(!p.isAlive() && dead.add(p.getUuid())) add(p);
                if(p.isAlive()) dead.remove(p.getUuid());
            }
            dead.retainAll(present);
        }
        @Override protected void draw(VisualGeometry.Frame f,Pulse p,float t,int c) {
            f.line(p.position.add(-0.3,0.7,0),p.position.add(0.3,0.7,0),c);
            f.line(p.position.add(0,0.1,0),p.position.add(0,1.2,0),c);
            f.ring(p.position.add(0,0.03,0),0.6,c);
        }
        @EventHandler public void labels(EventRender.Screen.Hud event) {
            if(nullCheck()) return;
            for(Pulse p:pulses) VisualGeometry.label(event,p.position.add(0,1.5,0),p.name,0xFFFF9DAE);
        }
        @Override public void onDisable() { dead.clear(); super.onDisable(); }
    }

    @FunctionAdd(name="TNTEffect",alias="TNT Effect",category=Category.Render,description="Shockwave on TNT explosion")
    public static final class TNTEffect extends Pulses {
        private final Map<UUID,TntEntity> tracked=new HashMap<>();
        private final SliderSetting radius=new SliderSetting("Radius",12f,4f,32f,1f);
        private final SliderSetting strength=new SliderSetting("Shader strength",1f,0.2f,2f,0.1f);
        private final BooleanSetting geometry=new BooleanSetting("Outline sphere",false);
        private int scanTick;
        public TNTEffect() {addSettings(radius,strength,geometry);}
        public void explosion(Vec3d center) {
            if(nullCheck() || !isState()) return;
            add(center,new Box(center.add(-1,-1,-1),center.add(1,1,1)),"Explosion");
            SceneShockwaves.emit(SceneShockwaves.EXPLOSION,center,radius.get(),duration.get(),strength.get(),color.get());
        }
        @EventHandler public void tnt(EventUpdate.Slow event) {
            if(nullCheck()) { tracked.clear(); return; }
            var it=tracked.values().iterator();
            while(it.hasNext()) {
                TntEntity t=it.next();
                if(t.getEntityWorld()!=mc.world) { it.remove(); continue; }
                if(t.isRemoved()) { if(t.getFuse()<=4) explosion(t.getEntityPos()); it.remove(); }
            }
            if((scanTick++&1)==0) for(TntEntity t:mc.world.getEntitiesByClass(TntEntity.class,mc.player.getBoundingBox().expand(80),e->true)) {
                if(tracked.size()<128) tracked.put(t.getUuid(),t);
            }
        }
        @Override protected boolean hasGeometry() {return geometry.get();}
        @Override protected void draw(VisualGeometry.Frame f,Pulse p,float t,int c) {
            f.ring(p.position.add(0,0.05,0),t*8,c);
            f.sphere(p.position.add(0,0.5,0),t*3,c);
        }
        @Override public void onDisable() {tracked.clear();scanTick=0;SceneShockwaves.clearKind(SceneShockwaves.EXPLOSION);super.onDisable();}
    }

    @FunctionAdd(name="KillCards",alias="Kill Cards",category=Category.Render,description="Cards of defeated targets")
    public static final class KillCards extends Function {
        private record Candidate(LivingEntity entity,long time) {}
        private record Card(String name,long time) {}
        private final Map<UUID,Candidate> targets=new HashMap<>();
        private final List<Card> cards=new ArrayList<>();
        private ClientWorld world;
        @EventHandler public void attack(EventAttack event) {
            if(nullCheck() || !(event.getTarget() instanceof LivingEntity target)) return;
            if(world!=mc.world) { clear(); world=mc.world; }
            targets.put(target.getUuid(),new Candidate(target,System.nanoTime()));
        }
        @EventHandler public void update(EventUpdate.Slow event) {
            if(nullCheck() || world!=mc.world) { clear(); world=mc.world; return; }
            long now=System.nanoTime();
            targets.values().removeIf(c->{
                if(c.entity.isDead()) { if(cards.size()>=5) cards.remove(0); cards.add(new Card(c.entity.getName().getString(),now)); return true; }
                return c.entity.isRemoved() || now-c.time>10_000_000_000L;
            });
            cards.removeIf(c->now-c.time>5_000_000_000L);
        }
        @EventHandler public void render(EventRender.Screen.Hud event) {
            if(nullCheck() || world!=mc.world) return;
            var r=event.getRenderer(); float y=45;
            for(Card c:cards) {
                String text="Defeated  "+c.name;
                var font=elexis.client.utils.render.main.newrender.text.FontRegistry.SF_MEDIUM;
                float width=r.measureText(font,text,14).width+24;
                float x=event.getViewportWidth()-width-20;
                r.rect(x,y,width,34,7,0xDE111822);
                r.text(font,x+12,y+22,14,text,0xFFE7DFFF); y+=40;
            }
        }
        private void clear() { targets.clear(); cards.clear(); }
        @Override public void onDisable() { clear(); world=null; super.onDisable(); }
    }
}
