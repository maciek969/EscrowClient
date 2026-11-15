package elexis.modules.impl.combat;

import elexis.Elexis;
import elexis.client.utils.client.IMinecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Shared target rules for camera assistance and crosshair attacks. */
public final class CombatTargeting implements IMinecraft {
    private CombatTargeting() {}

    public static boolean valid(LivingEntity target, double range, boolean playersOnly, boolean ignoreTeam) {
        return valid(target,range,playersOnly,ignoreTeam,false);
    }

    public static boolean valid(LivingEntity target,double range,boolean playersOnly,boolean ignoreTeam,boolean targetInvisibles) {
        if (mc.player == null || mc.world == null || target == null || target == mc.player
                || target.getEntityWorld() != mc.world || !target.isAlive() || target.isRemoved()
                || target instanceof ArmorStandEntity || target.isInvisible() && !targetInvisibles
                || playersOnly && !(target instanceof PlayerEntity)) return false;
        if (target instanceof PlayerEntity player) {
            if (player.isSpectator() || player.isCreative()
                    || Elexis.getInstance().getFriendStorage().isFriend(player.getGameProfile().name())) return false;
        }
        if (ignoreTeam && mc.player.isTeammate(target)) return false;
        AntiBot antiBot = Elexis.getFunctionManager().getAntiBot();
        if (antiBot != null && antiBot.isState() && AntiBot.checkBot(target)) return false;
        return distanceSquared(target) <= range * range && mc.player.canSee(target);
    }

    public static double distanceSquared(LivingEntity target) {
        Vec3d eye = mc.player.getEyePos();
        Box box = target.getBoundingBox();
        return eye.squaredDistanceTo(MathHelper.clamp(eye.x, box.minX, box.maxX),
                MathHelper.clamp(eye.y, box.minY, box.maxY), MathHelper.clamp(eye.z, box.minZ, box.maxZ));
    }

    public static boolean holdingWeapon() {
        return mc.player != null && (mc.player.getMainHandStack().isIn(ItemTags.SWORDS)
                || mc.player.getMainHandStack().isIn(ItemTags.AXES));
    }

    public static boolean anotherCombatModuleActive() {
        var manager = Elexis.getFunctionManager();
        return manager.getHitAura() != null && manager.getHitAura().isState()
                || manager.getBowAim() != null && manager.getBowAim().isState() && mc.player.isUsingItem();
    }

    public static Vec3d aimPoint(LivingEntity target) {
        Box box = target.getBoundingBox();
        return new Vec3d((box.minX + box.maxX) * 0.5,
                MathHelper.clamp(mc.player.getEyeY(), box.minY + target.getHeight() * 0.35,
                        box.minY + target.getHeight() * 0.8), (box.minZ + box.maxZ) * 0.5);
    }

    public static Vec3d aimPoint(LivingEntity target, float partialTicks) {
        Box box = target.getBoundingBox();
        Vec3d offset = target.getLerpedPos(partialTicks).subtract(target.getEntityPos());
        return new Vec3d((box.minX + box.maxX) * 0.5 + offset.x,
                MathHelper.clamp(mc.player.getCameraPosVec(partialTicks).y,
                        box.minY + offset.y + target.getHeight() * 0.35,
                        box.minY + offset.y + target.getHeight() * 0.8),
                (box.minZ + box.maxZ) * 0.5 + offset.z);
    }

    public static float[] angles(Vec3d point) {
        return angles(point, mc.player.getEyePos());
    }

    public static float[] angles(Vec3d point, Vec3d eye) {
        Vec3d delta = point.subtract(eye);
        return new float[]{(float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90f,
                (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)))};
    }
}
