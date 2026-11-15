package elexis.modules.impl.combat;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;

@FunctionAdd(name = "AimBot", alias = "AimBot", category = Category.Combat,
        description = "Плавное наведение на игроков в пределах FOV")
public final class AimBot extends Function {
    private final SliderSetting range = new SliderSetting("Range", 3f, 1f, 6f, 0.1f);
    private final SliderSetting fov = new SliderSetting("FOV", 360f, 5f, 360f, 1f);
    private final SliderSetting speed = new SliderSetting("Speed (deg/s)", 90f, 10f, 360f, 5f);
    private final SliderSetting smoothing = new SliderSetting("Smoothing", 7f, 1f, 20f, 0.5f);
    private final ModeSetting activation = new ModeSetting("Activation", "Always", "Always", "Hold attack");
    private final BooleanSetting weaponOnly = new BooleanSetting("Require weapon", false);
    private final BooleanSetting playersOnly = new BooleanSetting("Players only", true);
    private final BooleanSetting targetInvisibles = new BooleanSetting("Target invisibles", true);
    private final BooleanSetting targetTeammates = new BooleanSetting("Target teammates", true);
    private final BooleanSetting vertical = new BooleanSetting("Vertical aim", true);
    private LivingEntity target;
    private long lastFrameNanos;
    private ClientPlayerEntity framePlayer;
    private ClientWorld frameWorld;
    private LivingEntity frameTarget;

    public AimBot() {
        addSettings(range, fov, speed, smoothing, activation, weaponOnly, playersOnly, targetInvisibles, targetTeammates, vertical);
    }

    @EventHandler
    public void onUpdate(EventUpdate.Fast event) {
        if (!canAim()) {
            target = null;
            resetFrames();
            return;
        }
        // Keep the selected target until it leaves the cone to avoid rapid target switching.
        if (!eligible(target)) {
            target = null;
            double best = Double.MAX_VALUE;
            for (var entity : mc.world.getEntities()) {
                if (!(entity instanceof LivingEntity living) || !eligible(living)) continue;
                double score = angularDistance(living);
                if (score < best) {
                    best = score;
                    target = living;
                }
            }
        }
        if (target == null) resetFrames();
    }

    /** Called once per rendered frame, after mouse input and before the native camera/crosshair. */
    public void onFrame() {
        if (!canAim() || target == null) {
            target = null;
            resetFrames();
            return;
        }
        long now = System.nanoTime();
        if (framePlayer != mc.player || frameWorld != mc.world || frameTarget != target || lastFrameNanos == 0) {
            framePlayer = mc.player;
            frameWorld = mc.world;
            frameTarget = target;
            lastFrameNanos = now;
            return;
        }
        double seconds = (now - lastFrameNanos) * 1.0e-9;
        lastFrameNanos = now;
        onFrame(seconds, mc.getRenderTickCounter().getTickProgress(false));
    }

    /** Explicit frame duration also permits deterministic checks at different frame rates. */
    public void onFrame(double seconds, float partialTicks) {
        if (!canAim() || target == null || !eligible(target)) {
            target = null;
            resetFrames();
            return;
        }
        if (!Double.isFinite(seconds) || seconds <= 0) return;
        // A stalled frame or a resumed window must not consume a large catch-up turn.
        seconds = Math.min(seconds, 0.05);
        partialTicks = Float.isFinite(partialTicks) ? MathHelper.clamp(partialTicks, 0f, 1f) : 1f;
        float[] angles = CombatTargeting.angles(CombatTargeting.aimPoint(target, partialTicks),
                mc.player.getCameraPosVec(partialTicks));
        float yawDelta = MathHelper.wrapDegrees(angles[0] - mc.player.getYaw());
        float pitchDelta = vertical.get() ? angles[1] - mc.player.getPitch() : 0f;
        double length = Math.hypot(yawDelta, pitchDelta);
        if (length < 0.0001) return;
        double step = AimSmoothing.step(length, speed.get(), smoothing.get(), seconds);
        // Use the same path as mouse input so previous angles stay consistent, including while riding.
        mc.player.changeLookDirection(yawDelta / length * step / 0.15,
                pitchDelta / length * step / 0.15);
    }

    private boolean canAim() {
        return isState() && !nullCheck() && !mc.isPaused() && mc.currentScreen == null
                && mc.player.isAlive() && !mc.player.isSpectator() && !mc.player.isUsingItem()
                && !CombatTargeting.anotherCombatModuleActive()
                && (!activation.is("Hold attack") || mc.options.attackKey.isPressed())
                && (!weaponOnly.get() || CombatTargeting.holdingWeapon());
    }

    private void resetFrames() {
        lastFrameNanos = 0;
        framePlayer = null;
        frameWorld = null;
        frameTarget = null;
    }

    private boolean eligible(LivingEntity living) {
        return CombatTargeting.valid(living, range.get(), playersOnly.get(), !targetTeammates.get(),targetInvisibles.get())
                && (fov.get() >= 360f || angularDistance(living) <= fov.get() * 0.5f);
    }

    private double angularDistance(LivingEntity living) {
        float[] angles = CombatTargeting.angles(CombatTargeting.aimPoint(living));
        return Math.hypot(MathHelper.wrapDegrees(angles[0] - mc.player.getYaw()), angles[1] - mc.player.getPitch());
    }

    public LivingEntity getTarget() {
        return isState() && !nullCheck() && mc.currentScreen == null && eligible(target) ? target : null;
    }

    @Override
    public void onDisable() {
        target = null;
        resetFrames();
        super.onDisable();
    }
}
