package yabloko.base.font;

public class Fonts {
    public static final Fonts REGULAR = new Fonts();
    public static final Fonts MEDIUM = new Fonts();
    public static final Fonts ICONS = new Fonts();
    public static final Fonts ICONS2 = new Fonts();

    private Fonts() {
    }

    public Font getFont(float size) {
        return new Font(size);
    }

    public float getWidth(String text, float size) {
        return text != null ? text.length() * (size * 0.45F) : 0.0F;
    }
}

