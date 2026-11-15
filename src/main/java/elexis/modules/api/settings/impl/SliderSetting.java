package elexis.modules.api.settings.impl;

import elexis.modules.api.settings.api.Setting;

import java.util.function.Supplier;

public class SliderSetting extends Setting<Float> {

    public float min;
    public float max;
    public float increment;
    private float fixedValue;

    public SliderSetting(String name, float defaultVal, float min, float max, float increment) {
        super(name, defaultVal);
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public SliderSetting(String name, float defaultVal, float min, float max, float increment, boolean ignored) {
        this(name, defaultVal, min, max, increment);
    }

    @Override
    public SliderSetting setVisible(Supplier<Boolean> bool) {
        return (SliderSetting) super.setVisible(bool);
    }

    @Override
    public SliderSetting hidden(Supplier<Boolean> bool) {
        return (SliderSetting) super.hidden(bool);
    }

    public void setFixedValue(Float value) {
        this.fixedValue = value;
    }

    public Float getFixedValue() {
        return this.fixedValue;
    }

    public Float getDefaultValue() {
        return defaultVal;
    }
}
