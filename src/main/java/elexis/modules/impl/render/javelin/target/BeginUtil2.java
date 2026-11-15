package elexis.modules.impl.render.javelin.target;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;

/** Bounded triangle batch used by Javelin's ring, crystal and blade models. */
public final class BeginUtil2 {
    private static final int MAX_VERTICES = 8192;
    private static final float[] POSITIONS = new float[MAX_VERTICES * 3];
    private static final int[] COLORS = new int[MAX_VERTICES];
    private static int count;
    private static BufferAllocator allocator;
    private static VertexConsumerProvider.Immediate immediate;

    private BeginUtil2() {}
    public static void begin() { count = 0; }

    public static void tri(float x1, float y1, float z1, float x2, float y2, float z2,
                           float x3, float y3, float z3, int color) {
        tri(x1,y1,z1,color,x2,y2,z2,color,x3,y3,z3,color);
    }

    public static void tri(float x1, float y1, float z1, int c1, float x2, float y2, float z2, int c2,
                           float x3, float y3, float z3, int c3) {
        if (count + 3 > MAX_VERTICES) return;
        vertex(x1,y1,z1,c1);
        vertex(x2,y2,z2,c2);
        vertex(x3,y3,z3,c3);
    }

    private static void vertex(float x, float y, float z, int color) {
        int position = count * 3;
        POSITIONS[position] = x;
        POSITIONS[position + 1] = y;
        POSITIONS[position + 2] = z;
        COLORS[count++] = color;
    }

    public static void flush(boolean throughWalls) { flush(throughWalls, false); }
    public static void flushAdditive(boolean throughWalls) { flush(throughWalls, true); }

    private static void flush(boolean throughWalls, boolean additive) {
        if (count == 0) return;
        if (allocator == null) {
            allocator = new BufferAllocator(262144);
            immediate = VertexConsumerProvider.immediate(allocator);
        }
        VertexConsumer vertices = immediate.getBuffer(TargetRender.shapes(throughWalls, additive));
        for (int i = 0; i < count; i++) {
            int position = i * 3;
            vertices.vertex(TargetRender.MATRIX, POSITIONS[position], POSITIONS[position+1], POSITIONS[position+2]).color(COLORS[i]);
        }
        immediate.draw();
        count = 0;
    }

    public static void close() {
        immediate = null;
        if (allocator != null) allocator.close();
        allocator = null;
        count = 0;
    }
}
