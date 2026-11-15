package elexis.mixins.client;

import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(method = "logGlError", at = @At("HEAD"), cancellable = true)
    public void onGlError(int error, long description, CallbackInfo ci) {
        ci.cancel();
    }
}