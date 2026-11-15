package elexis.modules.impl.utils;

import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import lombok.Getter;

@FunctionAdd(name = "UnHook", alias = "Un Hook", category = Category.Utilities)
public class UnHook extends Function {
    public static boolean unhooked = false;
    @Getter private static int code = 1;

    @Override
    public void onEnable() {
        unhooked = true;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        unhooked = false;
        super.onDisable();
    }
}

