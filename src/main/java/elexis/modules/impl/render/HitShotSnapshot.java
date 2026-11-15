package elexis.modules.impl.render;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public final class HitShotSnapshot {
    public static final long LIFETIME_MS = 1500L;

    private final UUID ownerId;
    private final long createdAt;
    private final Vec3d position;
    private final Box box;
    private final float bodyYaw;
    private final float headYaw;
    private final float pitch;

    public HitShotSnapshot(UUID ownerId, long createdAt, Vec3d position, Box box, float bodyYaw, float headYaw, float pitch) {
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.position = position;
        this.box = box;
        this.bodyYaw = bodyYaw;
        this.headYaw = headYaw;
        this.pitch = pitch;
    }

    public static HitShotSnapshot capture(AbstractClientPlayerEntity player, Vec3d position, long createdAt) {
        Vec3d livePosition = new Vec3d(player.getX(), player.getY(), player.getZ());
        Box box = player.getBoundingBox().offset(position.subtract(livePosition));
        return new HitShotSnapshot(
                player.getUuid(),
                createdAt,
                position,
                box,
                player.bodyYaw,
                player.headYaw,
                player.getPitch()
        );
    }

    public boolean isExpired(long now) {
        return now - createdAt > LIFETIME_MS;
    }

    public float alpha(long now) {
        long age = now - createdAt;
        float fadeIn = Math.min(1.0f, age / 150.0f);
        float fadeOut = 1.0f - MathHelper.clamp((age - (LIFETIME_MS - 350L)) / 350.0f, 0.0f, 1.0f);
        return MathHelper.clamp(Math.min(fadeIn, fadeOut), 0.0f, 1.0f);
    }

    public double riseOffset(long now) {
        float progress = MathHelper.clamp((now - createdAt) / (float) LIFETIME_MS, 0.0f, 1.0f);
        float eased = 1.0f - (1.0f - progress) * (1.0f - progress);
        return eased * 0.6;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Vec3d getPosition() {
        return position;
    }

    public Box getBox() {
        return box;
    }

    public float getBodyYaw() {
        return bodyYaw;
    }

    public float getHeadYaw() {
        return headYaw;
    }

    public float getPitch() {
        return pitch;
    }
}
