package elexis.modules.impl.combat.auraProcess.auraUtil;

import elexis.client.utils.client.IMinecraft;
import elexis.modules.impl.combat.auraProcess.rotationProcess.impl.Rotation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import org.joml.Vector4f;

public final class AuraUtil implements IMinecraft {
    private AuraUtil() {
    }

    private static Vec3d entityPos(Entity entity) {
        return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
    }

    public static double getStrictDistance(Entity entity) {
        return getClosestVec(entity).length();
    }

    public static Rotation cameraAngle() {
        return new Rotation(mc.player.getYaw(), mc.player.getPitch());
    }

    public static boolean validDistance(Entity entity, float distance, boolean smart) {
        return getStrictDistance(entity) < distance;
    }

    public static Vec3d getClosestVec(Entity entity) {
        Vec3d eyePosVec = mc.player.getEyePos();
        return getClosestVec(eyePosVec, entity).subtract(eyePosVec);
    }

    public static Vec3d getClosestVec(Vec3d vec, Box box) {
        return new Vec3d(
                MathHelper.clamp(vec.x, box.minX, box.maxX),
                MathHelper.clamp(vec.y, box.minY, box.maxY),
                MathHelper.clamp(vec.z, box.minZ, box.maxZ)
        );
    }

    public static Vec3d getClosestVec(Vec3d vec, Entity entity) {
        return getClosestVec(vec, entity.getBoundingBox());
    }

    public static Vec3d getVector3(LivingEntity target) {
        return target.getBoundingBox().getCenter().subtract(entityPos(mc.player)).add(0.0, -0.8F, 0.0);
    }

    public static Vec3d getClosestTargetPoint(Vec3d vec, Entity entity, float point) {
        if (entity == null) {
            return Vec3d.ZERO;
        }

        Box box = entity.getBoundingBox().expand(-point);
        Vec3d center = box.getCenter();
        Vec3d closestPoint = null;
        double closestDistance = Double.MAX_VALUE;

        for (double offsetX = 0.0; offsetX <= (box.maxX - box.minX) / 2.0; offsetX += 0.1) {
            for (double offsetY = 0.0; offsetY <= (box.maxY - box.minY) / 2.0; offsetY += 0.1) {
                for (double offsetZ = 0.0; offsetZ <= (box.maxZ - box.minZ) / 2.0; offsetZ += 0.1) {
                    for (int signX : new int[]{-1, 1}) {
                        for (int signY : new int[]{-1, 1}) {
                            for (int signZ : new int[]{-1, 1}) {
                                Vec3d potentialPoint = new Vec3d(center.x + signX * offsetX, center.y + signY * offsetY, center.z + signZ * offsetZ);
                                Vector2f rotation = calculate(potentialPoint);
                                if (RayTraceUtil.calculateRayTrace(6.0, rotation.x, rotation.y, mc.player, false) instanceof EntityHitResult hit && hit.getEntity().equals(entity)) {
                                    double distance = vec.distanceTo(potentialPoint);
                                    if (distance < closestDistance) {
                                        closestDistance = distance;
                                        closestPoint = potentialPoint;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (closestPoint != null) {
            return closestPoint;
        }

        return new Vec3d(
                MathHelper.clamp(vec.x, box.minX, box.maxX),
                MathHelper.clamp(vec.y, box.minY, box.maxY),
                MathHelper.clamp(vec.z, box.minZ, box.maxZ)
        );
    }

    public static Vector2f calculate(Vec3d toVec) {
        return calculate(entityPos(mc.player).add(0.0, mc.player.getEyeY(), 0.0), toVec);
    }

    public static Vector2f calculate(Vec3d fromVec, Vec3d toVec) {
        Vec3d diff = toVec.subtract(fromVec);
        double distance = Math.hypot(diff.x, diff.z);
        float yaw = (float) (MathHelper.atan2(diff.z, diff.x) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) (-(MathHelper.atan2(diff.y, distance) * (180.0 / Math.PI)));
        return new Vector2f(yaw, pitch);
    }

    public static Vec3d getClosestTargetPoint(Entity entity) {
        float tickDelta = mc.getRenderTickCounter().getTickProgress(false);
        return getClosestTargetPoint(mc.player.getCameraPosVec(tickDelta), entity, Math.min(entity.getWidth(), entity.getHeight()) / 4.0F);
    }

    public static Vector4f calculateRotationFromCamera(LivingEntity target) {
        float tickDelta = mc.getRenderTickCounter().getTickProgress(false);
        Vec3d eyePos = mc.player.getCameraPosVec(tickDelta);
        Vec3d vec = getClosestTargetPoint(target).subtract(eyePos);
        float rawYaw = MathHelper.wrapDegrees((float) (Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0));
        float rawPitch = (float) (-Math.toDegrees(Math.atan2(vec.y, Math.sqrt(vec.x * vec.x + vec.z * vec.z))));
        float yawDelta = MathHelper.wrapDegrees(rawYaw - mc.player.getYaw());
        float pitchDelta = rawPitch - mc.player.getPitch();
        return new Vector4f(rawYaw, rawPitch, yawDelta, pitchDelta);
    }
}
