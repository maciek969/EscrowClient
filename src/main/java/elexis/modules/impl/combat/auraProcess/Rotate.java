package elexis.modules.impl.combat.auraProcess;

import elexis.client.utils.client.IMinecraft;
import elexis.client.utils.math.Mathf;
import elexis.modules.impl.combat.HitAura;
import elexis.modules.impl.combat.auraProcess.auraUtil.AuraUtil;
import elexis.modules.impl.combat.auraProcess.rotationProcess.impl.Rotation;
import elexis.modules.impl.combat.auraProcess.rotationProcess.impl.RotationProcess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public final class Rotate implements IMinecraft {
    private static int tick;

    private Rotate() {
    }

    public static void resetTransientState() {
        tick = 0;
    }

    private static Vec3d getAimPoint(LivingEntity target) {
        Vec3d center = target.getBoundingBox().getCenter();
        if (!HitAura.smartAim.get()) {
            return center;
        }
        Vec3d point = AuraUtil.getClosestTargetPoint(target);
        return point == null ? center : point;
    }

    public static void onQuickRotation(LivingEntity target, float yawSpeed, float pitchSpeed, float smoothing) {
        Vec3d direction = getAimPoint(target).subtract(mc.player.getEyePos());
        float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z)));
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        float yawDelta = MathHelper.wrapDegrees(targetYaw - yaw);
        float pitchDelta = targetPitch - pitch;
        float yawStep = Math.abs(yawDelta) > 3.0F ? yawDelta * smoothing : yawDelta;
        float pitchStep = Math.abs(pitchDelta) > 3.0F ? pitchDelta * smoothing : pitchDelta;
        RotationProcess.update(new Rotation(yaw + yawStep, pitch + pitchStep),
                yawSpeed, pitchSpeed, 40.0F, 32.0F, 1, 15, false);
    }

    public static void onSpookyRotation(LivingEntity target, boolean attack) {
        Vec3d vec = getAimPoint(target).subtract(mc.player.getEyePos());
        boolean attackF = false;
        if (attack) {
            tick = 4;
        }

        if (tick > 0) {
            attackF = true;
            tick--;
        }

        float yaw = (float)Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float pitch = (float)MathHelper.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90.0, 90.0);
        float randomToAttack = 0.0F;
        if (attackF) {
            randomToAttack = Mathf.random(-3.0F, 4.0F) + (float)(2.0 * Math.sin(System.currentTimeMillis() / 30.0));
        }

        float randomXY = Mathf.random(-3.0F, 3.0F) + (float)(3.0 * Math.cos(System.currentTimeMillis() / 40.0));
        float randomX = Mathf.random(-1.0F, 1.0F) + (float)(6.0 * Math.sin(System.currentTimeMillis() / 240.0));
        Rotation newRotation = new Rotation(yaw + randomXY + randomToAttack, pitch + randomX);
        RotationProcess.update(newRotation, Mathf.randomInt(90, 110), Mathf.randomValue(16.0F, 21.0F), 30.0F, 30.0F, 1, 15, false);
    }
}
