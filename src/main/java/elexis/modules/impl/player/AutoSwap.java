package elexis.modules.impl.player;

import elexis.client.events.impl.client.EventKey;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BindSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PlayerHeadItem;
import net.minecraft.screen.slot.SlotActionType;

@FunctionAdd(name = "AutoSwap", alias = "AutoSwap", category = Category.Combat, description = "Свап предметов в левой руке")
@Environment(EnvType.CLIENT)
public class AutoSwap extends Function {
    private final BindSetting swapBind = new BindSetting("Swap Bind", -1);
    private final ModeSetting firstItem = new ModeSetting("First Item", "Талик", "Талик", "Шары", "Щит");
    private final ModeSetting secondItem = new ModeSetting("Second Item", "Шары", "Талик", "Шары", "Щит");

    public AutoSwap() {
        this.addSettings(this.swapBind, this.firstItem, this.secondItem);
    }

    @EventHandler
    public void onKey(EventKey event) {
        if (nullCheck() || mc.currentScreen != null || !event.isKeyDown(this.swapBind.get())) {
            return;
        }

        String target = this.matches(mc.player.getOffHandStack(), this.firstItem.get()) ? this.secondItem.get() : this.firstItem.get();
        int slot = this.findSlot(target);
        if (slot == -1) {
            return;
        }

        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, this.toScreenSlot(slot), 40, SlotActionType.SWAP, mc.player);
    }

    private int findSlot(String mode) {
        for (int i = 0; i < 36; i++) {
            if (this.matches(mc.player.getInventory().getStack(i), mode)) {
                return i;
            }
        }
        return -1;
    }

    private boolean matches(ItemStack stack, String mode) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        if (mode.equalsIgnoreCase("Талик")) {
            return stack.isOf(Items.TOTEM_OF_UNDYING);
        }
        if (mode.equalsIgnoreCase("Шары")) {
            return stack.getItem() instanceof PlayerHeadItem;
        }
        if (mode.equalsIgnoreCase("Щит")) {
            return stack.isOf(Items.SHIELD);
        }

        return false;
    }

    private int toScreenSlot(int inventorySlot) {
        return inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
    }
}
