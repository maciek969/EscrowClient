package yabloko.utility.render.display.base;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import yabloko.base.font.Font;
import yabloko.utility.render.display.base.color.ColorRGBA;

public class CustomDrawContext {
   private final DrawContext drawContext;
   private final VertexConsumerProvider vertexConsumers;
   private final MatrixStack matrices = new MatrixStack();

   public CustomDrawContext(VertexConsumerProvider vertexConsumers) {
      this(null, vertexConsumers);
   }

   public CustomDrawContext(DrawContext drawContext, VertexConsumerProvider vertexConsumers) {
      this.drawContext = drawContext;
      this.vertexConsumers = vertexConsumers;
   }

   public DrawContext getDrawContext() {
      return this.drawContext;
   }

   public MatrixStack getMatrices() {
      return this.matrices;
   }

   public int getScaledWindowWidth() {
      return 0;
   }

   public int getScaledWindowHeight() {
      return 0;
   }

   public void drawText(Font font, String text, float x, float y, ColorRGBA color) {
   }

   public void drawText(Font font, Text text, float x, float y, float alpha) {
   }

   public void drawText(Font font, Text text, float x, float y, ColorRGBA color) {
   }

   public void drawTextWithBackground(TextRenderer renderer, Text text, int x, int y, int width, int color) {
   }

   public void drawItem(ItemStack stack, int x, int y) {
   }

   public void drawRoundedRect(float x, float y, float width, float height, BorderRadius radius, ColorRGBA color) {
   }

   public void drawRoundedBorder(float x, float y, float width, float height, float thickness, BorderRadius radius, ColorRGBA color) {
   }

   public void drawTexture(Identifier id, float x, float y, float width, float height, ColorRGBA color) {
   }

   public void pushMatrix() {
      this.matrices.push();
   }

   public void popMatrix() {
      this.matrices.pop();
   }
}
