package elexis.modules.impl.utils;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.SliderSetting;
import elexis.ui.gui.ClickGui;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

@FunctionAdd(name = "AutoJoin", alias = "AutoJoin", category = Category.Utilities, description = "Автоматически открывает компас и заходит в дуэли")
@Environment(EnvType.CLIENT)
public class AutoJoin extends Function {
    private final SliderSetting delay = new SliderSetting("Delay", 10.0F, 2.0F, 40.0F, 1.0F);
    private int ticks;

    public AutoJoin() {
        this.addSettings(this.delay);
    }

    @Override
    public void onDisable() {
        this.ticks = 0;
        super.onDisable();
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck() || mc.currentScreen instanceof ClickGui || ++this.ticks < this.delay.get()) {
            return;
        }
        this.ticks = 0;

        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler container) {
            int anchorSlot = this.findItemSlot(container, Items.RESPAWN_ANCHOR);
            if (anchorSlot != -1) {
                mc.interactionManager.clickSlot(container.syncId, anchorSlot, 0, SlotActionType.PICKUP, mc.player);
                return;
            }

            int duelSlot = this.findSlotByName(container, "дуели 1.16.5", "дуели", "duels", "duel");
            if (duelSlot != -1) {
                mc.interactionManager.clickSlot(container.syncId, duelSlot, 0, SlotActionType.PICKUP, mc.player);
            }
            return;
        }

        if (mc.player.getMainHandStack().isOf(Items.COMPASS)) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            return;
        }

        int compassSlot = this.findHotbarCompass();
        if (compassSlot != -1) {
            mc.player.getInventory().setSelectedSlot(compassSlot);
        }
    }

    private int findHotbarCompass() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.COMPASS)) {
                return i;
            }
        }
        return -1;
    }

    private int findItemSlot(GenericContainerScreenHandler container, net.minecraft.item.Item item) {
        int size = container.getInventory().size();
        for (int i = 0; i < size; i++) {
            if (container.getInventory().getStack(i).isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    private int findSlotByName(GenericContainerScreenHandler container, String... names) {
        int size = container.getInventory().size();
        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getInventory().getStack(i);
            if (stack.isEmpty()) {
                continue;
            }

            String name = stack.getName().getString().toLowerCase();
            for (String target : names) {
                if (name.contains(target)) {
                    return i;
                }
            }
        }
        return -1;
    }
}
