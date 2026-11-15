package elexis.client.utils.render.main.oldrender.system;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public class CustomVertexConsumerProvider implements VertexConsumerProvider {
    private final Immediate parent;
    private final Immediate plainDrawer = VertexConsumerProvider.immediate(new BufferAllocator(1536));
    private int red = 255;
    private int green = 255;
    private int blue = 255;
    private int alpha = 255;

    public CustomVertexConsumerProvider(Immediate parent) {
        this.parent = parent;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer renderLayer) {
//		if (renderLayer.isOutline()) {
//        if (renderLayer.isOutline()) {
//            ChatUtility.send(renderLayer.getName());
//        }
//			VertexConsumer vertexConsumer = this.plainDrawer.getBuffer(RenderLayer.getArmorEntityGlint());
//			return new CustomVertexConsumerProvider.OutlineVertexConsumer(vertexConsumer, this.red, this.green, this.blue, this.alpha);
//		} else {
//			VertexConsumer vertexConsumer = this.parent.getBuffer(RenderLayer.getEntityTranslucent());
//			Optional<RenderLayer> optional = renderLayer.getAffectedOutline();
//			if (optional.isPresent()) {
//                ChatUtility.send(optional.get().getName());
//				VertexConsumer vertexConsumer2 = this.plainDrawer.getBuffer((RenderLayer)optional.get());
//				CustomVertexConsumerProvider.OutlineVertexConsumer outlineVertexConsumer = new CustomVertexConsumerProvider.OutlineVertexConsumer(
//					vertexConsumer2, this.red, this.green, this.blue, this.alpha
//				);
//				return VertexConsumers.union(outlineVertexConsumer, vertexConsumer);
//			} else {
//				return vertexConsumer;
//			}
//		}
        VertexConsumer vertexConsumer = this.parent.getBuffer(renderLayer);
        VertexConsumer vertexConsumer2 = this.plainDrawer.getBuffer(renderLayer);
        OutlineVertexConsumer outlineVertexConsumer = new OutlineVertexConsumer(
                vertexConsumer2, this.red, this.green, this.blue, this.alpha
        );
        return VertexConsumers.union(outlineVertexConsumer, vertexConsumer);
    }

    public void setColor(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public void draw() {
        this.plainDrawer.draw();
    }

    @Environment(EnvType.CLIENT)
    record OutlineVertexConsumer(VertexConsumer delegate, int color) implements VertexConsumer {
        public OutlineVertexConsumer(VertexConsumer delegate, int red, int green, int blue, int alpha) {
            this(delegate, ColorHelper.getArgb(alpha, red, green, blue));
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            this.delegate.vertex(x, y, z).color(this.color);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer color(int color) {
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            this.delegate.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer lineWidth(float width) {
            return this;
        }
    }
}
