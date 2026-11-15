package elexis.modules.api.settings.impl;

import elexis.modules.api.settings.api.Setting;

import java.util.function.Supplier;

public class ButtonSetting extends Setting<Runnable> {
    public ButtonSetting(String name, Runnable action) {
        super(name, action);
    }

    public void press() {
        Runnable action = get();
        if (action != null) {
            action.run();
        }
    }

    @Override
    public ButtonSetting setVisible(Supplier<Boolean> bool) {
        return (ButtonSetting) super.setVisible(bool);
    }

    @Override
    public ButtonSetting hidden(Supplier<Boolean> bool) {
        return (ButtonSetting) super.hidden(bool);
    }
}