package elexis.modules.impl.render;

import elexis.Elexis;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;

@FunctionAdd(
        name = "Item Physic",
        alias = "ItemPhysic",
        category = Category.Render,
        description = "Adds physics to dropped items"
)
public class ItemPhysic extends Function {
    public static boolean isEnabledStatic() {
        return Elexis.getFunctionManager() != null
                && Elexis.getFunctionManager().getItemPhysic() != null
                && Elexis.getFunctionManager().getItemPhysic().isState();
    }
}