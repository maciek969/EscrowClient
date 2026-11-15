package elexis.modules.impl.render.javelin.target;

import elexis.client.utils.client.IMinecraft;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/** Camera-facing additive sprites and tapered beam geometry from Javelin. */
public final class RenderGlowingUtil implements IMinecraft {
    public static final Identifier BLOOM = Identifier.of("javelin", "images/target_bloom.png");
    private static final int MAX_QUADS = 8192;
    private static final float[] VERTICES = new float[MAX_QUADS * 20];
    private static final int[] COLORS = new int[MAX_QUADS * 4];
    private static final Vector3f RIGHT = new Vector3f(), UP = new Vector3f();
    private static BufferAllocator allocator;
    private static VertexConsumerProvider.Immediate immediate;
    private static Vec3d camera;
    private static int count;

    private RenderGlowingUtil() {}
    public static void begin() {
        count = 0;
        var view = mc.gameRenderer.getCamera();
        camera = view.getCameraPos();
        RIGHT.set(1,0,0).rotate(view.getRotation());
        UP.set(0,1,0).rotate(view.getRotation());
    }

    public static void glowPoint(double x, double y, double z, float size, int color) {
        glowSprite(x,y,z,size,0,color);
    }

    public static void glowSprite(double x, double y, double z, float half, float spin, int color) {
        if (count + 4 > COLORS.length) return;
        float cosine = (float)Math.cos(Math.toRadians(spin)), sine = (float)Math.sin(Math.toRadians(spin));
        float ax = (RIGHT.x * cosine + UP.x * sine) * half;
        float ay = (RIGHT.y * cosine + UP.y * sine) * half;
        float az = (RIGHT.z * cosine + UP.z * sine) * half;
        float bx = (-RIGHT.x * sine + UP.x * cosine) * half;
        float by = (-RIGHT.y * sine + UP.y * cosine) * half;
        float bz = (-RIGHT.z * sine + UP.z * cosine) * half;
        float px = (float)(x-camera.x), py = (float)(y-camera.y), pz = (float)(z-camera.z);
        vertex(px-ax+bx,py-ay+by,pz-az+bz,0,1,color);
        vertex(px+ax+bx,py+ay+by,pz+az+bz,1,1,color);
        vertex(px+ax-bx,py+ay-by,pz+az-bz,1,0,color);
        vertex(px-ax-bx,py-ay-by,pz-az-bz,0,0,color);
    }

    public static void glowBeam(double x1,double y1,double z1,double x2,double y2,double z2,float half,int color) {
        if (count + 4 > COLORS.length) return;
        float ax=(float)(x1-camera.x), ay=(float)(y1-camera.y), az=(float)(z1-camera.z);
        float bx=(float)(x2-camera.x), by=(float)(y2-camera.y), bz=(float)(z2-camera.z);
        float dx=bx-ax, dy=by-ay, dz=bz-az;
        float mx=(ax+bx)*0.5f, my=(ay+by)*0.5f, mz=(az+bz)*0.5f;
        float sx=dy*mz-dz*my, sy=dz*mx-dx*mz, sz=dx*my-dy*mx;
        float length=(float)Math.sqrt(sx*sx+sy*sy+sz*sz);
        if (length < 0.00001f) { sx=RIGHT.x; sy=RIGHT.y; sz=RIGHT.z; }
        else { sx/=length; sy/=length; sz/=length; }
        sx*=half; sy*=half; sz*=half;
        vertex(ax-sx,ay-sy,az-sz,0.5f,0,color);
        vertex(ax+sx,ay+sy,az+sz,0.5f,1,color);
        vertex(bx+sx,by+sy,bz+sz,0.5f,1,color);
        vertex(bx-sx,by-sy,bz-sz,0.5f,0,color);
    }

    private static void vertex(float x,float y,float z,float u,float v,int color) {
        int offset=count*5;
        VERTICES[offset]=x; VERTICES[offset+1]=y; VERTICES[offset+2]=z;
        VERTICES[offset+3]=u; VERTICES[offset+4]=v;
        COLORS[count++]=color;
    }

    public static void flush(Identifier texture, boolean throughWalls) {
        if (count == 0) return;
        if (allocator == null) {
            allocator=new BufferAllocator(1048576);
            immediate=VertexConsumerProvider.immediate(allocator);
        }
        VertexConsumer vertices=immediate.getBuffer(TargetRender.sprites(texture,throughWalls));
        for(int i=0;i<count;i++) {
            int offset=i*5;
            vertices.vertex(TargetRender.MATRIX,VERTICES[offset],VERTICES[offset+1],VERTICES[offset+2])
                    .texture(VERTICES[offset+3],VERTICES[offset+4]).color(COLORS[i]);
        }
        immediate.draw();
        count=0;
    }

    public static void close() {
        immediate=null;
        if(allocator!=null) allocator.close();
        allocator=null;
        count=0;
    }
}
