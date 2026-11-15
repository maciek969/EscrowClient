package elexis.modules.impl.render;

import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.client.utils.render.main.oldrender.system.ClientPipelines;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.ColorSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;

@FunctionAdd(name = "ChinaHatCustom", alias = "ChinaHatCustom", category = Category.Render, description = "Custom China Hat with settings")
@Environment(EnvType.CLIENT)
public class ChinaHatCustom extends Function {
    private final ColorSetting color = new ColorSetting("Color", new Color(255, 255, 255, 255).getRGB());
    private final SliderSetting height = new SliderSetting("Height", 0.16f, 0.0f, 0.5f, 0.01f);
    private final SliderSetting radius = new SliderSetting("Radius", 0.72f, 0.3f, 1.5f, 0.01f);
    private final SliderSetting segments = new SliderSetting("Segments", 72, 12, 144, 1);
    private final SliderSetting yOffset = new SliderSetting("Y Offset", 0.28f, 0.0f, 0.5f, 0.01f);

    public ChinaHatCustom() {
        addSettings(color, height, radius, segments, yOffset);
    }

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
            float y = (float) (pos.y - camera.y + player.getHeight() + height.get());
            float z = (float) (pos.z - camera.z);
            
            int col = color.get();
            int topColor = col;
            buffer.vertex(matrix, x, y + yOffset.get(), z).color((topColor >> 16) & 255, (topColor >> 8) & 255, topColor & 255, (topColor >> 24) & 255);
            
            int segCount = (int) (float) segments.get();
            for (int i = 0; i <= segCount; i++) {
                float angle = (float) (Math.PI * 2.0 * i / segCount);
                float px = x + MathHelper.cos(angle) * radius.get();
                float pz = z + MathHelper.sin(angle) * radius.get();
                int color = col;
                buffer.vertex(matrix, px, y, pz).color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >> 24) & 255);
            }
            immediate.draw();
        } finally {
            allocator.close();
        }
    }
}
