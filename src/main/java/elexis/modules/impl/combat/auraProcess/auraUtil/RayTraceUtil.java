package elexis.modules.impl.combat.auraProcess.auraUtil;

import elexis.client.utils.client.IMinecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class RayTraceUtil implements IMinecraft {
    private RayTraceUtil() {
    }

    public static HitResult calculateRayTrace(double distance, float yaw, float pitch, Entity entity, boolean ignoreBlocks) {
        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
        Vec3d startVector = mc.player.getCameraPosVec(tickDelta);
        Vec3d directionVector = getVectorForRotation(pitch, yaw);
        Vec3d endVector = startVector.add(directionVector.multiply(distance));
        HitResult blockResult = traceBlock(startVector, endVector, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE);
        double entityDistance = blockResult.getPos().squaredDistanceTo(startVector);
        Box entityBoundingBox = entity.getBoundingBox().stretch(directionVector.multiply(distance)).expand(1.0);
        EntityHitResult entityHitResult = ProjectileUtil.raycast(entity, startVector, endVector, entityBoundingBox, x -> !x.isSpectator() && x.isAlive() && x.canHit(), distance * distance);
        return entityHitResult == null || (!ignoreBlocks && !(entityHitResult.getPos().squaredDistanceTo(startVector) < entityDistance)) ? blockResult : entityHitResult;
    }

    public static boolean rayTraceEntity(float yaw, float pitch, double distance, Entity entity) {
        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
        Vec3d eyeVec = mc.player.getCameraPosVec(tickDelta);
        Vec3d lookVec = getVectorForRotation(pitch, yaw);
        Vec3d endVec = eyeVec.add(lookVec.multiply(distance));
        Box entityBox = entity.getBoundingBox();
        return entityBox.contains(eyeVec) || entityBox.raycast(eyeVec, endVec).isPresent();
    }

    public static Vec3d getVectorForRotation(float pitch, float yaw) {
        float yawRadians = -yaw * (float) (Math.PI / 180.0) - (float) Math.PI;
        float pitchRadians = -pitch * (float) (Math.PI / 180.0);
        float cosYaw = (float) Math.cos(yawRadians);
        float sinYaw = (float) Math.sin(yawRadians);
        float cosPitch = -(float) Math.cos(pitchRadians);
        float sinPitch = (float) Math.sin(pitchRadians);
        return new Vec3d(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
    }

    public static HitResult traceBlock(Vec3d startVec, Vec3d endVec, RaycastContext.ShapeType blockMode, RaycastContext.FluidHandling fluidMode) {
        return mc.world.raycast(new RaycastContext(startVec, endVec, blockMode, fluidMode, mc.player));
    }
}
