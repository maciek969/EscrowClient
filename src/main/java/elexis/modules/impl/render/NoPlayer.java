package elexis.modules.impl.render;

import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionAdd(name = "NoPlayer", alias = "NoPlayer", category = Category.Render, description = "Lightweight player rendering")
@Environment(EnvType.CLIENT)
public final class NoPlayer extends Function {
    public final BooleanSetting hideShadows = new BooleanSetting("Hide Shadows", true);
    public final BooleanSetting hideGlowing = new BooleanSetting("Hide Glowing", true);
    public final BooleanSetting simplePlayers = new BooleanSetting("Simple Players", false);

    public NoPlayer() {
        this.addSettings(hideShadows, hideGlowing, simplePlayers);
    }
}