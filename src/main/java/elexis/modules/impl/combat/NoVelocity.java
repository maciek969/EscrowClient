package elexis.modules.impl.combat;

import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

@FunctionAdd(name = "NoVelocity", alias = "NoVelocity", category = Category.Combat, description = "Removes knockback")
public final class NoVelocity extends Function {
    private final ModeSetting mode = new ModeSetting("Mode", "Cancel", "Cancel", "Vanilla", "Grim", "Grim Air", "GrimClassic");
    // GrimClassic keeps a fraction of the knockback. Grim's KnockbackHandler knows the exact
    // velocity it sent and flags on the movement offset: an immediate setback at offset >= 0.1
    // and an advantage that accumulates to 1.0 otherwise. A partial reduction keeps the per-tick
    // offset under that immediate threshold, unlike dropping whole packets.
    private final SliderSetting classicHorizontal = new SliderSetting("Horizontal reduction %", 15F, 0F, 100F, 1F)
            .hidden(() -> !mode.is("GrimClassic"));
    private final SliderSetting classicVertical = new SliderSetting("Vertical reduction %", 15F, 0F, 100F, 1F)
            .hidden(() -> !mode.is("GrimClassic"));
    private int skipTicks;

    public NoVelocity() {
        addSettings(mode, classicHorizontal, classicVertical);
    }

    @EventHandler
    public void onPacket(EventReceivePacket event) {
        if (nullCheck()) {
            return;
        }

        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            skipTicks = 8;
            return;
        }

        if (!(event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) || packet.getEntityId() != mc.player.getId()) {
            return;
        }

        if (mode.is("Cancel")) {
            event.cancel();
            return;
        }

        if (mode.is("Vanilla")) {
            return;
        }

        if (mode.is("Grim")) {
            if (skipTicks > 0) {
                skipTicks--;
                return;
            }
            event.cancel();
            skipTicks = 2;
            return;
        }

        if (mode.is("GrimClassic")) {
            // hand-written: take the knockback at a reduced magnitude instead of dropping it.
            // Grim reads the velocity it sent, so a cancelled packet leaves the full vector
            // "owed" and flags; applying a fraction keeps the resulting offset small.
            Vec3d kb = packet.getVelocity();
            double keepH = 1.0 - clampPercent(classicHorizontal.get()) / 100.0;
            double keepV = 1.0 - clampPercent(classicVertical.get()) / 100.0;
            event.cancel();
            mc.player.setVelocity(kb.x * keepH, kb.y * keepV, kb.z * keepH);
            return;
        }

        if (mode.is("Grim Air")
                && !mc.player.isOnGround()
                && !mc.player.isGliding()
                && !mc.player.isTouchingWater()
                && !mc.player.isInLava()
                && mc.player.fallDistance > 0.65F) {
            event.cancel();
        }
    }

    private static float clampPercent(float value) {
        return Math.max(0F, Math.min(100F, value));
    }

    @Override
    public void onEnable() {
        skipTicks = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        skipTicks = 0;
        super.onDisable();
    }
}
