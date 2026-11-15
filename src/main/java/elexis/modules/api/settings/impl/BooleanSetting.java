package elexis.modules.api.settings.impl;

import elexis.modules.api.settings.api.Setting;

import java.util.function.Supplier;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, Boolean defaultVal) {
        super(name, defaultVal);
    }

    public boolean isEnabled() {
        return get();
    }

    @Override
    public BooleanSetting setVisible(Supplier<Boolean> bool) {
        return (BooleanSetting) super.setVisible(bool);
    }

    @Override
    public BooleanSetting hidden(Supplier<Boolean> bool) {
        return (BooleanSetting) super.hidden(bool);
    }

    public boolean getValue() {
        return false;
    }
}
