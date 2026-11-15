package elexis.modules.impl.render.visual;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.modules.api.*;
import elexis.modules.api.settings.impl.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.*;
import java.util.*;

public final class AmbientVisuals {
    private AmbientVisuals() {}
    protected record Particle(Vec3d position, long birth, double phase) {}

    public abstract static class Field extends Function {
        protected final SliderSetting count=new SliderSetting("Amount",40f,8f,160f,4f);
        protected final SliderSetting radius=new SliderSetting("Radius",9f,3f,20f,1f);
        protected final SliderSetting size=new SliderSetting("Size",0.12f,0.03f,0.5f,0.01f);
        protected final List<Particle> particles=new ArrayList<>();
        private final Random random=new Random();
        private ClientWorld world;
        protected Field() { addSettings(count,radius,size); }
        @EventHandler public void update(EventUpdate.Slow event) {
            if(nullCheck() || world!=mc.world) { particles.clear(); world=mc.world; }
            if(nullCheck()) return;
            long now=System.nanoTime();
            particles.removeIf(p->now-p.birth>8_000_000_000L || p.position.squaredDistanceTo(mc.player.getEntityPos())>radius.get()*radius.get()*4);
            int wanted=count.get().intValue();
            if(elexis.modules.impl.utils.Optimizer.fewerParticles()) wanted=Math.min(wanted,32);
            while(particles.size()>wanted) particles.remove(particles.size()-1);
            for(int n=0;n<4 && particles.size()<wanted;n++) {
                Vec3d pos=mc.player.getEntityPos().add((random.nextDouble()*2-1)*radius.get(),random.nextDouble()*5-0.2,(random.nextDouble()*2-1)*radius.get());
                if(mc.world.getBlockState(BlockPos.ofFloored(pos)).isAir()) particles.add(new Particle(pos,now,random.nextDouble()*Math.PI*2));
            }
        }
        @EventHandler public void render(EventRender.World event) {
            if(nullCheck() || world!=mc.world || particles.isEmpty()) return;
            long now=System.nanoTime();
            try(var frame=new VisualGeometry.Frame(event)) {
                int index=0;
                for(Particle p:particles) {
                    float age=(now-p.birth)/1_000_000_000f;
                    float alpha=Math.min(1f,age*2)*Math.min(1f,Math.max(0,8-age));
                    int color=VisualGeometry.alpha(ThemeColorUtils.getIconColor(10,index++*13),alpha);
                    draw(frame,p,age,color);
                }
            }
        }
        protected abstract void draw(VisualGeometry.Frame frame,Particle p,float age,int color);
        @Override public void onDisable() { particles.clear(); world=null; super.onDisable(); }
    }

    @FunctionAdd(name="FireFly",alias="FireFly",category=Category.Render,description="Fireflies around the player")
    public static final class FireFly extends Field {
        @Override protected void draw(VisualGeometry.Frame f,Particle p,float age,int color) {
            Vec3d pos=p.position.add(Math.sin(age+p.phase)*0.5,Math.sin(age*1.5+p.phase)*0.3,Math.cos(age+p.phase)*0.5);
            double s=size.get()*(0.7+Math.sin(age*3+p.phase)*0.3);
            f.line(pos.add(-s,0,0),pos.add(s,0,0),color);
            f.line(pos.add(0,-s,0),pos.add(0,s,0),color);
            f.line(pos.add(0,0,-s),pos.add(0,0,s),color);
        }
    }

    @FunctionAdd(name="WorldParticles",alias="World Particles",category=Category.Render,description="Floating particles around the player")
    public static final class WorldParticles extends Field {
        private final ModeSetting shape=new ModeSetting("Shape","Diamond","Diamond","Cube","Ring");
        public WorldParticles() { addSettings(shape); }
        @Override protected void draw(VisualGeometry.Frame f,Particle p,float age,int c) {
            Vec3d pos=p.position.add(Math.sin(age+p.phase)*0.2,age*0.15,Math.cos(age+p.phase)*0.2);
            double s=size.get();
            if(shape.is("Cube")) f.box(new Box(pos.add(-s,-s,-s),pos.add(s,s,s)),c);
            else if(shape.is("Ring")) f.ring(pos,s,c);
            else {
                for(int sign:new int[]{-1,1}) {
                    f.line(pos.add(0,s*sign,0),pos.add(s,0,0),c); f.line(pos.add(0,s*sign,0),pos.add(-s,0,0),c);
                    f.line(pos.add(0,s*sign,0),pos.add(0,0,s),c); f.line(pos.add(0,s*sign,0),pos.add(0,0,-s),c);
                }
            }
        }
    }

