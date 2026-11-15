package elexis.mixins.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import elexis.Elexis;
import elexis.modules.impl.combat.CrystalAura;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class CrystalAuraMovementMixin {
    private static CrystalAura elexis$crystalAura() {
        var manager = Elexis.getFunctionManager();
        return manager == null ? null : manager.getCrystalAura();
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float elexis$crystalYaw(float original) {
        var aura = elexis$crystalAura();
        var rotation = aura == null ? null : aura.getMovementRotation();
        return rotation == null ? original : rotation.x;
    }

    @ModifyExpressionValue(method = "sendMovementPackets", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float elexis$crystalPitch(float original) {
        var aura = elexis$crystalAura();
        var rotation = aura == null ? null : aura.getMovementRotation();
        return rotation == null ? original : rotation.y;
    }

    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void elexis$crystalPostTick(CallbackInfo ci) {
        var aura = elexis$crystalAura();
        if (aura != null) aura.afterMovement();
    }
}
