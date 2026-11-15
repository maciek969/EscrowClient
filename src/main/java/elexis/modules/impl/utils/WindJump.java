package elexis.modules.impl.utils;

import elexis.client.events.impl.client.EventKey;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.player.PlayerInventoryUtil;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BindSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Items;

@FunctionAdd(name = "WindJump", alias = "WindJump", category = Category.Utilities, description = "Uses a wind charge on bind")
@Environment(EnvType.CLIENT)
public class WindJump extends Function {
    private final BindSetting windChargeBind = new BindSetting("Wind charge", -1);

    public WindJump() {
        addSettings(windChargeBind);
    }

    @EventHandler
    public void onKey(EventKey event) {
        if (nullCheck() || !event.isKeyDown(windChargeBind.get(), true)) {
            return;
        }
        PlayerInventoryUtil.swapAndUse(Items.WIND_CHARGE, false);
    }
}
