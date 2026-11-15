package elexis.modules.impl.render;

import elexis.Elexis;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;

@FunctionAdd(
        name = "Baby Boy",
        alias = "BabyBoy",
        category = Category.Render,
        description = "Makes your player model small"
)
public class BabyBoy extends Function {
    public static boolean child;

    @Override
    public void onEnable() {
        super.onEnable();
        child = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        child = false;
    }

    public static boolean shouldScalePlayer(int entityId) {
        if (Elexis.getFunctionManager() == null || Elexis.getFunctionManager().getBabyBoy() == null || mc.player == null) {
            return false;
        }
        return Elexis.getFunctionManager().getBabyBoy().isState() && mc.player.getId() == entityId;
    }
}