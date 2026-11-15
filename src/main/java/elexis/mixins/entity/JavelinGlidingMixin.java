package elexis.mixins.entity;

import elexis.Elexis;
import elexis.client.utils.client.IMinecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class JavelinGlidingMixin {
    @Inject(method = "calcGlidingVelocity", at = @At("RETURN"), cancellable = true)
    private void elexis$javelinFlight(Vec3d oldVelocity, CallbackInfoReturnable<Vec3d> cir) {
        var manager = Elexis.getFunctionManager();
        if (manager == null || (Object) this != IMinecraft.mc.player) return;
        Vec3d velocity = manager.getElytraBooster().modifyGlidingVelocity(cir.getReturnValue());
        velocity = manager.getElytraMotion().modifyGlidingVelocity(velocity);
        cir.setReturnValue(velocity);
    }
}
