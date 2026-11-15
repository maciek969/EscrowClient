package elexis.modules.impl.movement;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.ModeSetting;
import net.minecraft.util.math.Vec3d;

@FunctionAdd(name = "GrimGlide", alias = "Grim Glide", category = Category.Movement, description = "Stabilizes elytra flight")
public class GrimGlide extends Function {
    private final ModeSetting mode = new ModeSetting("Mode", "Default", "Default", "GrimClassic");

    public GrimGlide() {
        addSettings(mode);
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck() || !mc.player.isGliding()) {
            return;
        }

        if (mode.is("GrimClassic")) {
            handleGrimClassic();
            return;
        }

        if (mc.player.getVelocity().y < -0.04D) {
            mc.player.setVelocity(mc.player.getVelocity().x, -0.04D, mc.player.getVelocity().z);
        }
    }

    // ── GrimClassic: hand-written pitch-aware glide hold ───────────────────
    // Instead of a hard vertical clamp, blend the current fall speed toward a
    // gentle descent that scales with look pitch. Looking level holds height,
    // looking down allows a controlled dive, which matches the elytra physics
    // Grim predicts rather than flat-lining Y every tick.
    private void handleGrimClassic() {
        Vec3d velocity = mc.player.getVelocity();
        float pitch = mc.player.getPitch();
        double targetFall = pitch > 0.0F ? -0.04D - (pitch / 90.0D) * 0.35D : -0.02D;
        double blended = velocity.y + (targetFall - velocity.y) * 0.25D;
        if (blended < velocity.y) {
            blended = velocity.y;
        }
        mc.player.setVelocity(velocity.x, Math.max(blended, targetFall), velocity.z);
    }
}
