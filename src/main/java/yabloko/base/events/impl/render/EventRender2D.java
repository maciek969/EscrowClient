package yabloko.base.events.impl.render;

import net.minecraft.client.render.RenderTickCounter;
import yabloko.utility.render.display.base.CustomDrawContext;

public class EventRender2D {
    private final CustomDrawContext context;
    private final float tickDelta;

    public EventRender2D(CustomDrawContext context, float tickDelta) {
        this.context = context;
        this.tickDelta = tickDelta;
    }

    public CustomDrawContext getContext() {
        return context;
    }

    public float getTickDelta() {
        return tickDelta;
    }
}

