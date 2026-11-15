package elexis.modules.impl.movement;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.player.ContainerManager;
import elexis.client.utils.render.animations.impl.SimpleAlphaAnimation;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.ui.gui.ClickGui;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

@FunctionAdd(name = "GuiMove", alias = "GuiMove", category = Category.Movement, description = "Movement in inventory and containers")
@Environment(EnvType.CLIENT)
public class GuiMove extends Function {
    private final BooleanSetting inventory = new BooleanSetting("Inventory", true);
    private final BooleanSetting containers = new BooleanSetting("Containers", true);
    private final BooleanSetting jump = new BooleanSetting("Jump", true);
    private final BooleanSetting sneak = new BooleanSetting("Inventory", false);
    private SimpleAlphaAnimation onUpdate;

    public GuiMove() {
        this.addSettings(this.inventory, this.containers, this.jump, this.sneak);
    }

    @Override
    public void onDisable() {
        this.updateKey(mc.options.forwardKey, false);
        this.updateKey(mc.options.backKey, false);
        this.updateKey(mc.options.leftKey, false);
        this.updateKey(mc.options.rightKey, false);
        this.updateKey(mc.options.jumpKey, false);
        this.updateKey(mc.options.inventoryKey, false);
        super.onDisable();
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck() || mc.currentScreen == null || mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof AnvilScreen || mc.currentScreen instanceof ClickGui) {
            return;
        }
        if (!this.inventory.get() && ContainerManager.isMyInventory(mc.currentScreen)) {
            return;
        }
        if (!this.containers.get() && ContainerManager.isContainerScreen(mc.currentScreen) && !ContainerManager.isMyInventory(mc.currentScreen)) {
            return;
        }

        EventUpdate.Slow packet = sendPacket();
        this.onUpdate(packet != null && packet.isCancelled());

        this.updateKey(mc.options.forwardKey, true);
        this.updateKey(mc.options.backKey, true);
        this.updateKey(mc.options.leftKey, true);
        this.updateKey(mc.options.rightKey, true);
        this.updateKey(mc.options.sprintKey, true);
        this.updateKey(mc.options.jumpKey, this.jump.get());
        this.updateKey(mc.options.inventoryKey, this.sneak.get());
    }

    private void onUpdate(boolean b) {
    }


    private EventUpdate.Slow sendPacket() {
        return null;
    }

    private void updateKey(KeyBinding key, boolean allowed) {
        key.setPressed(allowed && InputUtil.isKeyPressed(mc.getWindow(), key.getDefaultKey().getCode()));
    }
}
