//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package elexis.mixins.entity;

import com.mojang.authlib.GameProfile;
import elexis.Elexis;
import elexis.client.events.impl.entity.EventMotion;
import elexis.client.events.impl.entity.EventMove;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.entity.SlowWalkingEvent;
import elexis.client.utils.client.IMinecraft;
import elexis.modules.impl.combat.HitAura;
import elexis.modules.impl.combat.auraProcess.rotationProcess.impl.FreeLookUtil;
import elexis.modules.impl.player.PlayerUtilsFunction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayerEntity.class})
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
    @Unique
    private float Elexis$preSilentYaw;
    @Unique
    private float Elexis$preSilentPitch;
    @Unique
    private boolean Elexis$restoreSilentRotations;

    protected ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(
            at = {@At("HEAD")},
            method = {"tick"}
    )
    public void onUpdate(CallbackInfo ci) {
        if (IMinecraft.mc.world != null && IMinecraft.mc.player != null) {
            Elexis.getEventBus().post(new EventUpdate.Slow());
        }

    }

    @Redirect(
            method = {"tickMovementInput"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"
            ),
            require = 0
    )
    private float Elexis$redirectMovementInputPitch(ClientPlayerEntity instance) {
        return !FreeLookUtil.active ? instance.getPitch() : MinecraftClient.getInstance().gameRenderer.getCamera().getPitch();
    }

    @Redirect(
            method = {"tickMovementInput"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"
            ),
            require = 0
    )
    private float Elexis$redirectMovementInputYaw(ClientPlayerEntity instance) {
        return !FreeLookUtil.active ? instance.getYaw() : MinecraftClient.getInstance().gameRenderer.getCamera().getYaw();
    }

    @Inject(
            at = {@At("HEAD")},
            method = {"tickMovement"}
    )
    public void onTickMovementHead(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        float preYaw = player.getYaw();
        float prePitch = player.getPitch();
        this.Elexis$restoreSilentRotations = HitAura.silentActive;
        if (this.Elexis$restoreSilentRotations) {
            this.Elexis$preSilentYaw = preYaw;
            this.Elexis$preSilentPitch = prePitch;
        }

        Box box = player.getBoundingBox();
        EventMotion event = new EventMotion(player.getYaw(), player.getPitch(), player.getX(), player.getY(), player.getZ(), player.isOnGround(), box, (Runnable)null);
        Elexis.getEventBus().post(event);
        if (!event.isCancelled()) {
            if (event.isRestoreRotation() && !this.Elexis$restoreSilentRotations) {
                this.Elexis$preSilentYaw = preYaw;
                this.Elexis$preSilentPitch = prePitch;
                this.Elexis$restoreSilentRotations = true;
            }

            if (event.getYaw() != player.getYaw() || event.getPitch() != player.getPitch()) {
                player.setYaw(event.getYaw());
                player.setPitch(event.getPitch());
            }

            if (event.getPosX() != player.getX() || event.getPosY() != player.getY() || event.getPosZ() != player.getZ()) {
                player.refreshPositionAndAngles(event.getPosX(), event.getPosY(), event.getPosZ(), event.getYaw(), event.getPitch());
            }

            if (event.isOnGround() != player.isOnGround()) {
                player.setOnGround(event.isOnGround());
            }

            if (event.getPostMotion() != null) {
                event.getPostMotion().run();
            }
        }

    }

    @Inject(
            at = {@At("RETURN")},
            method = {"tickMovement"}
    )
    public void onTickMovementReturn(CallbackInfo ci) {
        if (this.Elexis$restoreSilentRotations) {
            ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
            player.setYaw(this.Elexis$preSilentYaw);
            player.setPitch(this.Elexis$preSilentPitch);
            this.Elexis$restoreSilentRotations = false;
        }
    }

    @Inject(
            at = {@At("HEAD")},
            method = {"pushOutOfBlocks"},
            cancellable = true
    )
    public void noPush(CallbackInfo ci) {
        if ((Boolean)PlayerUtilsFunction.NoPush.get() && Elexis.getFunctionManager().getPlayerUtilsFunction().isState()) {
            ci.cancel();
        }

    }

    @Inject(
            method = {"move"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"
            )},
            cancellable = true
    )
    private void Elexis$move(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        EventMove event = new EventMove(movement.x, movement.y, movement.z);
        Elexis.getEventBus().post(event);
        if (event.isCancelled()) {
            double prevX = this.getX();
            double prevZ = this.getZ();
            super.move(movementType, event.getMovement());
            this.autoJump((float)(this.getX() - prevX), (float)(this.getZ() - prevZ));
            ci.cancel();
        }
    }

    @Redirect(
            method = {"applyMovementSpeedFactors"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/Vec2f;multiply(F)Lnet/minecraft/util/math/Vec2f;",
                    ordinal = 1
            ),
            require = 0
    )
    private Vec2f Elexis$noSlow(Vec2f vec, float multiplier) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        if (multiplier == 0.2F && player.isUsingItem() && !player.hasVehicle()) {
            SlowWalkingEvent event = new SlowWalkingEvent(vec.y, vec.x);
            Elexis.getEventBus().post(event);
            if (event.isCancelled()) {
                return vec;
            }
        }

        return vec.multiply(multiplier);
    }

    @Shadow
    protected abstract void autoJump(float var1, float var2);
}
