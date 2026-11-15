package elexis.mixins.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import elexis.modules.impl.combat.TargetPearl;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class JavelinPearlMovementMixin {
    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float elexis$pearlYaw(float original) {
        var rotation = TargetPearl.getMovementRotation();
        return rotation == null ? original : rotation.yaw();
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float elexis$pearlPitch(float original) {
        var rotation = TargetPearl.getMovementRotation();
        return rotation == null ? original : rotation.pitch();
    }

    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void elexis$finishPearlRotation(CallbackInfo ci) {
        TargetPearl.finishMovementRotation();
    }
}
