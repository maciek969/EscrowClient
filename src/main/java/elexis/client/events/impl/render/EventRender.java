package elexis.client.events.impl.render;

import elexis.client.events.api.Event;
import elexis.client.utils.render.main.newrender.core.Renderer2D;
import elexis.client.utils.render.main.newrender.text.FontObject;
import lombok.Getter;
import net.minecraft.client.util.math.MatrixStack;

public class EventRender extends Event {

    public static class World extends EventRender {
        @Getter MatrixStack matrixStack;
        @Getter float ticks;

        public World(MatrixStack matrixStack, float ticks) {
            this.matrixStack = matrixStack;
            this.ticks = ticks;
        }
    }

    public static class Screen extends EventRender {

        @Getter Renderer2D renderer;
        @Getter FontObject defaultFont;
        @Getter int viewportWidth;
        @Getter int viewportHeight;

        protected Screen(Renderer2D renderer, FontObject defaultFont, int viewportWidth, int viewportHeight) {
            this.renderer = renderer;
            this.defaultFont = defaultFont;
            this.viewportWidth = viewportWidth;
            this.viewportHeight = viewportHeight;
        }

        public static class Hud extends Screen {
            public Hud(Renderer2D renderer, FontObject defaultFont, int viewportWidth, int viewportHeight) {
                super(renderer, defaultFont, viewportWidth, viewportHeight);
            }
        }

        public static class Gui extends Screen {
            public Gui(Renderer2D renderer, FontObject defaultFont, int viewportWidth, int viewportHeight) {
                super(renderer, defaultFont, viewportWidth, viewportHeight);
            }
        }

        public static class Notifications extends Screen {
            public Notifications(Renderer2D renderer, FontObject defaultFont, int viewportWidth, int viewportHeight) {
                super(renderer, defaultFont, viewportWidth, viewportHeight);
            }
        }
    }
}