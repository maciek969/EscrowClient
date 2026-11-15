package elexis.modules.impl.combat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.PlayerInput;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import elexis.Elexis;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.entity.EventMotion;
import elexis.client.events.impl.render.EventRender;
import elexis.mixins.accessors.ClientPlayerEntityAccessor;
import elexis.modules.api.Category;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.Function;
import elexis.modules.api.settings.api.Setting;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.MultiBooleanSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import elexis.modules.impl.combat.auraProcess.Attack;
import elexis.modules.impl.combat.auraProcess.Rotate;
import elexis.modules.impl.combat.auraProcess.auraUtil.AuraUtil;
import elexis.modules.impl.combat.auraProcess.auraUtil.RayTraceUtil;
import elexis.modules.impl.combat.auraProcess.rotationProcess.impl.Rotation;
import elexis.modules.impl.combat.auraProcess.rotationProcess.impl.RotationProcess;
import elexis.modules.impl.combat.auraProcess.rotationProcess.impl.FreeLookUtil;
import elexis.client.utils.player.MovementManager;

@FunctionAdd(
        name = "HitAura",
        alias = "Hit Aura",
        description = "Automatically attacks the target",
        category = Category.Combat,
        key = 82
)
@Environment(EnvType.CLIENT)
public class HitAura extends Function {
    private static final String T_PLAYERS = "Players";
    private static final String T_NAKED = "Naked";
    private static final String T_MOBS = "Mobs";

    private static final String M_THROUGH_WALLS = "Hit through walls";
    private static final String M_ONLY_WEAPON = "Hit only with weapon";
    private static final String M_NO_EATING = "Do not hit while eating";
    private static final String M_NO_CONTAINERS = "Do not hit in container";
    private static final String M_BREAK_SHIELD = "Break shield";
    private static final String M_PRESS_SHIELD = "Press shield on hit";
    private static final String M_HIT_AND_EAT = "Hit and eat";

    private static final String E_LEGIT_SPRINT = "Legit sprint";
    private static final String E_SMART_CRITS = "Smart crits";

    private static final String SLOW_RADIUS = "By radius";
    private static final String SLOW_PRE_HIT = "Before hit";
    private static final String RES_LAG2 = "Lag2";
    private static final String RES_ANTI_AIM = "Anti-Aim";
    private static final String RES_BACKTRACK = "Backtrack";
    private static final String AIM_RESOLVER = "Resolve";
    private static final String AIM_FIREWORK = "Firework";
    private static final String AIM_MIDDLE = "Middle";
    private static final String AIM_NORMAL = "Normal";

    public static SliderSetting attackRange = new SliderSetting("Attack range", 3.0F, 3.0F, 6.0F, 0.1F, false);
    public static SliderSetting preRange = new SliderSetting("Detection range", 1.0F, 0.0F, 5.0F, 0.1F, false);
    public static BooleanSetting raycastCheck = new BooleanSetting("Raycast check", true);
    public static BooleanSetting smartAim = new BooleanSetting("Smart aim", true);
    public static ModeSetting rotationType = new ModeSetting("Rotation mode", "SpookyTime",
            "SpookyTime", "FunTime", "ReallyWorld", "CakeWorld") {
        @Override
        public void set(String value) {
            for (String mode : modes) {
                if (mode.equalsIgnoreCase(value)) {
                    super.set(mode);
                    return;
                }
            }
            super.set("SpookyTime");
        }
    };
    public static ModeSetting antiCheat = new ModeSetting("AntiCheat", "None", "None", "Grim", "SlotAC").hidden(() -> true);
    public static MultiBooleanSetting targets = new MultiBooleanSetting("Targets", new BooleanSetting(T_PLAYERS, true), new BooleanSetting(T_NAKED, true), new BooleanSetting(T_MOBS, true));
    public static ModeSetting attackDelay = new ModeSetting("Attack timing", "Fast", "Fast", "Dynamic");
    public static MultiBooleanSetting misc = new MultiBooleanSetting("Other before hit", new BooleanSetting(M_THROUGH_WALLS, false), new BooleanSetting(M_ONLY_WEAPON, false), new BooleanSetting(M_NO_EATING, true), new BooleanSetting(M_NO_CONTAINERS, false), new BooleanSetting(M_BREAK_SHIELD, false), new BooleanSetting(M_PRESS_SHIELD, false), new BooleanSetting(M_HIT_AND_EAT, false));
    public static MultiBooleanSetting extraSettings = new MultiBooleanSetting("Extra settings", new BooleanSetting(E_LEGIT_SPRINT, false), new BooleanSetting(E_SMART_CRITS, false));
    public static ModeSetting motion = new ModeSetting("Motion mode", "Default", "Default", "Free", "Target");

    public static BooleanSetting elytraSlowdown = new BooleanSetting("Elytra slowdown", true);
    public static ModeSetting elytraSlowdownMode = new ModeSetting("Slowdown mode", SLOW_RADIUS, SLOW_RADIUS, SLOW_PRE_HIT).hidden(() -> !elytraSlowdown.get());
    public static SliderSetting elytraSlowdownRadius = new SliderSetting("Slowdown radius", 3.0F, 1.0F, 6.0F, 0.1F, false).hidden(() -> !elytraSlowdown.get() || !elytraSlowdownMode.is(SLOW_RADIUS));
    public static SliderSetting elytraMinSpeed = new SliderSetting("Min speed", 0.3F, 0.1F, 0.9F, 0.05F, false).hidden(() -> !elytraSlowdown.get() || !elytraSlowdownMode.is(SLOW_RADIUS));
    public static SliderSetting elytraPreHitTicks = new SliderSetting("Ticks before hit", 3.0F, 1.0F, 10.0F, 1.0F, false).hidden(() -> !elytraSlowdown.get() || !elytraSlowdownMode.is(SLOW_PRE_HIT));
    public static BooleanSetting elytraHitAfterOvertake = new BooleanSetting("Hit after flyby", true);
    public static BooleanSetting elytraTurnaround = new BooleanSetting("Elytra turnaround", true);
    public static BooleanSetting elytraUseResolver = new BooleanSetting("Resolver (Elytra)", true);
    public static ModeSetting elytraResolverMode = new ModeSetting("Resolver mode", RES_LAG2, RES_LAG2, RES_ANTI_AIM, RES_BACKTRACK).hidden(() -> !elytraUseResolver.get());
    public static SliderSetting elytraResolverBacktrackTicks = new SliderSetting("Backtrack ticks", 15.0F, 2.0F, 25.0F, 1.0F, false).hidden(() -> !elytraUseResolver.get() || !elytraResolverMode.is(RES_BACKTRACK));
    public static ModeSetting elytraAimMode = new ModeSetting("Aim point mode", AIM_FIREWORK, AIM_RESOLVER, AIM_FIREWORK, AIM_MIDDLE, AIM_NORMAL);
    public static BooleanSetting elytraAutoFirework = new BooleanSetting("Auto firework", false);
    public static BooleanSetting elytraSmartFirework = new BooleanSetting("Smart usage", true).hidden(() -> !elytraAutoFirework.get());
    public static SliderSetting elytraFireworkDelay = new SliderSetting("Firework delay", 400.0F, 200.0F, 800.0F, 50.0F, false).hidden(() -> !elytraAutoFirework.get() || elytraSmartFirework.get());
    public static BooleanSetting elytraPredict = new BooleanSetting("Predict", true);
    public static SliderSetting elytraPredictValue = new SliderSetting("Predict value", 3.0F, 1.0F, 5.0F, 0.1F, false);
    public static BooleanSetting showPredictPoint = new BooleanSetting("Show predict point", true).hidden(() -> !elytraPredict.get());

    public static BooleanSetting elytraSmartOvertake = new BooleanSetting("Smart flyby", true);
    public static SliderSetting elytraOvertakeDistance = new SliderSetting("Flyby distance", 2.4F, 0.5F, 6.0F, 0.1F, false).hidden(() -> !elytraSmartOvertake.get());
    public static SliderSetting elytraOvertakeBoost = new SliderSetting("Flyby power", 0.55F, 0.1F, 1.5F, 0.05F, false).hidden(() -> !elytraSmartOvertake.get());
    // New Elytra features from DDATTACK
    public static BooleanSetting elytraCustomRotate = new BooleanSetting("Custom Rotate for elytra", false);
    public static SliderSetting elytraMinY = new SliderSetting("Y smoothness for elytra", 1.0F, 1.0F, 360.0F, 1.0F, false).hidden(() -> !elytraCustomRotate.get());
    public static SliderSetting elytraMinXZ = new SliderSetting("XZ smoothness for elytra", 1.0F, 1.0F, 360.0F, 1.0F, false).hidden(() -> !elytraCustomRotate.get());
    public static ModeSetting elytraRotationMode = new ModeSetting("Elytra rotation mode", "Snaps", "Snaps", "Packet").hidden(() -> !elytraCustomRotate.get());
    public static ModeSetting afterElytraOvertake = new ModeSetting("After flyby", "None", "Freeze", "Swirl", "Motion reset", "None");
    public static BooleanSetting elytraCrits = new BooleanSetting("Crit on elytra", false);
    public static SliderSetting elytraCritPitch = new SliderSetting("Pitch for crits on elytra", 90.0F, 0.0F, 90.0F, 1.0F, false).hidden(() -> !elytraCrits.get());
    public static BooleanSetting elytraHitAfterCloseRange = new BooleanSetting("Improved hits", false);
    public static BooleanSetting elytraPredictDefensive = new BooleanSetting("Anti Defensive", false).hidden(() -> !elytraPredict.get());
    public static BooleanSetting elytraDoubleRot = new BooleanSetting("Double rotation", false).hidden(() -> !elytraPredict.get());
    public static BooleanSetting elytraSwapElytra = new BooleanSetting("Equip elytra", false);
    public static BooleanSetting elytraHitOnlyOnDamage = new BooleanSetting("Hit only on damage", false);
    public static BooleanSetting elytraPearlFollow = new BooleanSetting("Follow pearls", false);
    public static BooleanSetting elytraDrawBacktrackBox = new BooleanSetting("Draw backtrack box", false).hidden(() -> !elytraPredict.get());
    public static SliderSetting elytraBacktrackTime = new SliderSetting("Backtrack time", 1.0F, 0.1F, 5.0F, 0.1F, false).hidden(() -> !elytraPredict.get() || !elytraDrawBacktrackBox.get());

