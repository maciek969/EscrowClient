package elexis.mixins.entity;

import elexis.Elexis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class CustomSwingDurationMixin {
    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void elexis$customSwingDuration(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) return;

        var manager = Elexis.getFunctionManager();
        var swing = manager == null ? null : manager.getCustomSwing();
        if (swing != null && swing.isAnimationActive()) {
            cir.setReturnValue(swing.getSwingDuration());
        }
    }
}
