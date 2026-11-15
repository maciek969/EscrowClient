package elexis.modules.impl.combat.auraProcess.rotationProcess.impl;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.player.PlayerUtil;
import elexis.modules.impl.combat.HitAura;
import elexis.modules.impl.combat.auraProcess.auraUtil.GCDUtil;
import elexis.modules.impl.combat.auraProcess.rotationProcess.Component;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class RotationProcess extends Component {
    public static RotationTask currentTask = RotationTask.IDLE;
    public static float currentYawSpeed;
    public static float currentPitchSpeed;
    public static float currentYawReturnSpeed;
    public static float currentPitchReturnSpeed;
    public static int currentPriority;
    public static int currentTimeout;
    public static int idleTicks;
    public static Rotation targetRotation;

    public static void stop() {
        currentTask = RotationTask.IDLE;
        currentPriority = 0;
        idleTicks = 0;
        targetRotation = null;
        FreeLookUtil.active = false;
        HitAura.silentActive = false;
    }

    public static boolean isRotating() {
        return currentTask != RotationTask.IDLE;
    }

    @EventHandler
    public void onRender(EventRender.World event) {
        if (mc.player != null && isRotating()) {
            mc.player.bodyYaw = PlayerUtil.calculateCorrectYawOffset(mc.player.getYaw());
        }
    }

    @EventHandler
    public void onTick(EventUpdate.Fast event) {
        if (currentTask == RotationTask.AIM && idleTicks > currentTimeout) {
            currentTask = RotationTask.RESET;
        }

        if (currentTask == RotationTask.RESET) {
            Rotation resetTarget = new Rotation(FreeLookUtil.freeYaw, FreeLookUtil.freePitch);
            if (updateRotation(resetTarget, currentYawReturnSpeed, currentPitchReturnSpeed)) {
                stopRotation();
            }
        }

        idleTicks++;
    }

    public static void update(Rotation target, float yawSpeed, float pitchSpeed, float yawReturnSpeed, float pitchReturnSpeed, int timeout, int priority, boolean clientRotation) {
        if (currentPriority > priority) {
            return;
        }

        if (mc.player == null) {
            return;
        }

        if (currentTask == RotationTask.IDLE && !clientRotation) {
            FreeLookUtil.active = true;
        }

        currentYawSpeed = Math.min(yawSpeed, 90.0F);
        currentPitchSpeed = Math.min(pitchSpeed, 90.0F);
        currentYawReturnSpeed = Math.min(yawReturnSpeed, 90.0F);
        currentPitchReturnSpeed = Math.min(pitchReturnSpeed, 90.0F);
        currentTimeout = timeout;
        currentPriority = priority;
        currentTask = RotationTask.AIM;
        targetRotation = target == null ? null : new Rotation(target.yaw, MathHelper.clamp(target.pitch, -89.0F, 89.0F));
        updateRotation(targetRotation, currentYawSpeed, currentPitchSpeed);
    }

    public static void update(Rotation targetRotation, float turnSpeed, float returnSpeed, int timeout, int priority) {
        update(targetRotation, turnSpeed, turnSpeed, returnSpeed, returnSpeed, timeout, priority, false);
    }

    static boolean updateRotation(Rotation targetRotation, float yawSpeed, float pitchSpeed) {
        if (mc.player == null || targetRotation == null) {
            return false;
        }

        Rotation currentRotation = new Rotation(mc.player);
        float yawDelta = MathHelper.wrapDegrees(targetRotation.yaw - currentRotation.yaw);
        float pitchDelta = targetRotation.pitch - currentRotation.pitch;
        float clampedYaw = Math.min(Math.abs(yawDelta), yawSpeed);
        float clampedPitch = Math.min(Math.abs(pitchDelta), pitchSpeed);

        mc.player.setYaw(mc.player.headYaw = mc.player.headYaw + GCDUtil.getSensitivity(MathHelper.clamp(yawDelta, -clampedYaw, clampedYaw)));
        mc.player.setPitch(MathHelper.clamp(mc.player.getPitch() + GCDUtil.getSensitivity(MathHelper.clamp(pitchDelta, -clampedPitch, clampedPitch)), -90.0F, 90.0F));

        HitAura.silentActive = FreeLookUtil.active;
        if (HitAura.silentActive) {
            HitAura.spoofYaw = mc.player.getYaw();
            HitAura.spoofPitch = mc.player.getPitch();
        }

        idleTicks = 0;
        return new Rotation(mc.player).getDelta(targetRotation) < 1.0F;
    }

    public void stopRotation() {
        currentTask = RotationTask.IDLE;
        currentPriority = 0;
        FreeLookUtil.active = false;
        HitAura.silentActive = false;
    }

    public enum RotationTask {
        AIM,
        RESET,
        IDLE
    }
}
