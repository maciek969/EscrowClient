package elexis.modules.impl.player;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

@FunctionAdd(name = "ChestStealer", alias = "ChestStealer", category = Category.Player, description = "Takes items from a chest with a delay")
@Environment(EnvType.CLIENT)
public class ChestStealer extends Function {
    private final SliderSetting delay = new SliderSetting("Delay", 4.0F, 1.0F, 20.0F, 1.0F);
    private int ticks;

    public ChestStealer() {
        this.addSettings(this.delay);
    }

    @Override
    public void onDisable() {
        this.ticks = 0;
        super.onDisable();
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck() || !(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler container)) {
            this.ticks = 0;
            return;
        }

        if (++this.ticks < this.delay.get()) {
            return;
        }
        this.ticks = 0;

        int slot = this.findNextContainerSlot(container);
        if (slot != -1) {
            mc.interactionManager.clickSlot(container.syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
        }
    }

    private int findNextContainerSlot(GenericContainerScreenHandler container) {
        int size = container.getInventory().size();
        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                return i;
            }
        }
        return -1;
    }
}
