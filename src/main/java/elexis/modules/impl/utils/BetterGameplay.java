package elexis.modules.impl.utils;

import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ModeListSetting;
import org.lwjgl.glfw.GLFW;

@FunctionAdd(name = "BetterGameplay", alias = "Better Gameplay", category = Category.Utilities)
public class BetterGameplay extends Function {
    public static ModeListSetting list = new ModeListSetting("What to optimize",
            new BooleanSetting("Crosshair", true),
            new BooleanSetting("Scroll", true));

    public BetterGameplay() {
        setBind(GLFW.GLFW_KEY_B);
    }
}