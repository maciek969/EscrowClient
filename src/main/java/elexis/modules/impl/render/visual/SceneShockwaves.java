package elexis.modules.impl.render.visual;

import elexis.client.utils.client.IMinecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import java.lang.ref.WeakReference;

/** Bounded shared GPU inputs for impact/explosion shaders. No per-frame entity scans. */
public final class SceneShockwaves implements IMinecraft {
    public static final int HIT=0,EXPLOSION=1,MAX=4;
    public record Wave(int kind,Vec3d center,float radius,float duration,float strength,int color,long start) {
        public float progress(long now) {return (now-start)/(duration*1_000_000f);}
    }
    private static final Wave[] waves=new Wave[MAX];
    private static int count;
    private static WeakReference<ClientWorld> world=new WeakReference<>(null);
    private SceneShockwaves() {}
    public static void emit(int kind,Vec3d center,float radius,float duration,float strength,int color) {
        if(mc.world==null || mc.player==null) return;
        if(mc.player.getEntityPos().squaredDistanceTo(center)>Math.pow(radius+80,2)) return;
        prune();long now=System.nanoTime();
        for(int i=0;i<count;i++) if(waves[i].kind==kind && now-waves[i].start<150_000_000L
                && waves[i].center.squaredDistanceTo(center)<1) return;
        if(count==MAX) {System.arraycopy(waves,1,waves,0,MAX-1);count--;}
        waves[count++]=new Wave(kind,center,radius,Math.max(100,duration),strength,color,now);
    }
    public static int prune() {
        if(world.get()!=mc.world) {clear();world=new WeakReference<>(mc.world);}
        long now=System.nanoTime();int kept=0;
        for(int i=0;i<count;i++) if(waves[i].progress(now)<1) waves[kept++]=waves[i];
        for(int i=kept;i<count;i++) waves[i]=null;
        return count=kept;
    }
    public static Wave get(int index) {return waves[index];}
    public static void clearKind(int kind) {
        int kept=0;for(int i=0;i<count;i++) if(waves[i].kind!=kind) waves[kept++]=waves[i];
        for(int i=kept;i<count;i++) waves[i]=null;count=kept;
    }
    public static void clear() {for(int i=0;i<count;i++) waves[i]=null;count=0;}
}
