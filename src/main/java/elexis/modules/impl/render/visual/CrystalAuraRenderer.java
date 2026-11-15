package elexis.modules.impl.render.visual;

import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.client.IMinecraft;
import elexis.modules.impl.combat.CrystalAura.VisualData;
import elexis.modules.impl.render.javelin.target.TargetRender;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

/** Astra's filled, outlined boxes with the original 300 ms cubic fade. */
public final class CrystalAuraRenderer implements IMinecraft {
    private static BufferAllocator allocator;
    private static VertexConsumerProvider.Immediate buffers;

    private CrystalAuraRenderer() {}

    public static void release() {
        if (allocator != null) allocator.close();
        allocator = null;
        buffers = null;
    }

    public static void render(EventRender.World event, Iterable<VisualData> visuals, long now, long duration) {
        if (allocator == null) {
            allocator = new BufferAllocator(4096);
            buffers = VertexConsumerProvider.immediate(allocator);
        }
        var camera = mc.gameRenderer.getCamera().getCameraPos();
        var matrix = event.getMatrixStack().peek().getPositionMatrix();
        var fill = buffers.getBuffer(TargetRender.shapes(true, false));
        try (var lines = new VisualGeometry.Frame(event)) {
            for (VisualData data : visuals) {
                float remaining = MathHelper.clamp(1 - (now - data.getStartedAt()) / (float) duration, 0, 1);
                float fade = remaining * remaining * remaining;
                if (fade <= .01f) continue;
                Box box = new Box(data.getPos());
                int color = (Math.round(data.getR() * 255) << 16) | (Math.round(data.getG() * 255) << 8)
                        | Math.round(data.getB() * 255);
                lines.box(box, (Math.round(fade * .85f * 255) << 24) | color);
                fillBox(fill, matrix, box.offset(-camera.x, -camera.y, -camera.z),
                        (Math.round(fade * .35f * 255) << 24) | color);
            }
            buffers.draw();
        } finally {
            allocator.clear();
        }
    }

    private static void fillBox(VertexConsumer out, Matrix4f matrix, Box b, int color) {
        quad(out, matrix, color, b.minX,b.minY,b.minZ, b.minX,b.minY,b.maxZ, b.minX,b.maxY,b.maxZ, b.minX,b.maxY,b.minZ);
        quad(out, matrix, color, b.maxX,b.minY,b.minZ, b.maxX,b.minY,b.maxZ, b.maxX,b.maxY,b.maxZ, b.maxX,b.maxY,b.minZ);
        quad(out, matrix, color, b.minX,b.minY,b.minZ, b.maxX,b.minY,b.minZ, b.maxX,b.minY,b.maxZ, b.minX,b.minY,b.maxZ);
        quad(out, matrix, color, b.minX,b.maxY,b.minZ, b.maxX,b.maxY,b.minZ, b.maxX,b.maxY,b.maxZ, b.minX,b.maxY,b.maxZ);
        quad(out, matrix, color, b.minX,b.minY,b.minZ, b.maxX,b.minY,b.minZ, b.maxX,b.maxY,b.minZ, b.minX,b.maxY,b.minZ);
        quad(out, matrix, color, b.minX,b.minY,b.maxZ, b.maxX,b.minY,b.maxZ, b.maxX,b.maxY,b.maxZ, b.minX,b.maxY,b.maxZ);
    }

    private static void quad(VertexConsumer out, Matrix4f m, int color,
                             double ax,double ay,double az,double bx,double by,double bz,
                             double cx,double cy,double cz,double dx,double dy,double dz) {
        vertex(out,m,ax,ay,az,color); vertex(out,m,bx,by,bz,color); vertex(out,m,cx,cy,cz,color);
        vertex(out,m,ax,ay,az,color); vertex(out,m,cx,cy,cz,color); vertex(out,m,dx,dy,dz,color);
    }

    private static void vertex(VertexConsumer out, Matrix4f m, double x, double y, double z, int color) {
        out.vertex(m, (float)x, (float)y, (float)z).color(color);
    }
}
