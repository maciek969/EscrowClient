package elexis.modules.impl.combat;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.entity.EventCrosshairUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

@FunctionAdd(name = "TriggerBot", alias = "TriggerBot", category = Category.Combat,
        description = "Критические удары по цели под прицелом при полном восстановлении")
public final class TriggerBot extends Function {
    private final SliderSetting range = new SliderSetting("Range", 3f, 1f, 3f, 0.1f);
    private final BooleanSetting instantHit = new BooleanSetting("Instant hit", true);
    private final SliderSetting delay = new SliderSetting("Reaction (ms)", 0f, 0f, 500f, 10f)
            .setVisible(() -> !instantHit.get());
    private final BooleanSetting weaponOnly = new BooleanSetting("Weapon only", true);
    private final BooleanSetting playersOnly = new BooleanSetting("Players only", true);
    private final BooleanSetting targetInvisibles = new BooleanSetting("Target invisibles", true);
    private final BooleanSetting ignoreTeam = new BooleanSetting("Ignore teammates", true);
    private final BooleanSetting criticalOnly = new BooleanSetting("Critical hits only", true);
    private LivingEntity target;
    private long readyAt;

    public TriggerBot() {
        addSettings(range, instantHit, delay, weaponOnly, playersOnly, targetInvisibles, ignoreTeam, criticalOnly);
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        tryAttack();
    }

    /** The native crosshair has just been recomputed for this rendered frame. */
    @EventHandler
    public void onCrosshairUpdate(EventCrosshairUpdate event) {
        tryAttack();
    }

    private void tryAttack() {
        if (!operational()
                || !(mc.crosshairTarget instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity living)
                || !managed(living)) {
            reset();
            return;
        }
        long now = System.nanoTime();
        long reactionNanos = instantHit.get() ? 0L : Math.round(delay.get() * 1_000_000d);
        if (target != living) {
            target = living;
            readyAt = now + reactionNanos;
        }
        if ((!instantHit.get() && now < readyAt) || !isAttackReady()) return;
        if (criticalOnly.get() && !isCriticalReady()) return;
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        readyAt = now + reactionNanos;
    }

    private boolean operational() {
        return isState() && !nullCheck() && mc.interactionManager != null && mc.currentScreen == null
                && !mc.isPaused() && mc.player.isAlive() && !mc.player.isSpectator() && !mc.player.isUsingItem()
                && !CombatTargeting.anotherCombatModuleActive()
                && (!weaponOnly.get() || CombatTargeting.holdingWeapon());
    }

    private boolean managed(LivingEntity living) {
        return CombatTargeting.valid(living, Math.min(range.get(), mc.player.getEntityInteractionRange()),
                playersOnly.get(), ignoreTeam.get(), targetInvisibles.get());
    }

    /** Full charge is fixed: early attacks lower Minecraft's damage and reset its recovery. */
    public boolean isAttackReady() {
        return mc.player != null && mc.player.getAttackCooldownProgress(0f) >= 1f;
    }

    /** Physical critical-hit conditions; full weapon recovery is checked separately. */
    public boolean isCriticalReady() {
        return mc.player != null && !mc.player.isOnGround() && mc.player.fallDistance > 0
                && !mc.player.isClimbing() && !mc.player.isTouchingWater() && !mc.player.isInLava()
                && !mc.player.hasVehicle() && !mc.player.isSprinting()
                && !mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS);
    }

    private boolean waitingForHitConditions() {
        return !isAttackReady() || criticalOnly.get() && !isCriticalReady();
    }

    /** Central guard also covers manual clicks between two automatic attacks. */
    public boolean shouldBlockAttack(PlayerEntity player, Entity candidate) {
        return player == mc.player && operational() && waitingForHitConditions()
                && candidate instanceof LivingEntity living && managed(living);
    }

    /** Air swings consume the same charge; block mining is deliberately left to Minecraft. */
    public boolean shouldBlockManualAttack() {
        if (!operational() || mc.crosshairTarget == null) return false;
        if (mc.crosshairTarget.getType() == HitResult.Type.MISS) return !isAttackReady();
        return mc.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity living && managed(living) && waitingForHitConditions();
    }

    public LivingEntity getTarget() {
        return isState() && !nullCheck() && mc.currentScreen == null
                && mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() == target
                && CombatTargeting.valid(target, range.get(), playersOnly.get(), ignoreTeam.get(),targetInvisibles.get()) ? target : null;
    }

    private void reset() {
        target = null;
        readyAt = 0;
    }

    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }
}
