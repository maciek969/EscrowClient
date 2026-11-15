package elexis.mixins.render;

import net.minecraft.client.gl.DynamicUniformStorage;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DynamicUniformStorage.class)
public abstract class DynamicUniformStorageMixin {
    @Redirect(
            method = {
                    "write(Lnet/minecraft/client/gl/DynamicUniformStorage$Uploadable;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;",
                    "writeAll([Lnet/minecraft/client/gl/DynamicUniformStorage$Uploadable;)[Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"
            ),
            require = 0,
            allow = 2
    )
    public void antiRenderLogSelfcode(Logger logger, String format, Object arg1, Object arg2) {
    }
}
