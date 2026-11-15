package elexis.modules.impl.combat;

import elexis.Elexis;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import elexis.modules.impl.combat.auraProcess.auraUtil.AuraUtil;
import elexis.modules.impl.combat.auraProcess.rotationProcess.impl.Rotation;
import elexis.modules.impl.combat.auraProcess.rotationProcess.impl.RotationProcess;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;

@FunctionAdd(name = "BowAim", alias = "Bow Aim", category = Category.Combat)
public class BowAim extends Function {

    public static final SliderSetting range = new SliderSetting("Range", 20f, 5f, 60f, 1f, false);
    public static final SliderSetting aimSpeed = new SliderSetting("Aim Speed", 30f, 5f, 90f, 1f, false);
    public static final SliderSetting returnSpeed = new SliderSetting("Return Speed", 15f, 5f, 90f, 1f, false);
    public static final BooleanSetting predictMovement = new BooleanSetting("Predict Movement", true);
    public static final BooleanSetting onlyPlayers = new BooleanSetting("Only Players", true);
    public static final BooleanSetting requireFullCharge = new BooleanSetting("Require Full Charge", false);

    public BowAim() {
        addSettings(range, aimSpeed, returnSpeed, predictMovement, onlyPlayers, requireFullCharge);
    }

    @EventHandler
    public void onUpdate(EventUpdate.Fast event) {
        if (mc.player == null || mc.world == null) return;

        // Check if holding bow or crossbow
        if (!isHoldingBow()) return;

        // Check if actively charging/using the bow
        if (!mc.player.isUsingItem()) return;

        // Check bow charge level if required
        if (requireFullCharge.get()) {
            int useTime = mc.player.getItemUseTime();
            if (useTime < 20) return; // need ~1 second for full charge
        }

        LivingEntity target = findTarget();
        if (target == null) return;

        // Calculate aim point (with arrow drop prediction)
        Vec3d aimPoint = calculateAimPoint(target);

        // Calculate rotation to aim point — from eye position
        Vec3d eyePos = mc.player.getEyePos();
        Vector2f rot = AuraUtil.calculate(eyePos, aimPoint);
        Rotation targetRotation = new Rotation(rot.x, rot.y);

        float speed = aimSpeed.get();
        float rSpeed = returnSpeed.get();

        // Use RotationProcess with clientRotation=true — this moves the ACTUAL screen/camera
        RotationProcess.update(targetRotation, speed, speed, rSpeed, rSpeed, 3, 5, true);
    }

    private boolean isHoldingBow() {
        if (mc.player == null) return false;
        var mainHand = mc.player.getMainHandStack();
        var offHand  = mc.player.getOffHandStack();
        return mainHand.getItem() instanceof BowItem
                || mainHand.getItem() instanceof CrossbowItem
                || offHand.getItem() instanceof BowItem
                || offHand.getItem() instanceof CrossbowItem;
    }

    private LivingEntity findTarget() {
        LivingEntity best   = null;
        double      bestDist = Double.MAX_VALUE;
        float       maxRange = range.get();

        for (var entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive()) continue;
            if (living.isInvisible()) continue;

            // Only players filter
            if (onlyPlayers.get() && !(living instanceof PlayerEntity)) continue;

            // Friends / team check
            if (living instanceof PlayerEntity player) {
                if (Elexis.getInstance().getFriendStorage().isFriend(player.getGameProfile().name())) continue;
            }

            double dist = mc.player.distanceTo(living);
            if (dist > maxRange) continue;

            if (dist < bestDist) {
                bestDist = dist;
                best     = living;
            }
        }
        return best;
    }

    /**
     * Calculates where to aim to hit the target, accounting for:
     * 1. Arrow drop (gravity)
     * 2. Target movement prediction
     */
    private Vec3d calculateAimPoint(LivingEntity target) {
        Vec3d eyePos = mc.player.getEyePos();

        // Target center chest
        Vec3d targetPos = new Vec3d(
                target.getX(),
                target.getY() + target.getHeight() * 0.6,
                target.getZ()
        );

        if (predictMovement.get()) {
            double dist        = eyePos.distanceTo(targetPos);
            double bowSpd      = getBowSpeed();
            double flightTicks = bowSpd > 0 ? dist / bowSpd : 0;

            targetPos = targetPos.add(
                    target.getVelocity().x * flightTicks,
                    0, // skip Y prediction — arrow drop handles vertical
                    target.getVelocity().z * flightTicks
            );
        }

        // Arrow drop compensation:
        // arrow gravity = 0.05 blocks/tick^2
        // drop = 0.5 * g * t^2
        Vec3d diff       = targetPos.subtract(eyePos);
        double horizDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        double bowSpd    = getBowSpeed();
        if (bowSpd > 0.1) {
            double t    = horizDist / bowSpd;
            double drop = 0.5 * 0.05 * t * t;
            targetPos   = targetPos.add(0, drop, 0);
        }

        return targetPos;
    }

    /**
     * Returns arrow speed in blocks/tick based on bow charge.
     * Full charge = ~3.0 blocks/tick, crossbow = ~3.15
     */
    private double getBowSpeed() {
        if (mc.player == null) return 3.0;
        var item = mc.player.getActiveItem().getItem();
        if (item instanceof CrossbowItem) return 3.15;

        // Bow speed scales with charge (max at 20 ticks)
        int useTime = mc.player.getItemUseTime();
        float charge = BowItem.getPullProgress(useTime);
        return 0.5 + charge * 2.5; // 0.5 to 3.0 blocks/tick
    }

    @Override
    public void onDisable() {
        RotationProcess.stop();
    }
}
