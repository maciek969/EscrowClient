package yabloko.client.modules.api.setting.impl;

public final class BooleanSetting {
    private final String name;
    private boolean value;

    public BooleanSetting(String name, boolean value) {
        this.name = name;
        this.value = value;
    }

    public boolean get() {
        return this.value;
    }

    public boolean isEnabled() {
        return this.value;
    }
}

