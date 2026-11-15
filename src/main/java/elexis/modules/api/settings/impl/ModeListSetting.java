package elexis.modules.api.settings.impl;

import elexis.modules.api.settings.api.Setting;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class ModeListSetting extends Setting<List<BooleanSetting>> {
    public ModeListSetting(String name, BooleanSetting... strings) {
        super(name, Arrays.asList(strings));
    }

    public BooleanSetting getByName(String settingName) {
        return get().stream().filter(BooleanSetting -> BooleanSetting.getName().equalsIgnoreCase(settingName)).findFirst().orElse(null);
    }
    public boolean isSelected(String settingName) {
        BooleanSetting setting = getByName(settingName);
        return setting != null && setting.get();
    }

    public BooleanSetting get(int index) {
        return get().get(index);
    }

    @Override
    public ModeListSetting setVisible(Supplier<Boolean> bool) {
        return (ModeListSetting) super.setVisible(bool);
    }
}