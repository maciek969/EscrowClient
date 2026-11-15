package elexis.modules.impl.combat.javelin.pearl;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public record PearlRotation(float yaw, float pitch) {
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public Vec3d toVector() {
        float p = pitch * (float) (Math.PI / 180);
        float y = -yaw * (float) (Math.PI / 180);
        return new Vec3d(MathHelper.sin(y) * MathHelper.cos(p), -MathHelper.sin(p), MathHelper.cos(y) * MathHelper.cos(p));
    }
}
