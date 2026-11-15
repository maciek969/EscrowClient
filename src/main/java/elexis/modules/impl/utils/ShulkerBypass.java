package elexis.modules.impl.utils;

import elexis.client.events.impl.client.EventKey;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.lang.reflect.Field;

@FunctionAdd(name = "ShulkerBypass", alias = "ShulkerBypass", category = Category.Utilities, description = "Moves an item from a shulker by the hovered slot")
@Environment(EnvType.CLIENT)
public class ShulkerBypass extends Function {
    @EventHandler
    public void onKey(EventKey event) {
        if (mc.player == null || mc.interactionManager == null || event.getAction() != 1) {
            return;
        }
        if (!(mc.currentScreen instanceof ShulkerBoxScreen screen) || !(mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler)) {
            return;
        }
        Slot hoveredSlot = getSlotUnderMouse(screen);
        if (hoveredSlot == null || !hoveredSlot.hasStack()) {
            return;
        }
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, hoveredSlot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
    }

    private Slot getSlotUnderMouse(HandledScreen<?> screen) {
        double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
        int screenX = readInt(screen, "x");
        int screenY = readInt(screen, "y");
        for (Slot slot : screen.getScreenHandler().slots) {
            int x = screenX + slot.x;
            int y = screenY + slot.y;
            if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17) {
                return slot;
            }
        }
        return null;
    }

    private int readInt(Object owner, String name) {
        Class<?> type = owner.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.getInt(owner);
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return 0;
    }
}
