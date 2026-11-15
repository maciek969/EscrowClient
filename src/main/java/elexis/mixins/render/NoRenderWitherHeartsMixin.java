package elexis.mixins.render;

import elexis.modules.impl.render.NoRneder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class NoRenderWitherHeartsMixin {

    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true, require = 0)
    private void noRender$witherHearts(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if (!NoRneder.is("Wither hearts")) return;
        if (effect != null && effect.equals(StatusEffects.WITHER)) {
            cir.setReturnValue(false);
        }
    }
}
