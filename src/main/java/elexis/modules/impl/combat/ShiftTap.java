package elexis.modules.impl.combat;

import elexis.client.events.impl.entity.EventAttack;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionAdd(name = "ShiftTap", alias = "ShiftTap", category = Category.Combat, description = "Sneaks for a short time after attack")
@Environment(EnvType.CLIENT)
public class ShiftTap extends Function {
    private final SliderSetting duration = new SliderSetting("Duration", 100.0F, 50.0F, 500.0F, 10.0F);
    private long endTime;
    private boolean controllingSneak;

    public ShiftTap() {
        addSettings(duration);
    }

    @EventHandler
    public void onAttack(EventAttack event) {
        if (mc.player == null || mc.player.isSpectator()) {
            return;
        }
        endTime = System.currentTimeMillis() + Math.round(duration.get());
        if (!controllingSneak) {
            mc.options.sneakKey.setPressed(true);
            controllingSneak = true;
        }
    }

    @EventHandler
    public void onUpdate(EventUpdate.Fast event) {
        if (mc.player == null || mc.player.isSpectator() || System.currentTimeMillis() > endTime) {
            stopSneak();
        }
    }

    @Override
    public void onDisable() {
        stopSneak();
        super.onDisable();
    }

    private void stopSneak() {
        if (controllingSneak) {
            mc.options.sneakKey.setPressed(false);
            controllingSneak = false;
        }
    }
}