    public static LivingEntity target;
    public static boolean canSwap = false;
    public static boolean silentActive;
    public static float spoofYaw;
    public static float spoofPitch;

    public static boolean isSlowdownActive = false;
    public boolean isTurnaroundActive = false;
    public float preddict = 2.7f;
    public int ticksToAttack = 0;
    private int elytraPostHitTicks = 0;
    private int elytraVelocityAssistCooldown = 0;
    private final Deque<Vec3d> resolverLag2Points = new ArrayDeque<>();
    private int resolverTrackedTargetId = Integer.MIN_VALUE;
    private Vec3d resolverLag2Point = Vec3d.ZERO;
    private Vec3d resolverLastLaggedPoint = Vec3d.ZERO;
    private long resolverLag2TimeoutTime = 0L;
    private final Deque<Vec3d> resolverPositionHistory = new ArrayDeque<>();
    private int resolverHistoryTargetId = Integer.MIN_VALUE;
    private boolean resolverAntiAimDirection = false;
    private boolean resolverAntiAimSwitched = false;
    private Vec3d smoothedPredictPoint = null;
    private LivingEntity smoothedPredictTarget = null;
    private boolean autoEatUseKeyHeld = false;
    private long lastFireworkUseTime = 0L;
    private long lastElytraAttackTime = 0L;
    private long targetLeaveStartTime = 0L;
    private int targetScanCooldown = 0;
    private final RotationProcess rotationProcess = new RotationProcess();
    private final FreeLookUtil freeLookUtil = new FreeLookUtil();

    private static Vec3d entityPos(Entity entity) {
        return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
    }

    private static boolean isObfuscatedRuntime() {
        Package pkg = HitAura.class.getPackage();
        if (pkg == null) return false;
        return pkg.getName().contains(".obf");
    }

    private boolean isElytraTargetEnabled() {
        return false;
    }

    public static float getEffectiveElytraPredictValue() {
        return elytraPredict.get() ? elytraPredictValue.get() : 0.0F;
    }

    public static boolean shouldUseElytraPredict() {
        return getEffectiveElytraPredictValue() > 0.01F;
    }

    public HitAura() {
        this.addSettings(new Setting[]{
                attackRange, preRange, raycastCheck, smartAim, rotationType, targets, attackDelay, misc, extraSettings,
                elytraSlowdown, elytraSlowdownMode, elytraSlowdownRadius, elytraMinSpeed, elytraPreHitTicks,
                elytraHitAfterOvertake, elytraTurnaround, elytraUseResolver, elytraResolverMode, elytraResolverBacktrackTicks,
                elytraAimMode, elytraAutoFirework, elytraSmartFirework, elytraFireworkDelay, elytraPredict, elytraPredictValue,
                showPredictPoint, elytraSmartOvertake, elytraOvertakeDistance, elytraOvertakeBoost,
                elytraCustomRotate, elytraMinY, elytraMinXZ, elytraRotationMode, afterElytraOvertake, elytraCrits, elytraCritPitch
        });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Elexis.getEventBus().subscribe(this.freeLookUtil);
        Elexis.getEventBus().subscribe(this.rotationProcess);
        this.reset();
    }

    @EventHandler
    public void onEvent(EventUpdate.Fast e) {
        if (target != null && mc.player != null && mc.world != null) {
            this.updateRotation();
        }
    }

    @EventHandler
    public void onEvent(EventMotion e) {
        if (mc.player == null) return;
        if (!silentActive) {
            spoofYaw = mc.player.getYaw();
            spoofPitch = mc.player.getPitch();
            return;
        }
        e.setYaw(spoofYaw);
        e.setPitch(spoofPitch);
    }

    @EventHandler
    public void onEvent(EventRender.World e) {
        if (mc.player == null || mc.world == null || target == null || !target.isGliding()) {
            this.resetPredictPointSmoothing();
            return;
        }

        Vec3d predict = this.smoothPredictPoint(target, this.resolveElytraTargetVector(target));
        if (showPredictPoint.get()) {
            this.renderPredictPoint(e, predict);
        }
    }

    @EventHandler
    public void onEvent(EventUpdate.Slow e) {
        if (mc.player == null || mc.world == null) {
            this.reset();
            return;
        }
        if (!mc.player.isAlive()) {
            this.toggle();
            return;
        }

        if (ticksToAttack > 0) ticksToAttack--;
        if (elytraPostHitTicks > 0) elytraPostHitTicks--;
        if (elytraVelocityAssistCooldown > 0) elytraVelocityAssistCooldown--;

        // Update the target every tick — to switch to a closer one
        // (previously updated only when target was null or invalid — target "stuck")
        this.updateTarget();

        if (target != null && mc.player != null && mc.world != null) {
            this.updateResolverHistory();
            if (rotationType.is("SpookyTime") && Attack.resetSprintTick(target, getRanges())) {
                this.stopSprintForAuraHit();
            }
            if (misc.get(M_HIT_AND_EAT)) {
                this.handleAutoEat();
            } else {
                this.releaseAutoEatUseKey();
            }

            // ElytraTarget only if elytra settings are enabled
            if (target.isGliding() && this.isElytraTargetEnabled()) {
                this.handleElytraTargetTick();
            } else if (mc.player.isGliding() && this.isElytraTargetEnabled()) {
                // You are flying on elytra, the target is on the ground — use elytra attack logic
                // (normal attackEntity cannot keep up during fast flight)
                this.handleElytraVsGroundTick();
            } else {
                isSlowdownActive = false;
                isTurnaroundActive = false;
                if (!this.checkToAttack()) {
                    this.attackEntity();
                }
            }
        } else {
            this.reset();
        }
    }

    private void handleElytraVsGroundTick() {

        if (target == null || mc.player == null) return;

        this.updateRotation();

        if (!Attack.msCooldownReached()) return;
        if (!this.isAttackCooldownReady()) return;
        if (ticksToAttack > 0) return;
        if (this.checkToAttack()) return;

        // Extended radius for elytra flight — use attackRange + 1.5
        boolean usingFirework = mc.player.isUsingItem()
                && mc.player.getActiveItem().getItem() instanceof FireworkRocketItem;
        float elytraRange = attackRange.get() + (usingFirework ? 2.5F : 1.5F);

        double dist = mc.player.getEyePos().distanceTo(
                this.resolveElytraAimPoint(target)
        );
        if (dist > elytraRange) return;

        if (raycastCheck.get() && !RayTraceUtil.rayTraceEntity(
                this.currentAuraYaw(), this.currentAuraPitch(), elytraRange, target)) {
            return;
        }

        doAttack();
        ticksToAttack = usingFirework ? 5 : 8;
    }

