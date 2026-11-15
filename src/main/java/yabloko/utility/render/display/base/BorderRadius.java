package yabloko.utility.render.display.base;

public final class BorderRadius {
   public static final BorderRadius ZERO = new BorderRadius();

   private BorderRadius() {
   }

   public static BorderRadius all(float radius) {
      return new BorderRadius();
   }

   public static BorderRadius left(float top, float bottom) {
      return new BorderRadius();
   }

   public static BorderRadius right(float top, float bottom) {
      return new BorderRadius();
   }

   public static BorderRadius top(float left, float right) {
      return new BorderRadius();
   }

   public static BorderRadius bottom(float left, float right) {
      return new BorderRadius();
   }
}
