package elexis.modules.api;

import elexis.modules.impl.render.Themes;

import java.awt.Color;

public enum Theme {
    RAINBOW("Rainbow", new Color(255, 255, 255), null),
    LAVENDER("Lavender", new Color(0xBE8FFF), darkened(0xBE8FFF, 0.35f)),
    SKY("Sky", new Color(0x5BC8FF), darkened(0x5BC8FF, 0.35f)),
    BLOOD("Blood", new Color(0xE63239), darkened(0xE63239, 0.35f)),
    OCEAN("Ocean", new Color(0x5F71BF), darkened(0x5F71BF, 0.35f)),
    EMERALD("Emerald", new Color(0x3CDC8C), darkened(0x3CDC8C, 0.35f)),
    ROSE("Rose", new Color(0xFF78BE), darkened(0xFF78BE, 0.35f)),
    GOLD("Gold", new Color(0xFCC058), darkened(0xFCC058, 0.35f)),
    DIAMOND("Diamond", new Color(0x7DD9FA), darkened(0x7DD9FA, 0.35f)),
    SUN("Sun", new Color(0xFCE758), darkened(0xFCE758, 0.35f)),
    CUSTOM("Custom", new Color(0xBE8FFF), darkened(0xBE8FFF, 0.35f));

    private final String displayName;
    private final Color main;
    private final Color secondary;

    Theme(String displayName, Color main, Color secondary) {
        this.displayName = displayName;
        this.main = main;
        this.secondary = secondary;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getMain() {
        if (this == CUSTOM && Themes.customColor != null) {
            return Themes.customColor.getColor();
        }
        return main;
    }

    public Color getSecondary() {
        if (this == RAINBOW) {
            // Rainbow has no fixed secondary — return white as fallback
            return new Color(255, 255, 255, 255);
        }
        if (this == CUSTOM && Themes.customColor != null) {
            return darkened(Themes.customColor.get(), 0.35f);
        }
        return secondary != null ? secondary : main;
    }

    public static Theme byDisplayName(String name) {
        if (name == null) {
            return LAVENDER;
        }
        for (Theme theme : values()) {
            if (theme.displayName.equalsIgnoreCase(name)) {
                return theme;
            }
        }
        return LAVENDER;
    }

    private static Color darkened(int rgb, float factor) {
        Color base = new Color(rgb, true);
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        hsb[2] = Math.max(0.0f, Math.min(1.0f, hsb[2] * factor));
        return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
    }
}
