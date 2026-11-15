package elexis.mixins.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import elexis.Elexis;
import elexis.client.events.impl.entity.EventCrosshairUpdate;
import elexis.client.events.impl.render.EventAspectRatio;
import elexis.client.events.impl.render.EventFov;
import elexis.client.utils.math.Mathf;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static elexis.client.utils.client.IMinecraft.mc;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "updateCrosshairTarget", at = @At("TAIL"))
    private void Elexis$triggerAfterCrosshairUpdate(float tickDelta, CallbackInfo ci) {
        var manager = Elexis.getFunctionManager();
        if (mc.world != null && mc.player != null && manager != null && manager.getTriggerBot() != null
                && manager.getTriggerBot().isState()) Elexis.getEventBus().post(new EventCrosshairUpdate());
    }

    @ModifyExpressionValue(method = "getFov", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", remap = false))
    private int hookGetFov(int original) {
        EventFov event = new EventFov();
        Elexis.getEventBus().post(event);
        if (event.isCancelled()) return event.getFov();
        return original;
    }

    @Inject(method = "getBasicProjectionMatrix", at = @At("TAIL"), cancellable = true)
    public void getBasicProjectionMatrixHook(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
        EventAspectRatio aspectRatioEvent = new EventAspectRatio();
        Elexis.getEventBus().post(aspectRatioEvent);

        if (aspectRatioEvent.isCancelled()) {
            Matrix4f matrix4f = new Matrix4f();
            matrix4f.perspective(fovDegrees * 0.01745329238474369F, aspectRatioEvent.getValue(), 0.05f, mc.gameRenderer.getFarPlaneDistance());
            cir.setReturnValue(matrix4f);
        }
    }

    @Inject(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
                    shift = Shift.AFTER
            ),
            require = 0
    )
    private void Elexis$captureWorldMatrices(RenderTickCounter tickCounter, CallbackInfo ci) {
        if (mc.player == null || mc.world == null || mc.gameRenderer == null) {
            return;
        }

        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(mc.gameRenderer.getCamera().getYaw() + 180.0f));

        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
        float fov = mc.gameRenderer.getFov(mc.gameRenderer.getCamera(), tickDelta, true);
        Mathf.lastProjMat.set(mc.gameRenderer.getBasicProjectionMatrix(fov));
        Mathf.lastModMat.set(RenderSystem.getModelViewMatrix());
        Mathf.lastWorldSpaceMatrix.set(matrixStack.peek().getPositionMatrix());
    }
}
