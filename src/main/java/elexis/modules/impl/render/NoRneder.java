package elexis.modules.impl.render;

import elexis.Elexis;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ModeListSetting;

@FunctionAdd(name = "NoRender", alias = "No Render", category = Category.Render)
public class NoRneder extends Function {

    public static final ModeListSetting modes = new ModeListSetting("Modes",
            new BooleanSetting("Fire", false),
            new BooleanSetting("Bad effects", false),
            new BooleanSetting("Block overlay", false),
            new BooleanSetting("Screen shake", false),
            new BooleanSetting("Totem animation", false),
            new BooleanSetting("Boss bar", false),
            new BooleanSetting("Shadows", false),
            new BooleanSetting("Smoke", false),
            new BooleanSetting("Vignette", false),
            new BooleanSetting("Arrows in player", false),
            new BooleanSetting("Player glow", false),
            new BooleanSetting("Lava overlay", false),
            new BooleanSetting("Water overlay", false),
            new BooleanSetting("Weather", false),
            new BooleanSetting("Scoreboard", false),
            new BooleanSetting("Wither hearts", false),
            new BooleanSetting("Air bubbles", false),
            new BooleanSetting("Armor stands", false),
            new BooleanSetting("Friends", false),
            new BooleanSetting("Titles", false),
            new BooleanSetting("Vegetation", false)
    );

    public NoRneder() {
        addSettings(modes);
    }

    public static boolean is(String modeName) {
        elexis.modules.api.FunctionManager fm = Elexis.getFunctionManager();
        if (fm == null) return false;
        NoRneder mod = (NoRneder) fm.getFunctionByName("NoRender");
        return mod != null && mod.isState() && modes.isSelected(modeName);
    }
}
