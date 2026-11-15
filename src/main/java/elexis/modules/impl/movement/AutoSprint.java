package elexis.modules.impl.movement;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.player.PlayerUtils;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.ModeSetting;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

@FunctionAdd(name = "AutoSprint", alias = "Auto Sprint", category = Category.Movement, description = "Автоматически бежит при движении")
public class AutoSprint extends Function {
    public static ModeSetting mode = new ModeSetting("Mode", "Обычный", "Обычный", "Пакетный", "Легитный");
    private boolean sentPacketSprint;
    private int legitWarmupTicks;

    public AutoSprint() {
        addSettings(mode);
        setState(true);
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck()) return;
        if (mc.player.isTouchingWater() || mc.player.isSubmergedInWater()) {
            this.sentPacketSprint = false;
            this.legitWarmupTicks = 0;
            return;
        }
        if (mode.is("Пакетный")) {
            this.handlePacketSprint();
            return;
        }
        if (mode.is("Легитный")) {
            this.handleLegitSprint();
            return;
        }
        setSprinting();
    }

    private void handlePacketSprint() {
        boolean canSprint = canSprint();
        mc.player.setSprinting(canSprint);
        if (mc.getNetworkHandler() == null) {
            return;
        }

        if (canSprint && !this.sentPacketSprint) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            this.sentPacketSprint = true;
        } else if (!canSprint && this.sentPacketSprint) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            this.sentPacketSprint = false;
        }
    }

    public static void setSprinting() {
        boolean canSprint = canSprint();
        mc.player.setSprinting(canSprint);
    }

    private void handleLegitSprint() {
        if (!canSprint()) {
            this.legitWarmupTicks = 0;
            mc.player.setSprinting(false);
            return;
        }

        if (this.legitWarmupTicks < 2) {
            this.legitWarmupTicks++;
            return;
        }
        mc.player.setSprinting(true);
    }

    private static boolean canSprint() {
        boolean hunger = mc.player.getHungerManager().getFoodLevel() > 6;
        boolean moving = PlayerUtils.isMovingForward();
        return hunger
                && moving
                && !mc.player.horizontalCollision
                && !mc.player.isSneaking()
                && !mc.player.isUsingItem()
                && !mc.player.isTouchingWater()
                && !mc.player.isInLava();
    }

    public static void unsetSprinting() {
        mc.player.setSprinting(false);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mc.player != null && !mc.player.isTouchingWater() && !mc.player.isSubmergedInWater()) {
            mc.player.setSprinting(false);
        }
        this.sentPacketSprint = false;
        this.legitWarmupTicks = 0;
        super.onDisable();
    }
}
