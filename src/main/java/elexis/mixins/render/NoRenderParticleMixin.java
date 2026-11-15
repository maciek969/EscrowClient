package elexis.mixins.render;

import elexis.modules.impl.render.NoRneder;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class NoRenderParticleMixin {

    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void noRender$particles(ParticleEffect parameters, double x, double y, double z,
                                    double velocityX, double velocityY, double velocityZ,
                                    CallbackInfoReturnable<Particle> cir) {
        if (parameters == null) return;

        if (NoRneder.is("Smoke")) {
            if (parameters.getType() == ParticleTypes.SMOKE
                    || parameters.getType() == ParticleTypes.LARGE_SMOKE
                    || parameters.getType() == ParticleTypes.CAMPFIRE_COSY_SMOKE
                    || parameters.getType() == ParticleTypes.CAMPFIRE_SIGNAL_SMOKE) {
                cir.setReturnValue(null);
                return;
            }
        }

        if (NoRneder.is("Bad effects")) {
            if (parameters.getType() == ParticleTypes.EFFECT
                    || parameters.getType() == ParticleTypes.INSTANT_EFFECT
                    || parameters.getType() == ParticleTypes.ENTITY_EFFECT) {
                cir.setReturnValue(null);
            }
        }
    }
}