    private void chaseElytraTarget(Vec3d predict) {
        if (target == null || mc.player == null || mc.world == null) return;
        if (!mc.player.isGliding()) return;

        boolean strictServer = this.isStrictElytraServer();
        Vec3d playerEye = mc.player.getEyePos();
        Vec3d flightPoint = this.resolveSmartOvertakePoint(predict);
        Vec3d toFlightPoint = flightPoint.subtract(playerEye);
        double distanceToPredict = toFlightPoint.length();

        // Do not chase if already in the attack zone
        if (distanceToPredict <= preddict) return;

        Vec3d currentVel = mc.player.getVelocity();
        Vec3d desiredDir = toFlightPoint.normalize();
        if (strictServer && elytraVelocityAssistCooldown > 0) {
            return;
        }

        // Chase speed: the farther the target — the faster we accelerate, but smoothly
        double targetSpeed = Math.hypot(target.getVelocity().x, target.getVelocity().z);
        double ourSpeed = Math.hypot(currentVel.x, currentVel.z);
        double pressure = this.computeElytraChasePressure(predict);
        boolean targetLeaving = this.targetIsLeaving(target);
        double extraBoost = strictServer
                ? Math.min(elytraSmartOvertake.get() ? elytraOvertakeBoost.get() : 0.2F, 0.32F)
                : (elytraSmartOvertake.get() ? elytraOvertakeBoost.get() : 0.2F);
        double desiredSpeed = MathHelper.clamp(
                Math.max(0.78 + distanceToPredict * (strictServer ? 0.055 : 0.10), targetSpeed + (strictServer ? 0.09 : 0.22) + pressure * extraBoost),
                0.78,
                strictServer ? 2.34 : 3.55
        );
        if (!strictServer) {
            desiredSpeed = Math.min(targetLeaving ? 4.0 : 3.75, desiredSpeed + 0.22 + pressure * 0.35 + Math.max(0.0, targetSpeed - ourSpeed) * 0.45);
        }
        if (strictServer && distanceToPredict < preddict + 2.0 && targetSpeed <= Math.hypot(currentVel.x, currentVel.z) + 0.1) {
            return;
        }
        if (isSlowdownActive) {
            desiredSpeed *= strictServer ? 0.88 : 0.93;
        }

        // Interpolate current speed toward the desired direction
        Vec3d desiredVel = desiredDir.multiply(desiredSpeed);
        desiredVel = new Vec3d(
                desiredVel.x,
                MathHelper.clamp(desiredVel.y, strictServer ? -0.16 : -0.42, strictServer ? (pressure > 0.45 ? 0.24 : 0.18) : (pressure > 0.45 ? 0.66 : 0.46)),
                desiredVel.z
        );
        double blend = strictServer ? 0.09 + pressure * 0.05 : 0.30 + pressure * 0.22 + (targetLeaving ? 0.12 : 0.0);
        Vec3d newVel = currentVel.add(desiredVel.subtract(currentVel).multiply(blend));
        if (strictServer) {
            newVel = this.limitVelocityDelta(currentVel, newVel, 0.06, 0.025);
        }
        double minHorizontalSpeed = strictServer ? 0.34 + pressure * 0.16 : 0.65 + pressure * 0.60 + (targetLeaving ? 0.25 : 0.0);
        double horizontalSpeed = Math.hypot(newVel.x, newVel.z);
        if (horizontalSpeed < minHorizontalSpeed) {
            Vec3d horizontalDir = new Vec3d(desiredDir.x, 0.0, desiredDir.z);
            if (horizontalDir.lengthSquared() > 1.0E-4) {
                horizontalDir = horizontalDir.normalize().multiply(minHorizontalSpeed);
                newVel = new Vec3d(horizontalDir.x, newVel.y, horizontalDir.z);
            }
        }

        // Limit the maximum speed
        double speed = newVel.length();
        double maxSpeed = strictServer ? 2.34 : (targetLeaving ? 4.0 : 3.75);
        if (speed > maxSpeed) {
            newVel = newVel.multiply(maxSpeed / speed);
        }

        if (strictServer && newVel.subtract(currentVel).lengthSquared() < 6.0E-4) {
            return;
        }

        mc.player.setVelocity(newVel);
        if (strictServer) {
            elytraVelocityAssistCooldown = 2;
        }
    }

    // Remember the target previous speed for second-order prediction
    private Vec3d resolveSmartOvertakePoint(Vec3d predict) {
        if (!shouldUseElytraPredict()) {
            return predict;
        }
        if (this.isPostHitOvertakeActive()) {
            Vec3d axis = this.getElytraTargetForwardAxis(predict);
            double postHitLead = this.isStrictElytraServer()
                    ? Math.max(1.15, elytraOvertakeDistance.get() * 0.55 + 0.45)
                    : Math.max(1.6, elytraOvertakeDistance.get() * 0.9 + 1.0);
            return predict.add(axis.multiply(postHitLead)).add(0.0, 0.08, 0.0);
        }
        if (!elytraSmartOvertake.get() || target == null || mc.player == null) {
            return predict;
        }

        boolean strictServer = this.isStrictElytraServer();
        Vec3d playerEye = mc.player.getEyePos();
        Vec3d axis = this.getElytraTargetForwardAxis(predict);
        Vec3d lateral = new Vec3d(-axis.z, 0.0, axis.x);
        Vec3d toPredict = predict.subtract(playerEye);
        double forwardGap = toPredict.dotProduct(axis);
        double verticalGap = predict.y - playerEye.y;
        double targetSpeed = Math.hypot(target.getVelocity().x, target.getVelocity().z);
        double ourSpeed = Math.hypot(mc.player.getVelocity().x, mc.player.getVelocity().z);
        double pressure = this.computeElytraChasePressure(predict);
        boolean targetLeaving = this.targetIsLeaving(target);
        double forwardLead = MathHelper.clamp(
                (strictServer ? elytraOvertakeDistance.get() * 0.7 : elytraOvertakeDistance.get())
                        + Math.max(0.0, forwardGap) * (strictServer ? 0.08 : 0.14)
                        + Math.max(0.0, targetSpeed - ourSpeed) * (strictServer ? 1.05 : 1.9),
                0.5,
                strictServer ? 4.2 : 7.0
        );
        if (!strictServer) {
            forwardLead = MathHelper.clamp(
                    forwardLead + 0.45 + Math.max(0.0, targetSpeed - ourSpeed) * 0.85 + pressure * 0.55 + (targetLeaving ? 1.1 : 0.0),
                    1.15,
                    targetLeaving ? 8.4 : 7.6
            );
        }
        Vec3d playerForward = mc.player.getRotationVec(1.0F);
        double sideSign = Math.signum(lateral.dotProduct(new Vec3d(playerForward.x, 0.0, playerForward.z)));
        if (sideSign == 0.0) {
            sideSign = (target.getId() & 1) == 0 ? 1.0 : -1.0;
        }

        double sideOffset = ((forwardGap > 0.0 ? (strictServer ? 0.18 : 0.35) : (strictServer ? 0.08 : 0.15)) + pressure * (strictServer ? 0.08 : 0.18)) * sideSign;
        if (!strictServer) {
            sideOffset += sideSign * (0.16 + pressure * 0.22 + (targetLeaving ? 0.18 : 0.0));
        }
        double verticalOffset = MathHelper.clamp(
                verticalGap * (strictServer ? 0.10 : 0.18) + (pressure > 0.45 ? (strictServer ? 0.04 : 0.12) : 0.0) + (!strictServer && targetLeaving ? 0.08 : 0.0),
                strictServer ? -0.16 : -0.35,
                strictServer ? 0.24 : 0.55
        );
        return predict
                .add(axis.multiply(forwardLead))
                .add(lateral.multiply(sideOffset))
                .add(0.0, verticalOffset, 0.0);
    }

    private Vec3d getElytraTargetForwardAxis(Vec3d predict) {
        Vec3d axis = target == null ? Vec3d.ZERO : target.getVelocity();
        axis = new Vec3d(axis.x, 0.0, axis.z);
        if (axis.lengthSquared() < 1.0E-4 && mc.player != null) {
            Vec3d fallback = predict.subtract(mc.player.getEyePos());
            axis = new Vec3d(fallback.x, 0.0, fallback.z);
        }
        if (axis.lengthSquared() < 1.0E-4) {
            axis = new Vec3d(0.0, 0.0, 1.0);
        }
        return axis.normalize();
    }

    private double computeElytraChasePressure(Vec3d predict) {
        if (target == null || mc.player == null) {
            return 0.0;
        }

        Vec3d axis = this.getElytraTargetForwardAxis(predict);
        Vec3d toPredict = predict.subtract(mc.player.getEyePos());
        double forwardGap = Math.max(0.0, toPredict.dotProduct(axis));
        double distance = toPredict.length();
        double targetSpeed = Math.hypot(target.getVelocity().x, target.getVelocity().z);
        double ourSpeed = Math.hypot(mc.player.getVelocity().x, mc.player.getVelocity().z);
        double speedGap = Math.max(0.0, targetSpeed - ourSpeed + 0.05);
        return MathHelper.clamp(forwardGap / 10.0 + distance / 20.0 + speedGap / 1.35, 0.0, 1.0);
    }

    private boolean isStrictElytraServer() {
        return antiCheat.is("Grim");
    }

    private boolean isAttackCooldownReady() {
        if (mc.player == null) {
            return false;
        }
        return mc.player.getAttackCooldownProgress(0.5F) >= 0.92F;
    }

    private Vec3d limitVelocityDelta(Vec3d currentVel, Vec3d nextVel, double maxHorizontalDelta, double maxVerticalDelta) {
        Vec3d delta = nextVel.subtract(currentVel);
        double horizontalDelta = Math.hypot(delta.x, delta.z);
        if (horizontalDelta > maxHorizontalDelta && horizontalDelta > 1.0E-4) {
            double scale = maxHorizontalDelta / horizontalDelta;
            delta = new Vec3d(delta.x * scale, delta.y, delta.z * scale);
        }

        double clampedY = MathHelper.clamp(delta.y, -maxVerticalDelta, maxVerticalDelta);
        return currentVel.add(delta.x, clampedY, delta.z);
    }

    private boolean isPostHitOvertakeActive() {
        return elytraPostHitTicks > 0 && target != null && mc.player != null && mc.player.isGliding() && target.isGliding();
    }

    private Vec3d lastTargetVelocity = Vec3d.ZERO;
    private boolean resolverDivePhase = false;
    private int resolverPhaseTicks = 0;
    private int resolverHitsOnTarget = 0;
    private int resolverTargetId = Integer.MIN_VALUE;

    private Vec3d predictElytraPos(LivingEntity entity, float ticks) {
        return this.predictElytraPos(entity, ticks, true);
    }

    private Vec3d predictElytraPos(LivingEntity entity, float ticks, boolean updateVelocityCache) {
        Vec3d previousVelocity = lastTargetVelocity;
        Vec3d predicted = this.predictElytraPosLegacy(entity, ticks);
        if (!updateVelocityCache) {
            lastTargetVelocity = previousVelocity;
        }
        return predicted;
    }

    private boolean isResolverActive() {
        return elytraUseResolver.get()
                && target != null
                && mc.player != null
                && mc.world != null
                && mc.player.isGliding()
                && target.isGliding();
    }

