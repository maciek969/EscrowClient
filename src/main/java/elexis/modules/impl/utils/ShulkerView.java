package elexis.modules.impl.utils;

import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionAdd(name = "ShulkerView", alias = "ShulkerView", category = Category.Utilities, description = "Enables shulker content preview support")
@Environment(EnvType.CLIENT)
public class ShulkerView extends Function {
}
