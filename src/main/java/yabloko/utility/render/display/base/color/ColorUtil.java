package yabloko.utility.render.display.base.color;

public final class ColorUtil {
   private ColorUtil() {
   }

   public static ColorRGBA interpolate(ColorRGBA from, ColorRGBA to, float amount) {
      return from.mix(to, amount);
   }
}
