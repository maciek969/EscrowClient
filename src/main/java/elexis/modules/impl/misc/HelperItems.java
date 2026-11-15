package elexis.modules.impl.misc;

import elexis.client.utils.client.IMinecraft;
import elexis.modules.impl.combat.javelin.JavelinCombat;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

final class HelperItems implements IMinecraft {
    private HelperItems() {}

    static int find(Item item, String name, boolean customOnly) {
        return JavelinCombat.find(stack -> {
            if (!stack.isOf(item)) return false;
            var text = customOnly ? stack.getCustomName() : stack.getName();
            return text != null && text.getString().equalsIgnoreCase(name);
        }, 0, 36);
    }

    static void use(int slot) {
        if (slot < 0 || !JavelinCombat.ready() || mc.currentScreen != null) return;
        var inventory = mc.player.getInventory();
        ItemStack stack = inventory.getStack(slot);
        if (stack.isEmpty() || mc.player.getItemCooldownManager().isCoolingDown(stack)) return;
        int selected = inventory.getSelectedSlot();
        boolean inventorySwap = slot > 8;
        if (inventorySwap && !JavelinCombat.swapInventory(slot, selected)) return;
        if (!inventorySwap && selected != slot) JavelinCombat.select(slot);
        try {
            var result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            if (result.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);
        } finally {
            if (inventorySwap) JavelinCombat.swapInventory(slot, selected);
            else if (slot != selected) JavelinCombat.select(selected);
        }
    }

    static boolean serverContains(String value) {
        var handler = mc.getNetworkHandler();
        if (handler == null) return false;
        var entry = mc.getCurrentServerEntry();
        String brand = handler.getBrand();
        return (entry != null && entry.address.toLowerCase(java.util.Locale.ROOT).contains(value))
                || (brand != null && brand.toLowerCase(java.util.Locale.ROOT).contains(value));
    }
}
