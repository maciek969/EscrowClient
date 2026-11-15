package yabloko.utility.render.display.shader;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import yabloko.utility.render.display.base.BorderRadius;
import yabloko.utility.render.display.base.color.ColorRGBA;

public final class DrawUtil {
   private DrawUtil() {
   }

   public static void drawBlurHud(MatrixStack matrices, float x, float y, float width, float height, float radius, BorderRadius borderRadius, ColorRGBA color) {
   }

   public static void drawBlur(MatrixStack matrices, float x, float y, float width, float height, float radius, BorderRadius borderRadius, ColorRGBA color) {
   }

   public static void drawRoundedCorner(
      MatrixStack matrices, float x, float y, float width, float height, float thickness, float rounding, ColorRGBA color, BorderRadius borderRadius
   ) {
   }

   public static void drawMetanoise(
      MatrixStack matrices, float x, float y, float width, float height, float strength, float blur, ColorRGBA border, ColorRGBA inner
   ) {
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA fill) {
   }

   public static void drawRoundedRect(
      MatrixStack matrices,
      float x,
      float y,
      float width,
      float height,
      BorderRadius borderRadius,
      ColorRGBA fill,
      ColorRGBA fill2,
      ColorRGBA stroke,
      ColorRGBA stroke2
   ) {
   }

   public static void drawPlayerHeadWithRoundedShader(
      MatrixStack matrices, Identifier texture, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color
   ) {
   }

   public static void drawRoundedTextureWithUV(
      MatrixStack matrices,
      Identifier texture,
      float x,
      float y,
      float width,
      float height,
      BorderRadius radius,
      ColorRGBA color,
      float u1,
      float v1,
      float u2,
      float v2
   ) {
   }
}
