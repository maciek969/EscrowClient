package elexis.modules.impl.movement;

import elexis.Elexis;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.impl.combat.HitAura;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@FunctionAdd(name = "ElytraMotion", alias = "Elytra Motion", category = Category.Movement,
        description = "Stops flight near a grounded target, like Javelin")
public final class ElytraMotion extends Function {
    public boolean active;

    public Vec3d modifyGlidingVelocity(Vec3d velocity) {
        active = false;
        if (!isState() || nullCheck() || !mc.player.isGliding()) return velocity;
        var aura = Elexis.getFunctionManager().getHitAura();
        var target = aura.isState() ? HitAura.target : null;
        if (target == null || !target.isAlive() || target.isGliding()) return velocity;
        var box = target.getBoundingBox();
        Vec3d eye = mc.player.getEyePos();
        Vec3d nearest = new Vec3d(MathHelper.clamp(eye.x, box.minX, box.maxX),
                MathHelper.clamp(eye.y, box.minY, box.maxY), MathHelper.clamp(eye.z, box.minZ, box.maxZ));
        active = eye.squaredDistanceTo(nearest) < 2.5 * 2.5;
        return active ? Vec3d.ZERO : velocity;
    }

    public boolean isActive() { return active; }

    @Override
    public void onDisable() {
        active = false;
        super.onDisable();
    }
}
