package elexis.client.utils.player;

import elexis.client.utils.client.IMinecraft;
import elexis.modules.impl.combat.HitAura;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class MoveUtil implements IMinecraft {
    private MoveUtil() {
    }

    public static double direction(float rotationYaw, float moveForward, float moveStrafing) {
        if (moveForward < 0.0F) {
            rotationYaw += 180.0F;
        }

        float forward = 1.0F;
        if (moveForward < 0.0F) {
            forward = -0.5F;
        }
        if (moveForward > 0.0F) {
            forward = 0.5F;
        }

        if (moveStrafing > 0.0F) {
            rotationYaw -= 90.0F * forward;
        }
        if (moveStrafing < 0.0F) {
            rotationYaw += 90.0F * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    public static float[] getMovementFromKeys() {
        float forward = 0.0F;
        float strafe = 0.0F;
        var window = mc.getWindow();

        if (InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode())) forward++;
        if (InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode())) forward--;
        if (InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode())) strafe++;
        if (InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode())) strafe--;

        return new float[]{forward, strafe};
    }

    public static void fixMovement(float cameraYaw) {
        float[] movement = getMovementFromKeys();
        float forward = movement[0];
        float strafe = movement[1];
        if (forward == 0.0F && strafe == 0.0F) {
            return;
        }

        double angle = MathHelper.wrapDegrees(Math.toDegrees(direction(mc.player.isFlyingVehicle() ? mc.player.getYaw() : cameraYaw, forward, strafe)));
        float closestForward = 0.0F;
        float closestStrafe = 0.0F;
        float closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward++) {
            for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe++) {
                if (predictedStrafe == 0.0F && predictedForward == 0.0F) {
                    continue;
                }
                double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(mc.player.getYaw(), predictedForward, predictedStrafe)));
                double difference = Math.abs(angle - predictedAngle);
                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        mc.options.forwardKey.setPressed(closestForward > 0.0F);
        mc.options.backKey.setPressed(closestForward < 0.0F);
        mc.options.leftKey.setPressed(closestStrafe > 0.0F);
        mc.options.rightKey.setPressed(closestStrafe < 0.0F);
    }

    public static void targetMovement(float cameraYaw, Vec3d position) {
        if (HitAura.target == null || mc.player == null) {
            return;
        }

        float[] movement = getMovementFromKeys();
        float forward = movement[0];
        float strafe = movement[1];
        if (forward == 0.0F && strafe == 0.0F) {
            return;
        }

        Box box = HitAura.target.getBoundingBox();
        double randX = MathHelper.lerp(Math.random(), box.minX, box.maxX);
        double randY = MathHelper.lerp(Math.random(), box.minY, box.maxY);
        double randZ = MathHelper.lerp(Math.random(), box.minZ, box.maxZ);
        randY = MathHelper.clamp(randY, HitAura.target.getY() + 0.2, HitAura.target.getY() + HitAura.target.getHeight() - 0.2);
        Vec3d randomHitVec = new Vec3d(randX, randY, randZ);
        Vec3d direction = randomHitVec.subtract(mc.player.getEyePos()).normalize();
        float targetYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);
        double angle = MathHelper.wrapDegrees(Math.toDegrees(direction(mc.player.isFlyingVehicle() ? mc.player.getYaw() : targetYaw, forward, strafe)));

        float closestForward = 0.0F;
        float closestStrafe = 0.0F;
        float closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward++) {
            for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe++) {
                if (predictedStrafe == 0.0F && predictedForward == 0.0F) {
                    continue;
                }
                double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(mc.player.getYaw(), predictedForward, predictedStrafe)));
                double difference = Math.abs(angle - predictedAngle);
                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        mc.options.forwardKey.setPressed(closestForward > 0.0F);
        mc.options.backKey.setPressed(closestForward < 0.0F);
        mc.options.leftKey.setPressed(closestStrafe > 0.0F);
        mc.options.rightKey.setPressed(closestStrafe < 0.0F);
    }
}
