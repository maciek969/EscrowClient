package elexis.api.drags;

import java.util.function.Supplier;

public class Drag {
    public float x;
    public float y;
    public float width;
    public float height;
    public float dX;
    public float dY;
    public final String name;
    public final Supplier<Boolean> canDrag;
    public boolean dragging;

    public Drag(String name, Supplier<Boolean> canDrag) {
        this.name = name;
        this.canDrag = canDrag;
    }

    public Drag bound(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }
}
