package elexis.modules.impl.render.visual;

import elexis.Elexis;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.client.IMinecraft;
import elexis.client.utils.math.Mathf;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.client.utils.render.main.newrender.text.FontRegistry;
import elexis.client.utils.render.world.WorldGeometryEmitter;
import elexis.modules.impl.render.Predictions;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class VisualGeometry implements IMinecraft {
    private static Buffers shared;
    private static final double[] COS48=new double[49],SIN48=new double[49],COS40=new double[41],SIN40=new double[41];
    static {
        for(int i=0;i<=48;i++) {COS48[i]=Math.cos(i*Math.PI*2/48);SIN48[i]=Math.sin(i*Math.PI*2/48);}
        for(int i=0;i<=40;i++) {COS40[i]=Math.cos(i*Math.PI*2/40);SIN40[i]=Math.sin(i*Math.PI*2/40);}
    }
    private static final class Buffers {
        final BufferAllocator allocator=new BufferAllocator(65536);
        final VertexConsumerProvider.Immediate consumers=VertexConsumerProvider.immediate(allocator);
        WorldGeometryEmitter emitter;
        boolean busy;
    }
    public static void release() {
        if(shared!=null && !shared.busy) {shared.allocator.close();shared=null;}
    }
    private VisualGeometry() {}

    public static Vec3d position(Entity entity, float delta) {
        return new Vec3d(entity.lastX + (entity.getX() - entity.lastX) * delta,
                entity.lastY + (entity.getY() - entity.lastY) * delta,
                entity.lastZ + (entity.getZ() - entity.lastZ) * delta);
    }

    public static boolean friend(Entity entity) {
        return entity instanceof PlayerEntity p && Elexis.getInstance().getFriendStorage().isFriend(p.getGameProfile().name());
    }

    public static int color(Entity entity) {
        return friend(entity) ? 0xFF73E9B6 : ThemeColorUtils.getIconColor(10, entity.getId());
    }

    public static int alpha(int color, float alpha) {
        return (Math.max(0, Math.min(255, Math.round(alpha * 255))) << 24) | color & 0xFFFFFF;
    }

    public static void label(EventRender.Screen.Hud event, Vec3d world, String text, int color) {
        Vec3d p = Mathf.worldSpaceToScreenSpace(world);
        if (p == null || !Double.isFinite(p.x) || !Double.isFinite(p.y) || p.z < 0 || p.z >= 1) return;
        float scale = (float) mc.getWindow().getScaleFactor();
        float x = (float) p.x * scale, y = (float) p.y * scale;
        if (x < -100 || x > event.getViewportWidth() + 100 || y < -50 || y > event.getViewportHeight() + 50) return;
        var r = event.getRenderer();
        float width = r.measureText(FontRegistry.SF_MEDIUM, text, 13f).width;
        r.rect(x - width / 2 - 5, y - 15, width + 10, 20, 5f, 0xCE0E131E);
        r.text(FontRegistry.SF_MEDIUM, x - width / 2, y, 13f, text, color);
    }

    public static final class Frame implements AutoCloseable {
        private final Buffers buffers;
        private final boolean pooled;
        private final WorldGeometryEmitter emitter;

        public Frame(EventRender.World event) {
            if(shared==null) shared=new Buffers();
            pooled=!shared.busy;buffers=pooled?shared:new Buffers();buffers.busy=true;
            var consumer=buffers.consumers.getBuffer(Predictions.LINES_LAYER);
            if(buffers.emitter==null) buffers.emitter=new WorldGeometryEmitter(mc.gameRenderer.getCamera(),event.getMatrixStack().peek(),consumer);
            else buffers.emitter.reset(mc.gameRenderer.getCamera(),event.getMatrixStack().peek(),consumer);
            emitter=buffers.emitter;
        }

        public void line(Vec3d a, Vec3d b, int color) { emitter.emitLine(a, b, color); }
        public void line(double x1,double y1,double z1,double x2,double y2,double z2,int color) {emitter.emitLine(x1,y1,z1,x2,y2,z2,color);}

        public void box(Box b, int color) {
            for(int level=0;level<2;level++) {
                double y=level==0?b.minY:b.maxY;
                line(b.minX,y,b.minZ,b.maxX,y,b.minZ,color);line(b.maxX,y,b.minZ,b.maxX,y,b.maxZ,color);
                line(b.maxX,y,b.maxZ,b.minX,y,b.maxZ,color);line(b.minX,y,b.maxZ,b.minX,y,b.minZ,color);
            }
            line(b.minX,b.minY,b.minZ,b.minX,b.maxY,b.minZ,color);line(b.maxX,b.minY,b.minZ,b.maxX,b.maxY,b.minZ,color);
            line(b.maxX,b.minY,b.maxZ,b.maxX,b.maxY,b.maxZ,color);line(b.minX,b.minY,b.maxZ,b.minX,b.maxY,b.maxZ,color);
        }

        public void ring(Vec3d center, double radius, int color) {
            for (int i=1;i<=48;i++) {
                line(center.x+COS48[i-1]*radius,center.y,center.z+SIN48[i-1]*radius,
                        center.x+COS48[i]*radius,center.y,center.z+SIN48[i]*radius,color);
            }
        }

        public void sphere(Vec3d center, double radius, int color) {
            ring(center,radius,color);
            for (int axis=0;axis<2;axis++) {
                for(int i=1;i<=40;i++) {
                    double a=COS40[i-1]*radius,b=SIN40[i-1]*radius,c=COS40[i]*radius,d=SIN40[i]*radius;
                    line(center.x+(axis==0?a:0),center.y+b,center.z+(axis==1?a:0),
                            center.x+(axis==0?c:0),center.y+d,center.z+(axis==1?c:0),color);
                }
            }
        }

        @Override public void close() {
            try {buffers.consumers.draw();} finally {
                buffers.busy=false;
                if(pooled) buffers.allocator.clear();else buffers.allocator.close();
            }
        }
    }
}