    private void resetResolverState() {
        resolverLag2Points.clear();
        resolverTrackedTargetId = Integer.MIN_VALUE;
        resolverLag2Point = Vec3d.ZERO;
        resolverLastLaggedPoint = Vec3d.ZERO;
        resolverLag2TimeoutTime = 0L;
        resolverPositionHistory.clear();
        resolverHistoryTargetId = Integer.MIN_VALUE;
        resolverAntiAimDirection = false;
        resolverAntiAimSwitched = false;
        resolverDivePhase = false;
        resolverPhaseTicks = 0;
        resolverHitsOnTarget = 0;
        resolverTargetId = Integer.MIN_VALUE;
    }

    private void updateResolverHistory() {
        if (!this.isResolverActive()) {
            this.resetResolverState();
            return;
        }

        if (resolverTrackedTargetId != target.getId()) {
            resolverTrackedTargetId = target.getId();
            resolverLag2Points.clear();
            resolverLag2Point = entityPos(target);
            resolverLastLaggedPoint = resolverLag2Point;
            resolverLag2TimeoutTime = 0L;
        }

        if (resolverHistoryTargetId != target.getId()) {
            resolverHistoryTargetId = target.getId();
            resolverPositionHistory.clear();
        }

        resolverPositionHistory.addLast(target.getBoundingBox().getCenter());
        while (resolverPositionHistory.size() > 30) {
            resolverPositionHistory.removeFirst();
        }

        if (!elytraResolverMode.is(RES_LAG2)) {
            return;
        }

        Vec3d serverPos = entityPos(target);
        long now = System.currentTimeMillis();
        if (resolverLastLaggedPoint.distanceTo(serverPos) > 2.0) {
            resolverLastLaggedPoint = serverPos;
            resolverLag2Points.addLast(serverPos);
            resolverLag2TimeoutTime = now;
        }

        while (resolverLag2Points.size() > 2) {
            resolverLag2Points.removeFirst();
        }

        if (resolverLag2Points.isEmpty()) {
            resolverLag2Point = serverPos;
            return;
        }

        Vec3d best = resolverLag2Points.getFirst();
        for (Vec3d sample : resolverLag2Points) {
            if (serverPos.distanceTo(sample) > serverPos.distanceTo(best) && sample.y > best.y) {
                best = sample;
            }
        }
        resolverLag2Point = best;
    }

    private Vec3d resolveElytraTargetVector(LivingEntity entity) {
        float predictValue = getEffectiveElytraPredictValue();
        Vec3d predictedCenter = this.predictElytraPos(entity, predictValue, false);
        Vec3d resolvedCenter = this.resolveResolverPoint(entity, predictedCenter);
        if (this.isResolverActive() && entity != null && entity.isGliding() && !entity.isOnGround() && !this.isResolverStoyak(entity)) {
            Vec3d serverPos = entityPos(entity);
            Vec3d unlerpedPos = this.getEntityUnlerpedPos(entity);
            double factor = this.shouldResolverMovePredict(entity) ? this.getResolverLatencyFactor(entity) : 0.4;
            resolvedCenter = resolvedCenter.add(serverPos.subtract(unlerpedPos).multiply(factor));
        }
        return this.clampToTargetBox(entity, resolvedCenter, mc.player == null ? resolvedCenter : mc.player.getEyePos());
    }

    private Vec3d resolveResolverPoint(LivingEntity entity, Vec3d fallback) {
        if (!this.isResolverActive() || entity == null) {
            return fallback;
        }

        if (elytraResolverMode.is(RES_LAG2)) {
            return resolverLag2Point == null ? fallback : resolverLag2Point;
        }

        if (elytraResolverMode.is(RES_BACKTRACK)) {
            if (resolverPositionHistory.isEmpty()) {
                return fallback;
            }

            int ticksBack = Math.max(1, Math.round(elytraResolverBacktrackTicks.get()));
            Vec3d[] history = resolverPositionHistory.toArray(Vec3d[]::new);
            int index = Math.max(0, history.length - 1 - ticksBack);
            return history[index];
        }

        if (mc.player == null || mc.player.getEyePos().squaredDistanceTo(fallback) > 16.0) {
            return fallback;
        }

        if (this.isAttackCooldownReady() && !resolverAntiAimSwitched) {
            resolverAntiAimDirection = !resolverAntiAimDirection;
            resolverAntiAimSwitched = true;
        } else if (!this.isAttackCooldownReady()) {
            resolverAntiAimSwitched = false;
        }

        float yaw = this.currentAuraYaw() + (resolverAntiAimDirection ? 12.0F : -12.0F);
        Vec3d vector = RayTraceUtil.getVectorForRotation(0.0F, yaw);
        return fallback.add(vector.x * 2.0, ThreadLocalRandom.current().nextDouble(-0.2, 0.2), vector.z * 2.0);
    }

    private boolean isLag2ResolverMode() {
        return elytraResolverMode.is(RES_LAG2);
    }

    private boolean isPortedAntiAimResolverMode() {
        return elytraResolverMode.is(RES_ANTI_AIM);
    }

    private boolean isPortedBacktrackResolverMode() {
        return elytraResolverMode.is(RES_BACKTRACK);
    }

    private void updateResolverState(Vec3d predict) {
        if (!this.isResolverActive()) {
            this.resetResolverState();
            return;
        }

        int currentTargetId = target.getId();
        if (resolverTargetId != currentTargetId) {
            resolverTargetId = currentTargetId;
            resolverDivePhase = false;
            resolverPhaseTicks = 0;
            resolverHitsOnTarget = 0;
        }

        resolverPhaseTicks++;

        double verticalDiff = mc.player.getY() - predict.y;
        double horizontalDistance = Math.hypot(mc.player.getX() - predict.x, mc.player.getZ() - predict.z);

        if (!resolverDivePhase) {
            boolean highEnough = verticalDiff > 1.05;
            boolean closeEnoughToDrop = horizontalDistance < 3.4;
            if ((highEnough && closeEnoughToDrop) || resolverPhaseTicks > 18) {
                resolverDivePhase = true;
                resolverPhaseTicks = 0;
            }
            return;
        }

        boolean passedBelowTarget = verticalDiff < -0.55;
        boolean flewPastTarget = horizontalDistance > 4.8 && resolverPhaseTicks > 4;
        if (passedBelowTarget || flewPastTarget || resolverPhaseTicks > 14) {
            resolverDivePhase = false;
            resolverPhaseTicks = 0;
        }
    }

    private Vec3d resolveResolverFlightPoint(Vec3d predict) {
        Vec3d anchor = predict;
        Vec3d travel = target.getVelocity();
        Vec3d horizontalTravel = new Vec3d(travel.x, 0.0, travel.z);
        if (horizontalTravel.lengthSquared() < 1.0E-4) {
            Vec3d fallback = predict.subtract(mc.player.getEyePos());
            horizontalTravel = new Vec3d(fallback.x, 0.0, fallback.z);
        }
        if (horizontalTravel.lengthSquared() < 1.0E-4) {
            horizontalTravel = new Vec3d(0.0, 0.0, 1.0);
        }

        horizontalTravel = horizontalTravel.normalize();
        Vec3d lateral = new Vec3d(-horizontalTravel.z, 0.0, horizontalTravel.x);
        double sideSign = ((resolverHitsOnTarget + (resolverDivePhase ? 1 : 0)) & 1) == 0 ? 1.0 : -1.0;
        double sideOffset = resolverDivePhase ? 0.35 * sideSign : 0.65 * sideSign;
        double forwardOffset = resolverDivePhase ? 0.18 : -0.38;
        double verticalOffset = resolverDivePhase ? -0.95 : 1.35;

        if (!resolverDivePhase && mc.player.getY() > predict.y + 2.2) {
            verticalOffset = 0.95;
        }
        if (resolverDivePhase && mc.player.getY() < predict.y - 0.7) {
            verticalOffset = -0.25;
        }

        return anchor
                .add(horizontalTravel.multiply(forwardOffset))
                .add(lateral.multiply(sideOffset))
                .add(0.0, verticalOffset, 0.0);
    }

    private void chaseResolverTarget(Vec3d predict) {
        if (!this.isResolverActive()) {
            return;
        }

        boolean strictServer = this.isStrictElytraServer();
        Vec3d flightPoint = this.resolveResolverFlightPoint(predict);
        Vec3d playerEye = mc.player.getEyePos();
        Vec3d toPoint = flightPoint.subtract(playerEye);
        double distance = toPoint.length();
        if (distance < 0.2) {
            return;
        }

        Vec3d currentVel = mc.player.getVelocity();
        Vec3d desiredDir = toPoint.normalize();
        if (strictServer && elytraVelocityAssistCooldown > 0) {
            return;
        }
        double desiredSpeed = MathHelper.clamp(0.9 + distance * (strictServer ? 0.055 : 0.09), 0.9, strictServer ? 2.22 : 2.8);
        if (isSlowdownActive && !resolverDivePhase) {
            desiredSpeed *= strictServer ? 0.88 : 0.82;
        }
        if (resolverDivePhase) {
            desiredSpeed = Math.min(strictServer ? 2.38 : 3.0, desiredSpeed + (strictServer ? 0.08 : 0.2));
        }

        Vec3d desiredVel = desiredDir.multiply(desiredSpeed);
        double minVertical = resolverDivePhase ? (strictServer ? -0.12 : -0.22) : (strictServer ? 0.03 : 0.08);
        double maxVertical = resolverDivePhase ? (strictServer ? 0.10 : 0.18) : (strictServer ? 0.22 : 0.55);
        desiredVel = new Vec3d(desiredVel.x, MathHelper.clamp(desiredVel.y, minVertical, maxVertical), desiredVel.z);

        double blend = resolverDivePhase ? (strictServer ? 0.14 : 0.30) : (strictServer ? 0.11 : 0.22);
        Vec3d newVel = currentVel.add(desiredVel.subtract(currentVel).multiply(blend));
        if (strictServer) {
            newVel = this.limitVelocityDelta(currentVel, newVel, resolverDivePhase ? 0.105 : 0.085, 0.04);
        }

        double horizontalSpeed = Math.hypot(newVel.x, newVel.z);
        double minHorizontalSpeed = resolverDivePhase ? (strictServer ? 0.36 : 0.55) : (strictServer ? 0.28 : 0.40);
        if (horizontalSpeed < minHorizontalSpeed) {
            Vec3d horizontalDir = new Vec3d(desiredDir.x, 0.0, desiredDir.z);
            if (horizontalDir.lengthSquared() > 1.0E-4) {
                horizontalDir = horizontalDir.normalize().multiply(minHorizontalSpeed);
                newVel = new Vec3d(horizontalDir.x, newVel.y, horizontalDir.z);
            }
        }

        double speed = newVel.length();
        double maxSpeed = strictServer ? 2.38 : 3.0;
        if (speed > maxSpeed) {
            newVel = newVel.multiply(maxSpeed / speed);
        }

        mc.player.setVelocity(newVel);
        if (strictServer) {
            elytraVelocityAssistCooldown = resolverDivePhase ? 1 : 2;
        }
    }

