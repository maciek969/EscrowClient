package elexis.modules.impl.render;

import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.NumberSetting;

@FunctionAdd(name = "Cosmetics", alias = "Cosmetics", category = Category.Render, description = "Косметические визуальные элементы")
public final class Cosmetics extends Function {
    private final ModeSetting wingMode = new ModeSetting("Wings", "Classic", "Classic", "Butterfly");
    private final BooleanSetting halo = new BooleanSetting("Halo", false);
    private final NumberSetting scale = new NumberSetting("Scale", 1.0F, 0.5F, 2.0F, 0.05F);

    public Cosmetics() {
        addSettings(wingMode, halo, scale);
    }
}