    @FunctionAdd(name="Cubes",alias="Cubes",category=Category.Render,description="Floating rotating cubes")
    public static final class Cubes extends Field {
        @Override protected void draw(VisualGeometry.Frame f,Particle p,float age,int c) {
            rotatingCube(f,p.position.add(0,Math.sin(age+p.phase)*0.5,0),size.get(),age+p.phase,c);
        }
    }

    @FunctionAdd(name="CubeField",alias="Cube Field",category=Category.Render,description="Bouncing cubes around the player")
    public static final class CubeField extends Field {
        @Override protected void draw(VisualGeometry.Frame f,Particle p,float age,int c) {
            BlockPos base=BlockPos.ofFloored(p.position).down();
            int attempts=0;
            while(mc.world.getBlockState(base).isAir() && attempts++<8) base=base.down();
            if(mc.world.getBlockState(base).isAir()) return;
            Vec3d pos=new Vec3d(p.position.x,base.getY()+1+Math.abs(Math.sin(age*2+p.phase))*0.5,p.position.z);
            rotatingCube(f,pos.add(0,size.get(),0),size.get(),age*1.4+p.phase,c);
        }
    }

    @FunctionAdd(name="LineGlyphs",alias="Line Glyphs",category=Category.Render,description="Neon broken lines in the world")
    public static final class LineGlyphs extends Field {
        @Override protected void draw(VisualGeometry.Frame f,Particle p,float age,int c) {
            Vec3d pos=p.position.add(0,Math.sin(age+p.phase)*0.4,0);
            for(int i=0;i<4;i++) {
                Vec3d next=pos.add(Math.sin(p.phase+i)*0.6,Math.cos(p.phase+i)*0.4,Math.cos(p.phase+i*2)*0.6);
                f.line(pos,next,c); pos=next;
            }
        }
    }

    @FunctionAdd(name="VoidButterflies",alias="Void Butterflies",category=Category.Render,description="Animated butterflies on block surfaces")
    public static final class VoidButterflies extends Field {
        @Override protected void draw(VisualGeometry.Frame f,Particle p,float age,int c) {
            BlockPos base=BlockPos.ofFloored(p.position);
            int tries=0;
            while(mc.world.getBlockState(base).isAir() && tries++<8) base=base.down();
            if(mc.world.getBlockState(base).isAir()) return;
            Vec3d pos=new Vec3d(p.position.x,base.getY()+1.02,p.position.z);
            double s=size.get()*2, flap=Math.sin(age*9+p.phase)*s*0.5;
            f.line(pos.add(0,0,-s*0.4),pos.add(0,0,s*0.5),c);
            for(int sign:new int[]{-1,1}) {
                Vec3d wing=pos.add(s*sign,Math.abs(flap),-s*0.7),tail=pos.add(s*0.8*sign,Math.abs(flap),s*0.7);
                f.line(pos,wing,c); f.line(wing,tail,c); f.line(tail,pos,c);
            }
        }
    }

    private static void rotatingCube(VisualGeometry.Frame f,Vec3d center,double size,double angle,int color) {
        Vec3d[] vertices=new Vec3d[8];
        double cosine=Math.cos(angle),sine=Math.sin(angle);
        for(int i=0;i<8;i++) {
            double x=(i%2==0?-size:size),y=(i/2%2==0?-size:size),z=(i/4==0?-size:size);
            vertices[i]=center.add(x*cosine-z*sine,y,x*sine+z*cosine);
        }
        for(int i=0;i<8;i++) for(int bit=1;bit<=4;bit*=2) if((i&bit)==0) f.line(vertices[i],vertices[i|bit],color);
    }
}
