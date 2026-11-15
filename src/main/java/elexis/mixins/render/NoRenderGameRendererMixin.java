package elexis.mixins.render;

import elexis.modules.impl.render.NoRneder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class NoRenderGameRendererMixin {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true, require = 0)
    private void noRender$screenShake(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (NoRneder.is("Screen shake")) ci.cancel();
    }
}
