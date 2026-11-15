package elexis.client.events.impl.render;

import elexis.client.events.api.Event;
import lombok.Getter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;

public abstract class RenderItemEvent extends Event {
    @Getter
    private final MatrixStack matrixStack;
    @Getter
    private final Hand hand;

    protected RenderItemEvent(MatrixStack matrixStack, Hand hand) {
        this.matrixStack = matrixStack;
        this.hand = hand;
    }

    public static class Pre extends RenderItemEvent {
        public Pre(MatrixStack matrixStack, Hand hand) {
            super(matrixStack, hand);
        }
    }

    public static class Post extends RenderItemEvent {
        public Post(MatrixStack matrixStack, Hand hand) {
            super(matrixStack, hand);
        }
    }
}
