package elexis.modules.api.settings.impl;

import elexis.modules.api.settings.api.Setting;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class ModeSetting extends Setting<String> {

    public String[] strings;
    public List<String> modes;
    public String currentMode;
    public int index;

    public ModeSetting(String name, String defaultVal, String... strings) {
        super(name, defaultVal);
        this.strings = strings;
        this.modes = Arrays.asList(strings);
        this.currentMode = defaultVal;
        this.index = Math.max(0, this.modes.indexOf(defaultVal));
    }

    public int getIndex() {
        syncState();
        int modeIndex = 0;
        for (String val : strings) {
            if (val.equalsIgnoreCase(get())) {
                return modeIndex;
            }
            modeIndex++;
        }
        return 0;
    }

    public boolean is(String s) {
        return get().equalsIgnoreCase(s);
    }

    @Override
    public void set(String value) {
        super.set(value);
        syncState();
    }

    @Override
    public String get() {
        syncState();
        return super.get();
    }

    private void syncState() {
        String value = super.get();
        this.currentMode = value;
        this.index = Math.max(0, this.modes.indexOf(value));
    }

    @Override
    public ModeSetting setVisible(Supplier<Boolean> bool) {
        return (ModeSetting) super.setVisible(bool);
    }

    @Override
    public ModeSetting hidden(Supplier<Boolean> bool) {
        return (ModeSetting) super.hidden(bool);
    }

}
