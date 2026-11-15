package elexis.modules.impl.player;

import elexis.client.events.impl.client.EventKey;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.player.PlayerUtils;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BindSetting;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@FunctionAdd(name = "Elytra Helper", alias = "Elytra Swap", description = "Помощник для элитры", category = Category.Player)
public class ElytraSwap extends Function {
    public static final ElytraSwap INSTANCE = new ElytraSwap();

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Grim");
    private final BindSetting swapKey = new BindSetting("Swap key", -1);
    private final BindSetting fireworkKey = new BindSetting("Firework key", -1);
    private final BooleanSetting autoTakeoff = new BooleanSetting("Auto takeoff", true);

    private boolean fireworkUsed;
    private boolean swapped;
    private int takeoffDelay;

    public ElytraSwap() {
        addSettings(mode, swapKey, fireworkKey, autoTakeoff);
    }

    @Override
    public void onDisable() {
        takeoffDelay = 0;
        super.onDisable();
    }

    @EventHandler
    public void onKey(EventKey event) {
        if (nullCheck() || event.getAction() == 0) {
            return;
        }
        if (event.matchesBind(swapKey.getKey())) {
            swapped = true;
        }
        if (event.matchesBind(fireworkKey.getKey()) && mc.player.isGliding()) {
            fireworkUsed = true;
        }
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck()) {
            return;
        }

        if (swapped) {
            swapped = false;
            swap(mode.get(), mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA));
        }

        handleAutoTakeoff();

        if (fireworkUsed) {
            useFireworkFromHotbar();
            fireworkUsed = false;
        }
    }

    public void swap(ModeSetting mode, boolean chestplate) {
        swap(mode.get(), chestplate);
    }

    public void swap(String mode, boolean chestplate) {
        vanillaSwap(chestplate);
    }

    private void vanillaSwap(boolean wearingElytra) {
        int slot = wearingElytra ? findBestChestplateSlot() : findBestElytraSlot();

        if (slot == -1) {
            String info = wearingElytra ? "Chestplate" : "Elytra";
            if (mc.player != null) {
                mc.player.sendMessage(Text.of("Elexis " + Formatting.DARK_GRAY + "-> " + Formatting.RESET + info + " not found"), false);
            }
            return;
        }

        if (slot >= 0 && slot <= 8) {
            mc.interactionManager.clickSlot(0, 6, slot, SlotActionType.SWAP, mc.player);
        } else if (slot >= 9 && slot <= 35) {
            int screenSlot = slot;
            mc.interactionManager.clickSlot(0, screenSlot, 8, SlotActionType.SWAP, mc.player);
            mc.interactionManager.clickSlot(0, 6, 8, SlotActionType.SWAP, mc.player);
            mc.interactionManager.clickSlot(0, screenSlot, 8, SlotActionType.SWAP, mc.player);
        }
    }

    private void handleAutoTakeoff() {
        if (!autoTakeoff.isEnabled()) {
            return;
        }

        var chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (!chest.isOf(Items.ELYTRA)
                || mc.player.isInLava()
                || mc.player.isTouchingWater()
                || mc.player.hasVehicle()
                || mc.player.isGliding()
                || mc.player.isSpectator()) {
            return;
        }

        if (mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) {
            mc.player.jump();
            takeoffDelay = 4;
            return;
        }

        if (!mc.player.isOnGround() && takeoffDelay > 0 && --takeoffDelay == 0) {
            sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.startGliding();
        }
    }

    private void useFireworkFromHotbar() {
        if (mc.player == null || mc.interactionManager == null || mc.getNetworkHandler() == null) {
            return;
        }

        int slotId = findInventorySlot(Items.FIREWORK_ROCKET);
        if (slotId == -1) {
            return;
        }

        int previousSlot = mc.player.getInventory().getSelectedSlot();
        boolean hotbarSlot = slotId < 9;
        int selectedSlot = hotbarSlot ? slotId : previousSlot;

        if (!hotbarSlot) {
            mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId,
                    toScreenSlot(slotId),
                    selectedSlot,
                    SlotActionType.SWAP,
                    mc.player
            );
        }

        if (previousSlot != selectedSlot) {
            mc.player.getInventory().setSelectedSlot(selectedSlot);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(selectedSlot));
        }

        PlayerUtils.interactItem(net.minecraft.util.Hand.MAIN_HAND);

        if (!hotbarSlot) {
            mc.interactionManager.clickSlot(
                    mc.player.playerScreenHandler.syncId,
                    toScreenSlot(slotId),
                    selectedSlot,
                    SlotActionType.SWAP,
                    mc.player
            );
        }

        if (previousSlot != selectedSlot) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
        }
    }

    private int findBestElytraSlot() {
        return findInventorySlot(Items.ELYTRA);
    }

    private int findBestChestplateSlot() {
        for (int i = 0; i < 36; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if ((stack.isOf(Items.LEATHER_CHESTPLATE)
                    || stack.isOf(Items.CHAINMAIL_CHESTPLATE)
                    || stack.isOf(Items.IRON_CHESTPLATE)
                    || stack.isOf(Items.GOLDEN_CHESTPLATE)
                    || stack.isOf(Items.DIAMOND_CHESTPLATE)
                    || stack.isOf(Items.NETHERITE_CHESTPLATE))
                    && !stack.isOf(Items.ELYTRA)) {
                return i;
            }
        }
        return -1;
    }

    private int findInventorySlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    private int toScreenSlot(int inventorySlot) {
        return inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
    }
}
