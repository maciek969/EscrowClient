package elexis.modules.impl.combat.auraProcess.rotationProcess.impl;

import elexis.client.utils.client.IMinecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;

public class Rotation implements IMinecraft {
    public float yaw;
    public float pitch;

    public Rotation(Entity entity) {
        this(entity.getYaw(), entity.getPitch());
    }

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getDelta(Rotation target) {
        float yawDelta = MathHelper.wrapDegrees(target.yaw - this.yaw);
        float pitchDelta = target.pitch - this.pitch;
        return (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
    }

    public static Vector2f camera() {
        return new Vector2f(cameraYaw(), cameraPitch());
    }

    public static float cameraYaw() {
        return MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() + (mc.gameRenderer.getCamera().isThirdPerson() ? 180 : 0));
    }

    public static float cameraPitch() {
        return (mc.gameRenderer.getCamera().isThirdPerson() ? -1 : 1) * mc.gameRenderer.getCamera().getPitch();
    }
}
