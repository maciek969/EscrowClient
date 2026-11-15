package elexis.mixins.render;

import elexis.modules.impl.combat.HitAura;
import elexis.Elexis;
import elexis.modules.impl.render.SeeInvisible;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = "getMixColor", at = @At("RETURN"), cancellable = true)
    private void Elexis$visualModelTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(elexis.modules.impl.render.visual.ModelVisuals.tint(state, cir.getReturnValue()));
    }

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void Elexis$visualModelState(LivingEntity entity, LivingEntityRenderState state, float ticks, CallbackInfo ci) {
        elexis.modules.impl.render.visual.ModelVisuals.update(entity, state);
    }
    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"),
            require = 0
    )
    private void Elexis$restoreOldSilentVisualRotation(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        SeeInvisible seeInvisible = Elexis.getFunctionManager() == null ? null : Elexis.getFunctionManager().getSeeInvisible();
        if (seeInvisible != null && seeInvisible.isState() && entity.isInvisible()) {
            state.invisible = false;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || entity instanceof PlayerEntity || entity.getId() != mc.player.getId() || !HitAura.silentActive) {
            return;
        }

        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.lastBodyYaw, entity.bodyYaw);
        float headYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.lastHeadYaw, entity.headYaw);
        state.bodyYaw = bodyYaw;
        state.relativeHeadYaw = MathHelper.wrapDegrees(headYaw - bodyYaw);
        state.pitch = MathHelper.clamp(entity.getPitch(tickDelta), -12.0F, 12.0F);
    }
}
