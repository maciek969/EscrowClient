package elexis.modules.impl.render;

import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.client.utils.render.main.oldrender.system.ClientPipelines;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@FunctionAdd(name = "ChinaHat", alias = "ChinaHat", category = Category.Render, description = "Draws a hat above player")
@Environment(EnvType.CLIENT)
public class ChinaHat extends Function {
    @EventHandler
    public void onRender(EventRender.World event) {
        if (nullCheck() || mc.options.getPerspective().isFirstPerson()) {
            return;
        }
        PlayerEntity player = mc.player;
        Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();
        float tickDelta = event.getTicks();
        Vec3d pos = new Vec3d(
                player.lastX + (player.getX() - player.lastX) * tickDelta,
                player.lastY + (player.getY() - player.lastY) * tickDelta,
                player.lastZ + (player.getZ() - player.lastZ) * tickDelta
        );

        BufferAllocator allocator = new BufferAllocator(8192);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        try {
            VertexConsumer buffer = immediate.getBuffer(ClientPipelines.WORLD_TRIANGLE_FAN);
            Matrix4f matrix = event.getMatrixStack().peek().getPositionMatrix();
            float x = (float) (pos.x - camera.x);
            float y = (float) (pos.y - camera.y + player.getHeight() + 0.16F);
            float z = (float) (pos.z - camera.z);
            int topColor = ThemeColorUtils.applyOpacity(ThemeColorUtils.getIconColor(8, 0), 180);
            buffer.vertex(matrix, x, y + 0.28F, z).color((topColor >> 16) & 255, (topColor >> 8) & 255, topColor & 255, (topColor >> 24) & 255);
            for (int i = 0; i <= 72; i++) {
                float angle = (float) (Math.PI * 2.0 * i / 72.0);
                float px = x + MathHelper.cos(angle) * 0.72F;
                float pz = z + MathHelper.sin(angle) * 0.72F;
                int color = ThemeColorUtils.applyOpacity(ThemeColorUtils.getIconColor(8, i * 7), 95);
                buffer.vertex(matrix, px, y, pz).color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >> 24) & 255);
            }
            immediate.draw();
        } finally {
            allocator.close();
        }
    }
}
