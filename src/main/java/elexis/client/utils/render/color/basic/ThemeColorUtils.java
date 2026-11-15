package elexis.client.utils.render.color.basic;

import elexis.modules.api.Theme;
import elexis.modules.impl.render.Themes;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;

public final class ThemeColorUtils {
    private ThemeColorUtils() {
    }

    public static Theme activeTheme() {
        return Theme.byDisplayName(Themes.theme.get());
    }

    public static int getIconColor(int speed, int index) {
        Theme theme = activeTheme();
        if (theme == Theme.RAINBOW) {
            return rainbow(speed, index, 255);
        }
        return fade(theme.getMain().getRGB(), theme.getSecondary().getRGB(), speed, index);
    }

    public static int getMainColor(int speed, int index) {
        return getIconColor(speed, index);
    }

    public static int[] getClientColor(int speed, int alpha) {
        return new int[] {
                applyOpacity(getIconColor(speed, 0), alpha),
                applyOpacity(getIconColor(speed, 90), alpha),
                applyOpacity(getIconColor(speed, 180), alpha),
                applyOpacity(getIconColor(speed, 270), alpha)
        };
    }

    public static int applyOpacity(int color, int alpha) {
        int safeAlpha = MathHelper.clamp(alpha, 0, 255);
        return (safeAlpha << 24) | (color & 0x00FFFFFF);
    }

    public static int rgba(int r, int g, int b, int a) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    public static int getColor(int r, int g, int b, int a) {
        return rgba(r, g, b, a);
    }

    public static int interpolateColor(int first, int second, double offset) {
        float t = (float) MathHelper.clamp(offset, 0.0, 1.0);
        int r = (int) MathHelper.lerp(t, (first >> 16) & 0xFF, (second >> 16) & 0xFF);
        int g = (int) MathHelper.lerp(t, (first >> 8) & 0xFF, (second >> 8) & 0xFF);
        int b = (int) MathHelper.lerp(t, first & 0xFF, second & 0xFF);
        int a = (int) MathHelper.lerp(t, (first >> 24) & 0xFF, (second >> 24) & 0xFF);
        return rgba(r, g, b, a);
    }

    public static int interpolateColor(Color first, Color second, double offset) {
        return interpolateColor(first.getRGB(), second.getRGB(), offset);
    }

    public static int overCol(int base, int overlay, double factor) {
        return interpolateColor(base, overlay, factor);
    }

    public static int multAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * MathHelper.clamp(alpha, 0f, 1f));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static int rainbow(int speed, int index, int alpha) {
        float hue = ((System.currentTimeMillis() / (float) Math.max(1, speed) + index) % 360f) / 360f;
        int rgb = Color.HSBtoRGB(hue, 0.8f, 1f);
        return applyOpacity(rgb, alpha);
    }

    private static int fade(int first, int second, int speed, int index) {
        int angle = (int) ((System.currentTimeMillis() / Math.max(1, speed) + index) % 360L);
        if (angle >= 180) {
            angle = 360 - angle;
        }
        return interpolateColor(first, second, angle / 180f);
    }
}
