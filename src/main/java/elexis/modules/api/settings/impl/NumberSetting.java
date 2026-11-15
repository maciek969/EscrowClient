package elexis.modules.api.settings.impl;

import java.util.function.Supplier;

public class NumberSetting extends SliderSetting {
    public NumberSetting(String name, float defaultVal, float min, float max, float increment) {
        super(name, defaultVal, min, max, increment);
    }

    public NumberSetting(String name, float defaultVal, float min, float max, float increment, Supplier<Boolean> visible) {
        this(name, defaultVal, min, max, increment);
        setVisible(visible);
    }
}
