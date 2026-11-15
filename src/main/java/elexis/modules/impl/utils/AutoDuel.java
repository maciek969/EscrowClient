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
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@FunctionAdd(name = "AutoDuel", alias = "AutoDuel", category = Category.Utilities, description = "Automatically sends a duel and picks the mode")
@Environment(EnvType.CLIENT)
public class AutoDuel extends Function {
    private final SliderSetting delay = new SliderSetting("Delay", 25.0F, 5.0F, 80.0F, 1.0F);
    private int ticks;
    private int modeIndex;
    private boolean waitingMenu;

    public AutoDuel() {
        this.addSettings(this.delay);
    }

    @Override
    public void onDisable() {
        this.ticks = 0;
        this.modeIndex = 0;
        this.waitingMenu = false;
        super.onDisable();
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck() || mc.currentScreen instanceof ClickGui || ++this.ticks < this.delay.get()) {
            return;
        }
        this.ticks = 0;

        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler container) {
            if (this.clickConfirm(container)) {
                this.waitingMenu = false;
                return;
            }

            if (this.clickDuelMode(container)) {
                this.waitingMenu = true;
            }
            return;
        }

        if (this.waitingMenu) {
            return;
        }

        String name = this.getRandomPlayerName();
        if (name != null && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendChatMessage("/duel " + name);
            this.waitingMenu = true;
        }
    }

    private boolean clickDuelMode(GenericContainerScreenHandler container) {
        String[][] modes = {
                {"totem", "totem"},
                {"читерский рай", "cheat", "читер"},
                {"но дебаф", "no debuff", "nodebuff", "debuff"}
        };

        for (int step = 0; step < modes.length; step++) {
            String[] names = modes[(this.modeIndex + step) % modes.length];
            int slot = this.findSlotByName(container, names);
            if (slot != -1) {
                mc.interactionManager.clickSlot(container.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
                this.modeIndex = (this.modeIndex + step + 1) % modes.length;
                return true;
            }
        }
        return false;
    }

    private boolean clickConfirm(GenericContainerScreenHandler container) {
        int size = container.getInventory().size();
        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getInventory().getStack(i);
            if (stack.isOf(Items.LIME_STAINED_GLASS) || stack.isOf(Items.LIME_STAINED_GLASS_PANE) || stack.isOf(Items.GREEN_STAINED_GLASS) || stack.isOf(Items.GREEN_STAINED_GLASS_PANE)) {
                mc.interactionManager.clickSlot(container.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                return true;
            }
        }
        return false;
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

    private String getRandomPlayerName() {
        if (mc.getNetworkHandler() == null) {
            return null;
        }

        List<String> names = new ArrayList<>();
        String self = mc.player.getGameProfile().name();
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().name();
            if (name != null && !name.equalsIgnoreCase(self) && !name.isBlank()) {
                names.add(name);
            }
        }

        if (names.isEmpty()) {
            return null;
        }
        return names.get(ThreadLocalRandom.current().nextInt(names.size()));
    }
}
