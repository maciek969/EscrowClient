package elexis.client;

import elexis.client.utils.render.color.basic.ThemeColorUtils;

import java.awt.Color;

public final class ClientColors {
    public static final Color FORE_COLOR = new Color(255, 255, 255, 255);
    public static final Color SECONDARY_FORE_COLOR = new Color(255, 255, 255, 255);
    public static final Color DARK_GRAY_COLOR = new Color(66, 68, 68, 255);
    public static final Color BACK_COLOR = new Color(12, 12, 12);
    public static final Color ENABLED = new Color(185, 221, 216, 255);
    public static final Color DISABLED = new Color(36, 36, 36, 255);
    public static final Color GUI_BACKGROUND = new Color(15, 15, 15);
    public static final Color UI_RED = new Color(255, 55, 55);
    public static final Color FRIEND_COLOR = new Color(50, 155, 50);
    public static final Color GREEN = new Color(100, 255, 100);
    public static final Color GUI_STROKE = new Color(255, 255, 255, 10);
    public static final Color GOLDEN_HP = new Color(222, 177, 27, 255);
    public static final Color RED = new Color(183, 0, 0, 255);
    public static final Color ICON_FOREGROUND_COLOR = new Color(255, 255, 255, 255);
    public static final Color HEALTH_COLOR = new Color(183, 0, 0, 255);
    public static final Color ARMOR_COLOR = new Color(100, 100, 100, 255);
    public static final Color HUNGER_COLOR = new Color(165, 105, 80, 255);
    public static final Color AIR_COLOR = new Color(10, 100, 255, 255);

    public static Color MAIN_COLOR = new Color(201, 31, 210, 255);
    public static Color SECONDARY_COLOR = new Color(101, 0, 110, 255);

    public static Color getMainColor() {
        try {
            Color c = ThemeColorUtils.activeTheme().getMain();
            return c != null ? c : MAIN_COLOR;
        } catch (Throwable t) {
            return MAIN_COLOR;
        }
    }

    public static Color getSecondaryColor() {
        try {
            Color c = ThemeColorUtils.activeTheme().getSecondary();
            return c != null ? c : SECONDARY_COLOR;
        } catch (Throwable t) {
            return SECONDARY_COLOR;
        }
    }

    private ClientColors() {
    }
}
