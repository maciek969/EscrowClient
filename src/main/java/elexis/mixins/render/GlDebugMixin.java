package elexis.mixins.render;

import net.minecraft.client.gl.GlDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlDebug.class)
public class GlDebugMixin {
    @Inject(method = "onDebugMessage", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V"), cancellable = true)
    public void suppressMatrixUniformError(int source, int type, int id, int severity, int length, long message, long l, CallbackInfo ci) {
        if (id == 1282) {
            ci.cancel();
        }
    }
}