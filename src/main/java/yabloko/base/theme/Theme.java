package yabloko.base.theme;

import yabloko.utility.render.display.base.color.ColorRGBA;

public class Theme {
    public ColorRGBA getForegroundColor() {
        return ColorRGBA.WHITE;
    }

    public ColorRGBA getForegroundStroke() {
        return ColorRGBA.GRAY;
    }

    public ColorRGBA getColor() {
        return ColorRGBA.PURPLE;
    }

    public ColorRGBA getGray() {
        return ColorRGBA.GRAY;
    }

    public ColorRGBA getWhite() {
        return ColorRGBA.WHITE;
    }

    public ColorRGBA getGrayLight() {
        return ColorRGBA.LIGHT_GRAY;
    }

    public ColorRGBA getForegroundLight() {
        return getForegroundColor();
    }

    public ColorRGBA getSecondColor() {
        return ColorRGBA.BLUE;
    }
}

