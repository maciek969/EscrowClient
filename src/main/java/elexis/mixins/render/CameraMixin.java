package elexis.mixins.render;

import elexis.Elexis;
import elexis.client.events.impl.render.EventCamera;
import elexis.client.events.impl.render.EventRotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    private EventRotation Elexis$rotationEvent;
    private float Elexis$originalYaw;
    private float Elexis$originalPitch;

    @Shadow
    private boolean thirdPerson;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void moveBy(float f, float g, float h);
    @Shadow protected abstract float clipToSpace(float f);

    @Inject(method = "update", at = @At("HEAD"))
    private void Elexis$cacheRotationEvent(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (focusedEntity != null) {
            this.Elexis$originalYaw = focusedEntity.getYaw(tickDelta);
            this.Elexis$originalPitch = focusedEntity.getPitch(tickDelta);
            this.Elexis$rotationEvent = new EventRotation(this.Elexis$originalYaw, this.Elexis$originalPitch, tickDelta);
            Elexis.getEventBus().post(this.Elexis$rotationEvent);
        } else {
            this.Elexis$rotationEvent = null;
        }
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift = At.Shift.AFTER), cancellable = true)
    public void onUpdate(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        EventCamera event = new EventCamera(false, 4);
        Elexis.getEventBus().post(event);

        if (event.isCancelled() && focusedEntity instanceof ClientPlayerEntity player && !player.isSleeping() && thirdPerson) {
            float pitch = this.Elexis$rotationEvent != null ? this.Elexis$rotationEvent.getPitch() : MinecraftClient.getInstance().player.getPitch();
            float yaw = this.Elexis$rotationEvent != null ? this.Elexis$rotationEvent.getYaw() : MinecraftClient.getInstance().player.getYaw();
            if (inverseView) {
                pitch = -pitch;
                yaw -= 180.0F;
            }
            float distance = event.getDistance();
            setRotation(yaw, pitch);
            moveBy(event.isCameraClip() ? -distance : -clipToSpace(distance), 0.0F, 0.0F);
            ci.cancel();
        }
    }

    @Redirect(
            method = "update",
            // Both initial branches use free look, before native front-view and bed adjustments.
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"),
            slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V")),
            require = 2,
            allow = 2
    )
    private void Elexis$redirectCameraRotation(Camera instance, float yaw, float pitch) {
        if (this.Elexis$rotationEvent == null
                || (this.Elexis$rotationEvent.getYaw() == this.Elexis$originalYaw
                && this.Elexis$rotationEvent.getPitch() == this.Elexis$originalPitch)) {
            this.setRotation(yaw, pitch);
        } else {
            this.setRotation(this.Elexis$rotationEvent.getYaw(), this.Elexis$rotationEvent.getPitch());
        }
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void Elexis$clearRotationEvent(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        this.Elexis$rotationEvent = null;
    }
}
