package elexis.modules.impl.movement;

import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.ModeSetting;

@FunctionAdd(name = "NoWeb", alias = "No Web", category = Category.Movement, description = "Removes slowdown in cobweb")
public class NoWeb extends Function {
    private final ModeSetting mode = new ModeSetting("Mode", "Default", "Default", "Grim Test", "GrimClassic");

    public NoWeb() {
        addSettings(mode);
    }

    public boolean isGrimTest() {
        return mode.is("Grim Test");
    }

    public boolean isGrimClassic() {
        return mode.is("GrimClassic");
    }
}
