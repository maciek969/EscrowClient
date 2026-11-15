package elexis.mixins.render;

import elexis.modules.impl.render.NoRneder;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class NoRenderWorldRendererMixin {

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true, require = 0)
    private void noRender$weather(CallbackInfo ci) {
        if (NoRneder.is("Weather")) ci.cancel();
    }
}
