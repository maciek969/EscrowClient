package elexis.mixins.entity;

import elexis.modules.impl.misc.ScoreboardHealth;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class JavelinScoreboardHealthMixin {
    @Inject(method = "getHealth", at = @At("RETURN"), cancellable = true)
    private void elexis$readScore(CallbackInfoReturnable<Float> cir) {
        float health = cir.getReturnValueF();
        float score = ScoreboardHealth.read((LivingEntity)(Object)this, health);
        if (score != health) cir.setReturnValue(score);
    }
}
