package elexis.modules.impl.player;

import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionAdd(name = "ChestLoker", alias = "ChestLoker", category = Category.Player, description = "Chest reward preview")
@Environment(EnvType.CLIENT)
public final class ChestLoker extends Function {
    public final BooleanSetting showItems = new BooleanSetting("Show Items", true);
    public final BooleanSetting notifyRare = new BooleanSetting("Notify Rare", true);
    public final SliderSetting previewTime = new SliderSetting("Preview Time", 3.0F, 1.0F, 10.0F, 0.5F);

    public ChestLoker() {
        this.addSettings(showItems, notifyRare, previewTime);
    }
}