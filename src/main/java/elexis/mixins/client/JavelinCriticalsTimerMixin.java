package elexis.mixins.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import elexis.modules.impl.combat.Criticals;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderTickCounter.Dynamic.class)
public abstract class JavelinCriticalsTimerMixin {
    @ModifyExpressionValue(method="beginRenderTick(J)I", at=@At(value="INVOKE",
            target="Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F", remap=false))
    private float javelin$criticalTickTime(float milliseconds) {
        return milliseconds / Criticals.timerMultiplier();
    }
}
