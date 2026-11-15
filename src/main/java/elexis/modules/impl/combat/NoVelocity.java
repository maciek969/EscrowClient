package elexis.modules.impl.combat;

import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.ModeSetting;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

@FunctionAdd(name = "NoVelocity", alias = "NoVelocity", category = Category.Combat, description = "Removes knockback")
public final class NoVelocity extends Function {
    private final ModeSetting mode = new ModeSetting("Mode", "Cancel", "Cancel", "Vanilla", "Grim", "Grim Air", "GrimClassic");
    private int skipTicks;
    private boolean classicToggle;

    public NoVelocity() {
        addSettings(mode);
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
            // hand-written: drop every other knockback packet while grounded so
            // Grim still sees periodic velocity applied (no "no knockback" flag)
            // but the net push the player takes is roughly halved
            if (mc.player.isOnGround()) {
                classicToggle = !classicToggle;
                if (classicToggle) {
                    event.cancel();
                }
            }
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

    @Override
    public void onEnable() {
        skipTicks = 0;
        classicToggle = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        skipTicks = 0;
        classicToggle = false;
        super.onDisable();
    }
}
