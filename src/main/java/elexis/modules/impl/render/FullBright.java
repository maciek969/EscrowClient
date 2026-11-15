package elexis.modules.impl.render;

import elexis.Elexis;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.SliderSetting;

@FunctionAdd(
        name = "FullBright",
        alias = "Full Bright",
        category = Category.Render,
        description = "Removes dark lighting"
)
public class FullBright extends Function {
    private final ModeSetting mode = new ModeSetting("Mode", "Normal", "Normal", "Adaptive");
    private final SliderSetting brightness = new SliderSetting("Brightness", 1f, 0.1f, 1f, 0.05f);
    public FullBright() { addSettings(mode, brightness); }

    public static float lightStrength(float original) {
        if (!isEnabledStatic()) return original;
        FullBright module = (FullBright) Elexis.getFunctionManager().getFunctionByName("FullBright");
        float amount = module.brightness.get();
        if (module.mode.is("Adaptive") && mc.world != null && mc.player != null) {
            float ambient = mc.world.getLightLevel(mc.player.getBlockPos()) / 15f;
            amount *= 1f - ambient * 0.8f;
        }
        return Math.max(original, amount);
    }
    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }

    public static boolean isEnabledStatic() {
        if (Elexis.getFunctionManager() == null) {
            return false;
        }
        Function function = Elexis.getFunctionManager().getFunctionByName("FullBright");
        return function != null && function.isState();
    }
}
