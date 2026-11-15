package elexis.modules.api.settings.impl;

import java.util.function.Supplier;

public class MultiBooleanSetting extends ModeListSetting {
    public MultiBooleanSetting(String name, BooleanSetting... strings) {
        super(name, strings);
    }

    public boolean get(String settingName) {
        return isSelected(settingName);
    }

    @Override
    public MultiBooleanSetting setVisible(Supplier<Boolean> bool) {
        return (MultiBooleanSetting) super.setVisible(bool);
    }
}
