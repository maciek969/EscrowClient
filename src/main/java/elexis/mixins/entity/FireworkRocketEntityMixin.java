package elexis.mixins.entity;

import elexis.Elexis;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {
    @Shadow private LivingEntity shooter;

    @ModifyArg(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"), index = 0)
    private Vec3d elexis$recordRocketImpulse(Vec3d vanillaVelocity) {
        var manager = Elexis.getFunctionManager();
        if (manager != null && shooter != null && shooter == elexis.client.utils.client.IMinecraft.mc.player) {
            manager.getElytraBooster().recordFirework(shooter.getVelocity());
        }
        return vanillaVelocity;
    }
}
