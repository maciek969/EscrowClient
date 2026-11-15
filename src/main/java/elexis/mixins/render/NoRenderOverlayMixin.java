package elexis.mixins.render;

import elexis.modules.impl.render.NoRneder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.tag.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class NoRenderOverlayMixin {

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void noRender$fire(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void noRender$waterOverlay(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (NoRneder.is("\u041e\u0432\u0435\u0440\u043b\u0435\u0439 \u0432\u043e\u0434\u044b")) ci.cancel();
    }

    @Inject(method = "setFloatingItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void noRender$totemAnim(net.minecraft.item.ItemStack stack, net.minecraft.util.math.random.Random random, CallbackInfo ci) {
        if (NoRneder.is("\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u044f \u0442\u043e\u0442\u0435\u043c\u0430")) ci.cancel();
    }

    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void noRender$blockOverlay(Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (NoRneder.is("\u041e\u0432\u0435\u0440\u043b\u0435\u0439 \u0431\u043b\u043e\u043a\u043e\u0432")) ci.cancel();
    }

    @Inject(method = "renderOverlays", at = @At("HEAD"), cancellable = true, require = 0)
    private void noRender$lavaOverlay(boolean hudHidden, float tickDelta, OrderedRenderCommandQueue queue, CallbackInfo ci) {
        if (!NoRneder.is("\u041e\u0432\u0435\u0440\u043b\u0435\u0439 \u043b\u0430\u0432\u044b")) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.player.getFluidHeight(FluidTags.LAVA) > 0) {
            ci.cancel();
        }
    }
}