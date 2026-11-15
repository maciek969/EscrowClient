package elexis.mixins.render;

import elexis.Elexis;
import elexis.modules.impl.combat.HitAura;
import elexis.modules.impl.combat.auraProcess.rotationProcess.impl.RotationProcess;
import elexis.modules.impl.render.BabyBoy;
import elexis.modules.impl.misc.NameProtect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void Elexis$visualAntiAim(PlayerLikeEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (!state.isSwimming && state.leaningPitch == 0.0F
                && MinecraftClient.getInstance().player != null && player.getId() == MinecraftClient.getInstance().player.getId())
            elexis.modules.impl.render.visual.OverlayVisuals.applyAntiAim(state);
    }
    private static float Elexis$visualBodyYaw = Float.NaN;
    private static float Elexis$visualHeadYaw = Float.NaN;
    private static float Elexis$visualPitch = Float.NaN;

    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void Elexis$keepVanillaPlayerTags(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, CameraRenderState cameraState, CallbackInfo ci) {
        if (Elexis.getFunctionManager() == null) {
            return;
        }
        var nameTags = Elexis.getFunctionManager().getNameTags();
        if (nameTags != null) {
            nameTags.rememberVanillaPlayerLabel(state.playerName, state.displayName);
            if (nameTags.shouldHideVanillaPlayerLabels()) ci.cancel();
        }
        state.displayName = NameProtect.display(state.displayName);
    }

    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("TAIL"),
            require = 0
    )
    private void Elexis$applySilentRotationInF5(PlayerLikeEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player.getId() != player.getId()) {
            return;
        }

        if (state.isSwimming || state.leaningPitch > 0.0F
                || !HitAura.silentActive && !RotationProcess.isRotating() && HitAura.target == null) {
            Elexis$resetVisualRotation();
            return;
        }

        float targetHeadYaw = HitAura.silentActive ? HitAura.spoofYaw : mc.player.headYaw;
        float targetBodyYaw = mc.player.bodyYaw;
        float targetPitch = MathHelper.clamp(HitAura.silentActive ? HitAura.spoofPitch : mc.player.getPitch(), -32.0F, 24.0F);

        if (Float.isNaN(Elexis$visualHeadYaw) || Float.isNaN(Elexis$visualBodyYaw) || Float.isNaN(Elexis$visualPitch)) {
            Elexis$visualHeadYaw = targetHeadYaw;
            Elexis$visualBodyYaw = targetBodyYaw;
            Elexis$visualPitch = targetPitch;
        }

        float yawFactor = 0.22F;
        float pitchFactor = 0.18F;
        Elexis$visualHeadYaw += MathHelper.wrapDegrees(targetHeadYaw - Elexis$visualHeadYaw) * yawFactor;
        Elexis$visualBodyYaw += MathHelper.wrapDegrees(targetBodyYaw - Elexis$visualBodyYaw) * yawFactor;
        Elexis$visualPitch += (targetPitch - Elexis$visualPitch) * pitchFactor;

        state.bodyYaw = Elexis$visualBodyYaw;
        state.relativeHeadYaw = MathHelper.clamp(MathHelper.wrapDegrees(Elexis$visualHeadYaw - Elexis$visualBodyYaw), -75.0F, 75.0F);
        state.pitch = Elexis$visualPitch;
    }

    @Inject(method = "scale(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At("TAIL"), require = 0)
    private void Elexis$applyBabyBoyScale(PlayerEntityRenderState state, MatrixStack matrices, CallbackInfo ci) {
        if (BabyBoy.shouldScalePlayer(state.id)) {
            matrices.scale(0.5F, 0.5F, 0.5F);
        }
    }

    private static void Elexis$resetVisualRotation() {
        Elexis$visualBodyYaw = Float.NaN;
        Elexis$visualHeadYaw = Float.NaN;
        Elexis$visualPitch = Float.NaN;
    }
}



