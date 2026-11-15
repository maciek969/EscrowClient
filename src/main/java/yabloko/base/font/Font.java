package yabloko.base.font;

public class Font {
    private final float size;

    public Font(float size) {
        this.size = size;
    }

    public float width(String text) {
        return text != null ? text.length() * (size * 0.45F) : 0.0F;
    }

    public float height() {
        return size;
    }
}

