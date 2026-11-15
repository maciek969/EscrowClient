package elexis.api.drags;

import java.util.ArrayList;
import java.util.List;

public class Drags {
    private final List<Drag> drags = new ArrayList<>();

    public void addDrag(Drag drag) {
        drags.add(drag);
    }

    public Drag findDrag(String name) {
        return drags.stream().filter(drag -> drag.name.equals(name)).findFirst().orElse(null);
    }

    public void release() {
        for (Drag drag : drags) {
            drag.dragging = false;
        }
    }
}
