package elexis.modules.impl.render;

import elexis.Elexis;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.events.impl.render.EventRender;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ColorSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;

@FunctionAdd(
        name = "CustomWorld",
        alias = "Custom World",
        category = Category.Render,
        description = "Changes time, weather and fog in the world"
)
public class CustomWorld extends Function {
    public static final ModeSetting timeMode = new ModeSetting("Time", "Day", "Day", "Midnight", "Night", "Custom", "None");
    public static final SliderSetting customTime = new SliderSetting("Custom Time", 1000f, 0f, 24000f, 100f)
            .hidden(() -> !timeMode.is("Custom"));

    public static final ModeSetting weatherMode = new ModeSetting("Weather", "Sunny", "Sunny", "Rainy", "Thunder", "Snowy", "Custom", "None");
    public static final SliderSetting customRainGradient = new SliderSetting("Custom Rain", 0f, 0f, 1f, 0.1f)
            .hidden(() -> !weatherMode.is("Custom"));
    public static final SliderSetting customThunderGradient = new SliderSetting("Custom Thunder", 0f, 0f, 1f, 0.1f)
            .hidden(() -> !weatherMode.is("Custom"));

    public static final BooleanSetting useFog = new BooleanSetting("Fog", true);
    public static final ModeSetting fogColorMode = new ModeSetting("Fog Color Mode", "Custom", "Custom", "Vanilla");
    public static final ColorSetting fogColor = new ColorSetting("Fog Color", new Color(255, 102, 214, 255).getRGB());
    public static final SliderSetting fogDistance = new SliderSetting("Fog Distance", 1f, 0.05f, 1f, 0.01f)
            .hidden(() -> !useFog.get());

    private float animatedTime = 1000f;

    public CustomWorld() {
        fogColor.setVisible(() -> fogColorMode.is("Custom"));
        addSettings(timeMode, customTime, weatherMode, customRainGradient, customThunderGradient, useFog, fogColorMode, fogColor, fogDistance);
    }

    @EventHandler
    public void onRender(EventRender.World event) {
        float target = resolveTimeValue();
        animatedTime = MathHelper.lerp(0.08f, animatedTime, target);
    }

    public long getTime(long original) {
        if (!isState() || timeMode.is("None")) {
            return original;
        }
        if (timeMode.is("Custom")) {
            return (long) customTime.get().floatValue();
        }
        return (long) animatedTime;
    }

    public float getRainGradient() {
        if (!isState() || weatherMode.is("None")) {
            return -1f;
        }
        if (weatherMode.is("Custom")) {
            return customRainGradient.get();
        }
        return switch (weatherMode.get()) {
            case "Rainy" -> 1f;
            case "Thunder" -> 1f;
            case "Snowy" -> 0.9f;
            default -> 0f;
        };
    }

    public float getThunderGradient() {
        if (!isState() || weatherMode.is("None")) {
            return -1f;
        }
        if (weatherMode.is("Custom")) {
            return customThunderGradient.get();
        }
        return weatherMode.is("Thunder") ? 1f : 0f;
    }

    public static long getCustomTime() {
        CustomWorld module = getInstance();
        if (module == null) {
            return 1000L;
        }
        return module.getTime(1000L);
    }

    public static CustomWorld getInstance() {
        if (Elexis.getFunctionManager() == null) {
            return null;
        }
        return (CustomWorld) Elexis.getFunctionManager().getFunctionByName("CustomWorld");
    }

    private float resolveTimeValue() {
        return switch (timeMode.get()) {
            case "Midnight" -> 18000f;
            case "Night" -> 12300f;
            case "Custom" -> customTime.get();
            case "None" -> animatedTime;
            default -> 1000f;
        };
    }
}
