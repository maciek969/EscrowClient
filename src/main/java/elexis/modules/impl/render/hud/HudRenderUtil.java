package elexis.modules.impl.render.hud;

import elexis.Elexis;
import elexis.client.utils.render.main.newrender.text.FontObject;
import elexis.client.utils.render.main.newrender.text.FontRegistry;
import elexis.modules.impl.combat.HitAura;
import elexis.client.events.impl.entity.EventAttack;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.client.IMinecraft;
import net.minecraft.entity.LivingEntity;

import java.util.Locale;

public final class HudRenderUtil {
    private static LivingEntity lastAttacked;
    private static long lastAttackTime;
    private static final Object TARGET_TRACKER = new Object() {
        @EventHandler
        public void onAttack(EventAttack event) {
            if (event.getTarget() instanceof LivingEntity living && living != IMinecraft.mc.player) {
                lastAttacked = living;
                lastAttackTime = System.nanoTime();
            }
        }
    };

    public static void registerTargetTracker() {
        Elexis.getEventBus().subscribe(TARGET_TRACKER);
    }
    public static final float BASE_WIDTH = 1920f;
    public static final float BASE_HEIGHT = 1080f;

    private HudRenderUtil() {
    }

    public static float centeredBaseline(FontObject font, char sample, float size, float centerY) {
        return centerY + FontRegistry.centeredBaselineOffset(font, sample, size);
    }

    public static String format1(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    public static String trim(String text, int maxChars) {
        return text.length() <= maxChars ? text : text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    public static String formatDuration(int ticks) {
        int seconds = Math.max(0, ticks / 20);
        int minutes = seconds / 60;
        int remain = seconds % 60;
        return String.format(Locale.US, "%d:%02d", minutes, remain);
    }

    public static float sx(int viewportWidth, float x) {
        return x * viewportWidth / BASE_WIDTH;
    }

    public static float sy(int viewportHeight, float y) {
        return y * viewportHeight / BASE_HEIGHT;
    }

    public static LivingEntity currentHudTarget() {
        if (IMinecraft.mc.world == null || IMinecraft.mc.player == null) {
            lastAttacked = null;
            return null;
        }
        var manager = Elexis.getFunctionManager();
        if (manager == null) return null;
        HitAura hitAura = manager.getHitAura();
        if (hitAura != null && hitAura.isState() && HitAura.target instanceof LivingEntity living && validHudTarget(living)) return living;
        if (manager.getAimBot() != null && validHudTarget(manager.getAimBot().getTarget())) return manager.getAimBot().getTarget();
        if (manager.getTriggerBot() != null && validHudTarget(manager.getTriggerBot().getTarget())) return manager.getTriggerBot().getTarget();
        if (System.nanoTime() - lastAttackTime > 2_500_000_000L || !validHudTarget(lastAttacked)) lastAttacked = null;
        return lastAttacked;
    }

    private static boolean validHudTarget(LivingEntity target) {
        return target != null && target.getEntityWorld() == IMinecraft.mc.world && target.isAlive() && !target.isRemoved();
    }
}