    private void updateResolverRotation(Vec3d predict) {
        Vec3d flightPoint = this.resolveResolverFlightPoint(predict);
        float[] resolverRotation = this.calculateRotation(flightPoint);
        float yawSpeed = resolverDivePhase ? 75.0F : 65.0F;
        float pitchSpeed = resolverDivePhase ? 85.0F : 70.0F;
        RotationProcess.update(new Rotation(resolverRotation[0], resolverRotation[1]), yawSpeed, pitchSpeed, 90.0F, 90.0F, 0, 6, false);
    }

    private Vec3d predictElytraPosLegacy(LivingEntity entity, float ticks) {
        if (entity == null) return mc.player.getEyePos();
        Vec3d pos = entity.getBoundingBox().getCenter();
        if (!elytraPredict.get() || ticks <= 0.01F) {
            lastTargetVelocity = entity.getVelocity();
            return pos;
        }
        Vec3d velocity = entity.getVelocity();
        if (velocity.lengthSquared() < 0.001) {
            lastTargetVelocity = velocity;
            return pos;
        }
        lastTargetVelocity = velocity;
        double leadTicks = ticks;
        if (entity instanceof PlayerEntity player && mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry != null) {
                leadTicks += MathHelper.clamp(entry.getLatency() / 50.0, 0.0, 3.0) * 0.3;
            }
        }
        // Pure linear predict — pos + vel*t
        // Without acceleration — does not fly off randomly
        return this.clampPredictDistance(pos, pos.add(velocity.multiply(leadTicks)), 5.0);
    }

    private void handleElytraTargetTick() {
        Vec3d predict = this.resolveElytraTargetVector(target);
        this.updateTargetLeaveState(predict);
        this.updateElytraTargetState(predict);
        this.updateElytraSlowdown(predict);
        this.updateResolverState(predict);
        this.processAutoFirework(target, predict);
        this.updateRotation();

        if (canElytraAttack(predict)) {
            this.snapElytraAttackRotation();
            Attack.antiMissesHittingUpdate(target, true, false, false);

            // Resolver: cancel only horizontal velocity, keep vertical
            // to avoid dropping sharply on hit
            if (this.isResolverActive()) {
                Vec3d vel = mc.player.getVelocity();
                mc.player.setVelocity(vel.x, Math.min(vel.y, -0.12), vel.z);
            } else if (elytraUseResolver.get() && mc.player.isGliding()) {
                Vec3d vel = mc.player.getVelocity();
                double horizSpeed = Math.hypot(vel.x, vel.z);
                if (horizSpeed > 0.3) {
                    double scale = Math.max(0.0, 1.0 - 0.6 * (horizSpeed / Math.max(horizSpeed, 1.0)));
                    mc.player.setVelocity(vel.x * scale, vel.y, vel.z * scale);
                }
            }

            doAttack();
            // Sweep: hit all other players in the flyby zone
            this.doElytraSweepAttack();
            elytraPostHitTicks = this.isResolverActive() ? 3 : 6;
            if (this.isResolverActive()) {
                resolverDivePhase = false;
                resolverPhaseTicks = 0;
                resolverHitsOnTarget++;
            }
            // With an active firework — short delay between hits (5 ticks instead of 10)
            // to keep up with hits during fast circular flight
            boolean usingFirework = mc.player.isUsingItem()
                    && mc.player.getActiveItem().getItem() instanceof FireworkRocketItem;
            ticksToAttack = usingFirework ? 3 : 4;
        } else if (target != null && mc.player.isGliding() && target.isGliding()) {
            if (this.isResolverActive()) {
                this.chaseResolverTarget(predict);
            } else {
                this.chaseElytraTarget(predict);
            }
        }
    }

    private void updateElytraTargetState(Vec3d predict) {
        isTurnaroundActive = false;
        if (target == null || mc.player == null || !target.isGliding()) {
            preddict = elytraHitAfterOvertake.get() ? 2.7F : 4.0F;
            elytraPostHitTicks = 0;
            return;
        }

        if (this.isPostHitOvertakeActive()) {
            preddict = Math.max(preddict, 4.6F);
            return;
        }

        double distanceToPredict = mc.player.getEyePos().distanceTo(predict);
        boolean targetBehind = this.isTargetBehindForElytra(predict);

        // "Hit after overtaking" — detect that the target is behind us
        // If enabled: expand attack zone to 4.5 when target is behind (overtaken)
        // If disabled: standard zone 4.0
        if (elytraHitAfterOvertake.get()) {
            preddict = targetBehind ? 4.5F : 2.7F;
        } else {
            preddict = 4.0F;
        }

        if (distanceToPredict <= preddict + 1.5 && elytraTurnaround.get()) {
            isTurnaroundActive = true;
        }
    }

    private void updateElytraSlowdown(Vec3d predict) {
        if (!elytraSlowdown.get() || !mc.player.isGliding()) {
            isSlowdownActive = false;
            return;
        }

        double distanceToPredict = mc.player.getEyePos().distanceTo(predict);
        if (isPreHitSlowdownMode()) {
            // Mode "Before attack" — slow down for N ticks before the attack
            isSlowdownActive = ticksToAttack <= elytraPreHitTicks.get().intValue();
        } else {
            // Mode "By radius" — use elytraSlowdownRadius, not predict
            double slowRadius = elytraSlowdownRadius.get();
            isSlowdownActive = distanceToPredict < slowRadius;
        }
    }

    private boolean isPreHitSlowdownMode() {
        return elytraSlowdownMode.modes.size() > 1 && elytraSlowdownMode.currentMode.equalsIgnoreCase(elytraSlowdownMode.modes.get(1));
    }

    private boolean isTargetBehindForElytra(Vec3d predict) {
        if (mc.player == null) {
            return false;
        }

        Vec3d toTarget = predict.subtract(mc.player.getEyePos());
        if (toTarget.lengthSquared() < 1.0E-4) {
            return false;
        }

        Vec3d lookVec = mc.player.getRotationVec(1.0F);
        return lookVec.dotProduct(toTarget.normalize()) < 0.0;
    }

    private float getElytraYawDiffToPoint(Vec3d point) {
        if (mc.player == null) {
            return 180.0F;
        }

        float targetYaw = this.calculateRotation(point)[0];
        return Math.abs(MathHelper.wrapDegrees(this.currentAuraYaw() - targetYaw));
    }

    private double getElytraClosingSpeed(Vec3d predict) {
        if (target == null || mc.player == null) {
            return 0.0;
        }

        Vec3d toTarget = predict.subtract(mc.player.getEyePos());
        if (toTarget.lengthSquared() < 1.0E-4) {
            return 0.0;
        }

        Vec3d relativeVelocity = mc.player.getVelocity().subtract(target.getVelocity());
        return relativeVelocity.dotProduct(toTarget.normalize());
    }

    private double getLeetLikeElytraAttackDistance(LivingEntity entity) {
        if (entity == null) {
            return 3.2;
        }
        if (!entity.isGliding()) {
            return 3.2;
        }
        double distance = Math.max(this.targetIsLeaving(entity) ? 5.0F : 3.8F, attackRange.get());
        if (this.isPostHitOvertakeActive()) {
            distance = Math.max(distance, Math.max(preddict, 4.8F));
        }
        return distance;
    }

    private float getElytraAttackLead(LivingEntity entity) {
        if (!shouldUseElytraPredict()) {
            return 0.0F;
        }
        if (entity == null || mc.player == null) {
            return 0.8F;
        }

        double distance = mc.player.getEyePos().distanceTo(entity.getBoundingBox().getCenter());
        double targetSpeed = Math.hypot(entity.getVelocity().x, entity.getVelocity().z);
        double ourSpeed = Math.hypot(mc.player.getVelocity().x, mc.player.getVelocity().z);
        float lead = 0.55F;
        lead += (float)Math.min(0.35, targetSpeed * 0.28);
        lead += (float)Math.min(0.25, distance * 0.020);
        if (this.targetIsLeaving(entity)) {
            lead += 0.20F;
        }
        if (mc.player.isUsingItem() && mc.player.getActiveItem().getItem() instanceof FireworkRocketItem) {
            lead += 0.15F;
        }
        return MathHelper.clamp(lead, 0.45F, 1.2F);
    }

    private boolean canElytraAttack(Vec3d predict) {
        if (target == null || mc.player == null) return false;

        boolean postHitWindow = this.isPostHitOvertakeActive();
        boolean cooldownReady = Attack.msCooldownReached()
                || postHitWindow && Attack.msCooldownHasMs(Math.max(160L, Attack.getMsCooldown() - 35L));
        if (!cooldownReady) return false;

        boolean weaponReady = this.isAttackCooldownReady()
                || postHitWindow && mc.player.getAttackCooldownProgress(0.0F) >= 0.84F;
        if (!weaponReady) return false;
        if (ticksToAttack > 0) return false;

        if (target.isGliding()) {
            Vec3d aimPoint = this.resolveElytraAimPoint(target);
            double distanceToHitbox = this.distanceToBox(mc.player.getEyePos(), target.getBoundingBox().expand(0.12D));
            boolean targetBehind = this.isTargetBehindForElytra(predict);

            boolean usingFirework = mc.player.isUsingItem()
                    && mc.player.getActiveItem().getItem() instanceof FireworkRocketItem;
            double effectivePreddict = this.getLeetLikeElytraAttackDistance(target);
            effectivePreddict = Math.max(effectivePreddict, preddict);
            if (usingFirework) {
                effectivePreddict += 0.5;
            }
            boolean bodyContactWindow = mc.player.getBoundingBox().expand(0.85D).intersects(target.getBoundingBox().expand(0.12D));
            boolean forceHitWindow = bodyContactWindow;
            if (postHitWindow) {
                forceHitWindow = forceHitWindow || distanceToHitbox <= effectivePreddict + 0.45;
            }
            if (elytraHitAfterOvertake.get() && targetBehind) {
                effectivePreddict = Math.max(effectivePreddict, 5.0);
                forceHitWindow = forceHitWindow || distanceToHitbox <= effectivePreddict + 0.5;
            }

            if (distanceToHitbox > effectivePreddict && !forceHitWindow) return false;
            // Yaw and raycast checks removed — we do not rely on anticheat
        }

        return true;
    }
    private float currentAuraYaw() {
        return silentActive ? spoofYaw : mc.player.getYaw();
    }

    private float currentAuraPitch() {
        return silentActive ? spoofPitch : mc.player.getPitch();
    }

    private Vec3d predictTargetPoint(LivingEntity target, float lead) {
        if (target == null) {
            return mc.player == null ? Vec3d.ZERO : mc.player.getEyePos();
        }
        if (lead <= 0.01F) {
            return target.getBoundingBox().getCenter();
        }
        return this.predictElytraPos(target, lead, false);
    }

    private Vec3d clampPredictDistance(Vec3d origin, Vec3d predicted, double maxDistance) {
        Vec3d offset = predicted.subtract(origin);
        double length = offset.length();
        if (length <= maxDistance || length < 1.0E-4) {
            return predicted;
        }
        return origin.add(offset.multiply(maxDistance / length));
    }

    private void processAutoFirework(LivingEntity entity, Vec3d predict) {
        if (!elytraAutoFirework.get() || mc.player == null || mc.interactionManager == null || mc.getNetworkHandler() == null) {
            return;
        }
        if (!mc.player.isGliding() || entity == null) {
            return;
        }
        if (mc.player.isUsingItem() && mc.player.getActiveItem().getItem() instanceof FireworkRocketItem) {
            return;
        }

        long now = System.currentTimeMillis();
        int delay = Math.round(elytraFireworkDelay.get());
        if (elytraSmartFirework.get()) {
            long sinceAttack = now - lastElytraAttackTime;
            if (sinceAttack >= 80L && sinceAttack <= 220L && now - lastFireworkUseTime >= 200L && mc.player.isGliding()) {
                if (this.useFireworkFromInventory()) {
                    lastFireworkUseTime = now;
                }
                return;
            }
            delay = this.getSmartFireworkDelay(entity, predict);
        }

        if (now - lastFireworkUseTime < delay) {
            return;
        }

        if (this.useFireworkFromInventory()) {
            lastFireworkUseTime = now;
        }
    }

    private int getSmartFireworkDelay(LivingEntity entity, Vec3d predict) {
        int delay = 400;
        double distance = mc.player.getEyePos().distanceTo(predict);
        if (distance < 4.0) {
            delay = 270;
        }
        if (this.targetIsLeaving(entity)) {
            delay = 400;
        }
        if (entity.isOnGround() || !entity.isGliding()) {
            delay = 450;
        }
        if (this.isPostHitOvertakeActive()) {
            delay = 220;
        }
        if (System.currentTimeMillis() - lastElytraAttackTime < 260L) {
            delay = Math.min(delay, 200);
        }
        return delay;
    }

    private void updateTargetLeaveState(Vec3d predict) {
        if (target == null || mc.player == null || !target.isGliding()) {
            targetLeaveStartTime = 0L;
            return;
        }

        Vec3d toTarget = predict.subtract(mc.player.getEyePos());
        Vec3d horizontalToTarget = new Vec3d(toTarget.x, 0.0, toTarget.z);
        Vec3d horizontalVelocity = new Vec3d(target.getVelocity().x, 0.0, target.getVelocity().z);
        if (horizontalToTarget.lengthSquared() < 1.0E-4 || horizontalVelocity.lengthSquared() < 1.0E-4) {
            targetLeaveStartTime = 0L;
            return;
        }

        double movingAway = horizontalVelocity.normalize().dotProduct(horizontalToTarget.normalize());
        boolean leaveCandidate = movingAway > 0.38 && horizontalVelocity.lengthSquared() > 0.18;
        if (leaveCandidate) {
            if (targetLeaveStartTime == 0L) {
                targetLeaveStartTime = System.currentTimeMillis();
            }
        } else {
            targetLeaveStartTime = 0L;
        }
    }

    private boolean targetIsLeaving(LivingEntity entity) {
        return entity != null && entity.isGliding() && targetLeaveStartTime != 0L && System.currentTimeMillis() - targetLeaveStartTime >= 1500L;
    }

    private boolean useFireworkFromInventory() {
        if (mc.player.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            return true;
        }

        int slotId = this.findFireworkInventorySlot();
        if (slotId == -1) {
            return false;
        }

        int prevSelected = mc.player.getInventory().getSelectedSlot();
        boolean isHotbarSlot = slotId < 9;
        int hotbarIndex = isHotbarSlot ? slotId : prevSelected;

        if (isHotbarSlot) {
            if (prevSelected != hotbarIndex) {
                mc.player.getInventory().setSelectedSlot(hotbarIndex);
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarIndex));
            }

            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);

            if (prevSelected != hotbarIndex) {
                mc.player.getInventory().setSelectedSlot(prevSelected);
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSelected));
            }
            return true;
        }

        int screenSlot = this.toScreenSlot(slotId);
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, screenSlot, hotbarIndex, SlotActionType.SWAP, mc.player);
        mc.player.getInventory().setSelectedSlot(hotbarIndex);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarIndex));
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, screenSlot, hotbarIndex, SlotActionType.SWAP, mc.player);
        mc.player.getInventory().setSelectedSlot(prevSelected);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSelected));
        return true;
    }

    private int findFireworkInventorySlot() {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.FIREWORK_ROCKET)) {
                return i;
            }
        }
        return -1;
    }

    private int toScreenSlot(int inventorySlot) {
        return inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
    }

    private Vec3d findTargetFireworkPoint(LivingEntity entity, Vec3d fallback) {
        Vec3d best = fallback;
        double bestDistance = 9.0;
        for (Entity worldEntity : mc.world.getEntities()) {
            if (!(worldEntity instanceof FireworkRocketEntity firework)) {
                continue;
            }
            Entity owner = firework.getOwner();
            if (owner != entity) {
                continue;
            }

            Vec3d pos = entityPos(firework);
            double distance = pos.distanceTo(fallback);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos;
            }
        }
        return best;
    }

    /**
     * For elytra combat aim at the body center with predict — max chance to hit
     */
    private Vec3d resolveElytraAimPoint(LivingEntity entity) {
        if (entity == null) return mc.player == null ? Vec3d.ZERO : mc.player.getEyePos();
        if (mc.player == null) return entity.getBoundingBox().getCenter();

        Vec3d defaultCenter = entity.getBoundingBox().getCenter();
        if (!shouldUseElytraPredict()) {
            return this.clampToVirtualTargetBox(entity, defaultCenter, mc.player.getEyePos());
        }
        Vec3d predictedCenter = this.predictTargetPoint(entity, this.getElytraAttackLead(entity));

        if (entity.isGliding() && !entity.isOnGround()) {
            predictedCenter = this.resolveResolverPoint(entity, predictedCenter);
        }

        // maxOffset increased — do not clamp the predict so aggressively
        double attackOffset = predictedCenter.distanceTo(defaultCenter);
        double maxOffset = entity.isGliding() ? 2.5 : 0.8;
        if (attackOffset > maxOffset && attackOffset > 1.0E-4) {
            predictedCenter = defaultCenter.lerp(predictedCenter, maxOffset / attackOffset);
        }

        if (elytraAimMode.is(AIM_NORMAL)) {
            return this.clampToVirtualTargetBox(entity, defaultCenter, mc.player.getEyePos());
        }
        if (elytraAimMode.is(AIM_MIDDLE) || elytraAimMode.is(AIM_RESOLVER)) {
            return this.clampToVirtualTargetBox(entity, defaultCenter.lerp(predictedCenter, 0.5), mc.player.getEyePos());
        }
        if (elytraAimMode.is(AIM_FIREWORK)) {
            Vec3d fireworkPoint = this.findTargetFireworkPoint(entity, predictedCenter);
            if (fireworkPoint.distanceTo(defaultCenter) > 1.6) {
                fireworkPoint = defaultCenter.lerp(fireworkPoint, 0.55);
            }
            return this.clampToVirtualTargetBox(entity, fireworkPoint, mc.player.getEyePos());
        }

        // Mode "Resolve" and default — aim at predict with a small downward offset (hitbox center)
        Vec3d aimPoint = new Vec3d(
                predictedCenter.x,
                predictedCenter.y - entity.getHeight() * 0.15, // slightly below center — more reliable hit
                predictedCenter.z
        );
        return this.clampToVirtualTargetBox(entity, aimPoint, mc.player.getEyePos());
    }

    private void snapElytraAttackRotation() {
        if (target == null || mc.player == null) {
            return;
        }

        float[] attackRotation = this.calculateRotation(this.resolveElytraAimPoint(target));
        mc.player.setYaw(attackRotation[0]);
        mc.player.setPitch(attackRotation[1]);
        mc.player.headYaw = attackRotation[0];
        mc.player.bodyYaw = attackRotation[0];
        silentActive = true;
        spoofYaw = attackRotation[0];
        spoofPitch = attackRotation[1];
        RotationProcess.update(new Rotation(attackRotation[0], attackRotation[1]), 90.0F, 90.0F, 90.0F, 90.0F, 0, 10, false);
    }

    private boolean applyElytraDefensiveRotation() {
        if (!elytraPredictDefensive.get() || target == null || mc.player == null) {
            return false;
        }

        float[] defensiveRotation = this.calculateRotation(this.resolveElytraAimPoint(target));
        spoofPitch = defensiveRotation[1];
        RotationProcess.update(new Rotation(defensiveRotation[0], defensiveRotation[1]), 160.0F, 160.0F, 90.0F, 90.0F, 0, 4, false);
        return true;
    }
    private float[] calculateRotation(Vec3d point) {
        Vec3d diff = point.subtract(mc.player.getEyePos());
        double horizontal = Math.hypot(diff.x, diff.z);
        float yaw = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float pitch = (float)(-Math.toDegrees(Math.atan2(diff.y, horizontal)));
        return new float[]{yaw, MathHelper.clamp(pitch, -89.0F, 89.0F)};
    }

    /**
     * Sweep attack on elytra — hit all valid players within the flyby radius,
     * not only the current target. Works only while flying on elytra.
     */
    private void doElytraSweepAttack() {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isGliding()) return;

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d playerVel = mc.player.getVelocity();
        // Sweep radius — wide to catch everyone in the flyby zone
        float sweepRadius = attackRange.get() + 2.5f;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity == target) continue;
            if (!this.isValidTarget(living)) continue;

            double dist = eyePos.distanceTo(living.getBoundingBox().getCenter());
            if (dist > sweepRadius) continue;

            // Check that the entity is in the movement cone (flying toward or near it)
            boolean inContact = mc.player.getBoundingBox().expand(1.5).intersects(living.getBoundingBox());
            boolean inFlightCone = false;
            if (playerVel.lengthSquared() > 0.01) {
                Vec3d velDir = playerVel.normalize();
                Vec3d toEntity = living.getBoundingBox().getCenter().subtract(eyePos).normalize();
                inFlightCone = velDir.dotProduct(toEntity) > 0.3; // ~72° cone
            }
            boolean canSeeEntity = mc.player.canSee(living);

            if (!inContact && !inFlightCone && !canSeeEntity) continue;

            // Attack without changing target and without cooldown check — sweep is an instant hit on flyby
            LivingEntity savedTarget = target;
            target = living;
            Attack.useEntity(living,
                    () -> {},
                    () -> {},
                    Hand.MAIN_HAND,
                    true
            );
            target = savedTarget;
        }
    }

    private void doAttack() {
        if (target == null) return;
        if (!this.isAttackCooldownReady()) return;
        Runnable[] shieldBreak = Attack.hitShieldBreakTaskForUse(target, misc.get(M_BREAK_SHIELD));
        Runnable[] shieldPressBypass = Attack.resetShieldSilentTaskForUse(true);
        boolean canPacket = this.canUseSprintSpoofPackets();
        Runnable[] skipSilentSprint = Attack.skipSilentSprintingTaskForUse(canPacket);
        Runnable pre = () -> {
            skipSilentSprint[0].run();
            shieldPressBypass[0].run();
            shieldBreak[0].run();
            if (!this.isLegitSprintEnabled() && mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
            }
        };
        Runnable post = () -> {
            if (!this.isLegitSprintEnabled() && mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(this.currentPlayerInput()));
            }
            shieldBreak[1].run();
            shieldPressBypass[1].run();
            skipSilentSprint[1].run();
        };
        if (misc.get(M_PRESS_SHIELD) && mc.player.getActiveItem().getItem().equals(Items.SHIELD) && mc.player.isUsingItem()) {
            mc.interactionManager.stopUsingItem(mc.player);
        }
        this.stopSprintForAuraHit();
        Attack.useEntity(target, pre, post, Hand.MAIN_HAND, true);
        lastElytraAttackTime = System.currentTimeMillis();
    }

    private PlayerInput currentPlayerInput() {
        return new PlayerInput(
                mc.options.forwardKey.isPressed(),
                mc.options.backKey.isPressed(),
                mc.options.leftKey.isPressed(),
                mc.options.rightKey.isPressed(),
                mc.options.jumpKey.isPressed(),
                mc.options.sneakKey.isPressed(),
                mc.options.sprintKey.isPressed()
        );
    }

    private void handleAutoEat() {
        if (mc.player == null || mc.world == null) {
            this.releaseAutoEatUseKey();
            return;
        }
        if (mc.player.getHealth() > 15.0f) {
            this.releaseAutoEatUseKey();
            return;
        }
        if (mc.player.isUsingItem()) return;
        int bestFoodSlot = -1;
        float bestSaturation = 0;
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            var item = stack.getItem();
            if (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE) {
                bestFoodSlot = i;
                break;
            }
            FoodComponent foodComponent = stack.get(DataComponentTypes.FOOD);
            if (foodComponent != null) {
                float saturation = foodComponent.saturation();
                if (saturation > bestSaturation) {
                    bestSaturation = saturation;
                    bestFoodSlot = i;
                }
            }
        }
        if (bestFoodSlot != -1) {
            mc.player.getInventory().setSelectedSlot(bestFoodSlot);
            mc.options.useKey.setPressed(true);
            this.autoEatUseKeyHeld = true;
        } else {
            this.releaseAutoEatUseKey();
        }
    }

    public static float[] getRanges() {
        return new float[]{attackRange.get(), preRange.get()};
    }

    public boolean isRayCastRuleToAttack() {
        if (isObfuscatedRuntime()) return false;
        return raycastCheck.get();
    }

    public void attackEntity() {
        if (target == null || mc.player == null) {
            return;
        }

        if (!rotationType.is("SpookyTime")) {
            this.tryQuickAttack();
            return;
        }

        this.updateRotation();
        if (AuraUtil.getStrictDistance(target) >= attackRange.get()) {
            return;
        }

        float[] ranges = getRanges();
        boolean isElytraCombat = mc.player.isGliding() && target.isGliding();
        if (!Attack.shouldAttack(target, this.isRayCastRuleToAttack(), true, !isElytraCombat, 0L, ranges)) {
            return;
        }

        Runnable[] shieldBreak = Attack.hitShieldBreakTaskForUse(target, misc.get(M_BREAK_SHIELD));
        Runnable[] shieldPressBypass = Attack.resetShieldSilentTaskForUse(true);
        Runnable[] skipSilentSprint = Attack.skipSilentSprintingTaskForUse(this.canUseSprintSpoofPackets());
        Runnable preHit = () -> {
            skipSilentSprint[0].run();
            shieldPressBypass[0].run();
            shieldBreak[0].run();
        };
        Runnable postHit = () -> {
            shieldBreak[1].run();
            shieldPressBypass[1].run();
            skipSilentSprint[1].run();
        };
        if (misc.get(M_PRESS_SHIELD) && mc.player.getActiveItem().isOf(Items.SHIELD) && mc.player.isUsingItem()) {
            mc.interactionManager.stopUsingItem(mc.player);
        }
        this.stopSprintForAuraHit();
        Attack.useEntity(target, preHit, postHit, Hand.MAIN_HAND, true);
    }

    private void tryQuickAttack() {
        float chargeLead = rotationType.is("FunTime") ? 0.85F : 0.0F;
        if (mc.player.getAttackCooldownProgress(chargeLead) < 1.0F) {
            return;
        }

        double reach = Math.min(attackRange.get(), mc.player.getEntityInteractionRange());
        if (AuraUtil.getStrictDistance(target) > reach) {
            return;
        }

        // Use the look already sent by vanilla movement before sending the attack.
        ClientPlayerEntityAccessor lastLook = (ClientPlayerEntityAccessor) mc.player;
        if (RayTraceUtil.calculateRayTrace(reach, lastLook.getLastYaw(), lastLook.getLastPitch(), mc.player, false)
                instanceof EntityHitResult hit && hit.getEntity() == target) {
            Attack.useEntity(target, null, null, Hand.MAIN_HAND, false);
        }
    }

    private void updateRotation() {

        if (this.applyElytraDefensiveRotation()) {
            return;
        }

        if (this.isResolverActive()) {
            Vec3d predict = this.resolveElytraTargetVector(target);
            this.updateResolverRotation(predict);
            return;
        }

        if (this.isStrictElytraServer() && target != null && mc.player != null && mc.player.isGliding() && target.isGliding()) {
            Vec3d predict = this.predictElytraPos(target, getEffectiveElytraPredictValue(), false);
            Vec3d flightPoint = this.resolveSmartOvertakePoint(predict);
            float[] rotation = this.calculateRotation(flightPoint);
            RotationProcess.update(new Rotation(rotation[0], rotation[1]), 42.0F, 28.0F, 90.0F, 90.0F, 0, 7, false);
            return;
        }

        if (target != null && mc.player != null && mc.player.isGliding() && target.isGliding() && isTurnaroundActive) {
            // For elytra combat aim at the lower third of the body — optimal hit point on flyby
            float[] turnaroundRotation = this.calculateRotation(this.resolveElytraAimPoint(target));
            float turnaroundYawSpeed = 360.0F;
            float turnaroundPitchSpeed = 360.0F;
            RotationProcess.update(new Rotation(turnaroundRotation[0], turnaroundRotation[1]), turnaroundYawSpeed, turnaroundPitchSpeed, 90.0F, 90.0F, 0, 5, false);
            return;
        }

        switch (rotationType.get()) {
            case "FunTime" -> Rotate.onQuickRotation(target, 82.0F, 53.0F, 0.78F);
            case "ReallyWorld" -> Rotate.onQuickRotation(target, 76.0F, 49.0F, 0.72F);
            case "CakeWorld" -> Rotate.onQuickRotation(target, 70.0F, 45.0F, 0.68F);
            default -> {
                boolean isElytraCombat = mc.player.isGliding() && target.isGliding();
                boolean canAttack = Attack.shouldAttack(target, false, true, !isElytraCombat, 0L, getRanges());
                Rotate.onSpookyRotation(target, canAttack);
            }
        }
    }

    private void updateTarget() {
        // If the target is still valid and the cooldown has not expired — do not scan the whole world every tick
        if (targetScanCooldown > 0 && target != null && this.isValidTarget(target)) {
            targetScanCooldown--;
            return;
        }
        targetScanCooldown = 3;
        // Lock on current target — don't switch if it's still valid and in range
        if (target != null && this.isValidTarget(target)
                && mc.player.distanceTo(target) <= attackRange.get() + preRange.get()) {
            return;
        }

        LivingEntity bestTarget = null;
        boolean playerGliding = mc.player.isGliding();
        double bestScore = Double.MAX_VALUE;
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0F).normalize();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!this.isValidTarget(living)) continue;

            double score;
            if (playerGliding && living.isGliding()) {
                // Elytra combat — by look angle
                Vec3d targetPos = this.predictTargetPoint(living, Math.min(getEffectiveElytraPredictValue(), 2.6F));
                Vec3d toTarget = targetPos.subtract(eyePos).normalize();
                score = Math.acos(MathHelper.clamp(lookVec.dotProduct(toTarget), -1.0, 1.0));
            } else {
                // Normal combat
                score = mc.player.distanceTo(living);
            }

            if (score < bestScore) {
                bestScore = score;
                bestTarget = living;
            }
        }
        target = bestTarget;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity instanceof ClientPlayerEntity) return false;
        if (entity instanceof PlayerEntity && AntiBot.bot.contains(entity)) return false;
        float maxDist = mc.player.isGliding() && entity.isGliding() ? 50.0f : attackRange.get() + preRange.get();
        if (mc.player.distanceTo(entity) > maxDist) return false;
        if (!(mc.player.isGliding() && entity.isGliding()) && !misc.get(M_THROUGH_WALLS) && !mc.player.canSee(entity)) return false;
        if (entity instanceof PlayerEntity p && Elexis.getInstance().getFriendStorage().isFriend(p.getName().getString())) return false;
        if (entity instanceof PlayerEntity && !targets.get(T_PLAYERS)) return false;
        if (entity instanceof PlayerEntity && entity.getArmor() == 0 && !targets.get(T_NAKED)) return false;
        if (entity instanceof PlayerEntity && ((PlayerEntity) entity).isCreative()) return false;
        if ((entity instanceof Monster || entity instanceof SlimeEntity || entity instanceof VillagerEntity || entity instanceof AnimalEntity) && !targets.get(T_MOBS)) return false;
        return !entity.isInvulnerable() && entity.isAlive() && !(entity instanceof ArmorStandEntity);
    }

    private void renderPredictPoint(EventRender.World event, Vec3d predictPos) {
    }

    private void renderElytraTargetLine(EventRender.World event, Vec3d predictPos) {
    }

    private void reset() {
        target = null;
        silentActive = false;
        isSlowdownActive = false;
        isTurnaroundActive = false;
        ticksToAttack = 0;
        elytraPostHitTicks = 0;
        elytraVelocityAssistCooldown = 0;
        targetScanCooldown = 0;
        lastTargetVelocity = Vec3d.ZERO;
        this.resetResolverState();
        Rotate.resetTransientState();
        MovementManager.getInstance().unlockMovement("Aura");
        lastFireworkUseTime = 0L;
        lastElytraAttackTime = 0L;
        targetLeaveStartTime = 0L;
        this.releaseAutoEatUseKey();
        this.resetPredictPointSmoothing();
    }

    private void stopSprintForAuraHit() {
        if (mc.player == null || mc.options == null || mc.player.isTouchingWater() || this.isLegitSprintEnabled()) {
            return;
        }
        mc.player.setSprinting(false);
        mc.options.sprintKey.setPressed(false);
    }

    private boolean isLegitSprintEnabled() {
        return !rotationType.is("SpookyTime") || extraSettings.get(E_LEGIT_SPRINT);
    }

    private boolean canUseSprintSpoofPackets() {
        return mc.player != null
                && !mc.player.isTouchingWater()
                && !this.isLegitSprintEnabled()
                && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !mc.player.isFlyingVehicle();
    }

    private void releaseAutoEatUseKey() {
        if (!this.autoEatUseKeyHeld) {
            return;
        }
        this.autoEatUseKeyHeld = false;
        if (mc.options != null) {
            mc.options.useKey.setPressed(false);
        }
    }

    private void resetPredictPointSmoothing() {
        this.smoothedPredictPoint = null;
        this.smoothedPredictTarget = null;
    }

    private Vec3d smoothPredictPoint(LivingEntity entity, Vec3d point) {
        if (entity == null || point == null) {
            this.resetPredictPointSmoothing();
            return point;
        }
        if (this.smoothedPredictTarget != entity || this.smoothedPredictPoint == null) {
            this.smoothedPredictTarget = entity;
            this.smoothedPredictPoint = point;
            return point;
        }
        this.smoothedPredictPoint = this.smoothedPredictPoint.lerp(point, 0.38);
        return this.smoothedPredictPoint;
    }

    private boolean isResolverStoyak(LivingEntity entity) {
        Vec3d velocity = entity.getVelocity();
        return velocity.horizontalLengthSquared() < 0.0036 && Math.abs(velocity.y) < 0.04;
    }

    private Vec3d getEntityUnlerpedPos(LivingEntity entity) {
        Vec3d velocity = entity.getVelocity();
        return new Vec3d(entity.getX() - velocity.x, entity.getY() - velocity.y, entity.getZ() - velocity.z);
    }

    private boolean shouldResolverMovePredict(LivingEntity entity) {
        return entity.getVelocity().horizontalLengthSquared() > 0.01 || Math.abs(entity.getVelocity().y) > 0.06;
    }

    private double getResolverLatencyFactor(LivingEntity entity) {
        double speed = Math.sqrt(entity.getVelocity().horizontalLengthSquared());
        return MathHelper.clamp(0.35 + speed * 4.5, 0.35, 1.15);
    }

    private double distanceToBox(Vec3d point, Box box) {
        double x = MathHelper.clamp(point.x, box.minX, box.maxX);
        double y = MathHelper.clamp(point.y, box.minY, box.maxY);
        double z = MathHelper.clamp(point.z, box.minZ, box.maxZ);
        return point.distanceTo(new Vec3d(x, y, z));
    }
    private Vec3d clampToTargetBox(LivingEntity entity, Vec3d point, Vec3d eyePos) {
        if (entity == null || point == null) {
            return point;
        }
        Box box = entity.getBoundingBox().expand(0.08, 0.12, 0.08);
        return new Vec3d(
                MathHelper.clamp(point.x, box.minX, box.maxX),
                MathHelper.clamp(point.y, box.minY + 0.08, box.maxY - 0.05),
                MathHelper.clamp(point.z, box.minZ, box.maxZ)
        );
    }

    private Vec3d clampToVirtualTargetBox(LivingEntity entity, Vec3d point, Vec3d eyePos) {
        return this.clampToTargetBox(entity, point, eyePos);
    }
    private boolean checkToAttack() {
        boolean usingBlockableItem = mc.player.isUsingItem()
                && !(mc.player.getActiveItem().getItem() instanceof ShieldItem)
                && !(mc.player.getActiveItem().getItem() instanceof FireworkRocketItem);
        return usingBlockableItem && misc.get(M_NO_EATING)
                || mc.currentScreen != null && misc.get(M_NO_CONTAINERS)
                || !mc.player.getMainHandStack().isIn(ItemTags.SWORDS)
                && !mc.player.getMainHandStack().isIn(ItemTags.AXES)
                && misc.get(M_ONLY_WEAPON);
    }

    @Override
    public void onDisable() {
        Elexis.getEventBus().unsubscribe(this.freeLookUtil);
        Elexis.getEventBus().unsubscribe(this.rotationProcess);
        RotationProcess.stop();
        super.onDisable();
        this.reset();
    }
}

