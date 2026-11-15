package elexis.modules.impl.render;

import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionAdd(name = "SeeInvisible", alias = "SeeInvisible", category = Category.Render, description = "Makes invisible entities easier to see")
@Environment(EnvType.CLIENT)
public class SeeInvisible extends Function {
    private final SliderSetting alpha = new SliderSetting("Alpha", 0.5F, 0.1F, 1.0F, 0.05F);

    public SeeInvisible() {
        addSettings(alpha);
    }

    public float getAlpha() {
        return alpha.get();
    }
}
