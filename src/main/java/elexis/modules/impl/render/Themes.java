package elexis.modules.impl.render;

import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.ColorSetting;
import elexis.modules.api.settings.impl.ModeSetting;

import java.awt.Color;

@FunctionAdd(
        name = "Themes",
        alias = "Themes",
        category = Category.Render,
        description = "Selects client theme colors for visuals"
)
public class Themes extends Function {
    public static final ModeSetting theme = new ModeSetting(
            "Theme",
            "Lavender",
            "Rainbow",
            "Lavender",
            "Sky",
            "Blood",
            "Ocean",
            "Emerald",
            "Rose",
            "Gold",
            "Diamond",
            "Sun",
            "Custom"
    );
    public static final ColorSetting customColor = new ColorSetting("Custom Color", new Color(190, 143, 255, 255).getRGB());

    public Themes() {
        customColor.setVisible(() -> theme.is("Custom"));
        addSettings(theme, customColor);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.worldRenderer != null) {
            mc.worldRenderer.scheduleTerrainUpdate();
        }
    }
}
