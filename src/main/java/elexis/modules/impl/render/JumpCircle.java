package elexis.modules.impl.render;

import elexis.client.events.impl.entity.EventMotion;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.client.utils.render.main.oldrender.system.ClientPipelines;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.SliderSetting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

@FunctionAdd(name = "JumpCircle", alias = "Jump Circle", category = Category.Render, description = "Shows a textured circle when you jump")
public class JumpCircle extends Function {
    private static final Identifier JUMP_TEXTURE = Identifier.of("elexis", "jump/jump.png");
    public static final SliderSetting size = new SliderSetting("Size", 1.6F, 0.5F, 4.0F, 0.1F, false);
    public static final SliderSetting life = new SliderSetting("Life", 900.0F, 300.0F, 2000.0F, 50.0F, false);

    private final List<Circle> circles = new ArrayList<>();
    private boolean wasOnGround;

    public JumpCircle() {
        addSettings(size, life);
    }

    @Override
    public void onEnable() {
        circles.clear();
        wasOnGround = mc.player != null && mc.player.isOnGround();
        super.onEnable();
    }

    @EventHandler
    public void onMotion(EventMotion event) {
        if (mc.player == null) {
            circles.clear();
            return;
        }
        boolean onGround = mc.player.isOnGround();
        if (wasOnGround && !onGround && mc.player.getVelocity().y > 0.0) {
            circles.add(new Circle(new Vec3d(mc.player.getX(), mc.player.getY() + 0.03, mc.player.getZ())));
        }
        wasOnGround = onGround;
        circles.removeIf(circle -> circle.age() > life.get());
    }

    @EventHandler
    public void onRender3D(EventRender.World event) {
        if (circles.isEmpty() || mc.player == null || mc.world == null) {
            return;
        }
        BufferAllocator allocator = new BufferAllocator(262144);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        try {
            VertexConsumer buffer = immediate.getBuffer(ClientPipelines.TARGET_ESP.apply(JUMP_TEXTURE));
            Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
            MatrixStack matrices = event.getMatrixStack();
            for (Circle circle : circles) {
                float progress = Math.min(1.0f, circle.age() / life.get());
                float scale = size.get() * (0.45f + progress * 0.85f);
                int color = ThemeColorUtils.applyOpacity(ThemeColorUtils.getIconColor(7, circle.hashCode()), (int) ((1.0f - progress) * 220.0f));
                matrices.push();
                matrices.translate(circle.position.x - cam.x, circle.position.y - cam.y, circle.position.z - cam.z);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((System.currentTimeMillis() / 8L) % 360L));
                drawTexturedQuad(buffer, matrices.peek(), scale, color);
                matrices.pop();
            }
            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    private void drawTexturedQuad(VertexConsumer buffer, MatrixStack.Entry entry, float size, int color) {
        Matrix4f matrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();
        Vector3f normal = new Vector3f(0.0f, 0.0f, 1.0f);
        normalMatrix.transform(normal);
        normal.normalize();
        float half = size * 0.5f;
        put(buffer, matrix, normal, -half, -half, 0f, 1f, color);
        put(buffer, matrix, normal, half, -half, 1f, 1f, color);
        put(buffer, matrix, normal, half, half, 1f, 0f, color);
        put(buffer, matrix, normal, -half, half, 0f, 0f, color);
    }

    private void put(VertexConsumer buffer, Matrix4f matrix, Vector3f normal, float x, float y, float u, float v, int color) {
        buffer.vertex(matrix, x, y, 0.0f)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(15728880)
                .normal(normal.x, normal.y, normal.z);
    }

    private static final class Circle {
        private final Vec3d position;
        private final long createdAt = System.currentTimeMillis();

        private Circle(Vec3d position) {
            this.position = position;
        }

        private long age() {
            return System.currentTimeMillis() - createdAt;
        }
    }
}
