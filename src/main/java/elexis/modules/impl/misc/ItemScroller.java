package elexis.modules.impl.misc;

import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.SliderSetting;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

@FunctionAdd(name = "ItemScroller", alias = "ItemScroller", category = Category.Player,
        description = "Перенос стаков через Shift + ЛКМ и выброс одного типа через Ctrl + Shift + Q")
public final class ItemScroller extends Function {
    private final SliderSetting delay = new SliderSetting("Delay", 80f, 0f, 500f, 1f);
    private Slot previous;
    private HandledScreen<?> screen;
    private long nextMove;
    private boolean bulkDrop;

    public ItemScroller() { addSettings(delay); }

    public void drag(HandledScreen<?> current, Slot slot) {
        if (screen != current) {
            screen = current;
            previous = null;
            nextMove = 0;
        }
        if (slot == null || !slot.hasStack() || slot == previous || mc.player == null
                || mc.interactionManager == null || current.getScreenHandler() != mc.player.currentScreenHandler
                || !current.getScreenHandler().getCursorStack().isEmpty() || !shift()) return;
        long now = System.nanoTime();
        if (now < nextMove) return;
        previous = slot;
        nextMove = now + (long)(delay.get() * 1_000_000L);
        mc.interactionManager.clickSlot(current.getScreenHandler().syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
    }

    public void endDrag() { previous = null; screen = null; }

    public boolean dropMatching(int syncId, int slotId, SlotActionType action) {
        if (bulkDrop || action != SlotActionType.THROW || !shift() || !control() || mc.player == null
                || mc.player.isCreative() || !(mc.currentScreen instanceof HandledScreen<?>)) return false;
        var handler = mc.player.currentScreenHandler;
        if (handler.syncId != syncId || slotId < 0 || slotId >= handler.slots.size() || !handler.getCursorStack().isEmpty()) return false;
        Slot source = handler.getSlot(slotId);
        if (!source.hasStack() || !source.canTakeItems(mc.player)) return false;
        var item = source.getStack().getItem();
        bulkDrop = true;
        try {
            for (Slot slot : handler.slots) {
                if (slot.hasStack() && slot.getStack().isOf(item) && slot.canTakeItems(mc.player)) {
                    mc.interactionManager.clickSlot(syncId, slot.id, 1, SlotActionType.THROW, mc.player);
                }
            }
        } finally { bulkDrop = false; }
        return true;
    }

    private static boolean shift() { return down(GLFW.GLFW_KEY_LEFT_SHIFT) || down(GLFW.GLFW_KEY_RIGHT_SHIFT); }
    private static boolean control() { return down(GLFW.GLFW_KEY_LEFT_CONTROL) || down(GLFW.GLFW_KEY_RIGHT_CONTROL); }
    private static boolean down(int key) { return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS; }
    @Override public void onDisable() { endDrag(); super.onDisable(); }
}
