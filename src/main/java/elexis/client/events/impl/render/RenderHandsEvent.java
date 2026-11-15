package elexis.client.events.impl.render;

import elexis.client.events.api.Event;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;

public abstract class RenderHandsEvent extends Event {
    private MatrixStack matrixStack;
    private Hand hand;

    public RenderHandsEvent(MatrixStack matrixStack, Hand hand) {
        this.matrixStack = matrixStack;
        this.hand = hand;
    }

    public MatrixStack getMatrixStack() {
        return matrixStack;
    }

    public Hand getHand() {
        return hand;
    }

    public static class Pre extends RenderHandsEvent {
        public Pre(MatrixStack matrixStack, Hand hand) {
            super(matrixStack, hand);
        }
    }

    public static class Post extends RenderHandsEvent {
        public Post(MatrixStack matrixStack, Hand hand) {
            super(matrixStack, hand);
        }
    }
}
