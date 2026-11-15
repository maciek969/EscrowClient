package yabloko.utility.render.display.base.color;

public final class ColorRGBA {
   public static final ColorRGBA WHITE = new ColorRGBA(255, 255, 255, 255);
   public static final ColorRGBA GRAY = new ColorRGBA(128, 128, 128, 255);
   public static final ColorRGBA LIGHT_GRAY = new ColorRGBA(200, 200, 200, 255);
   public static final ColorRGBA PURPLE = new ColorRGBA(179, 145, 255, 255);
   public static final ColorRGBA BLUE = new ColorRGBA(0, 120, 255, 255);
   public static final ColorRGBA GREEN = new ColorRGBA(102, 255, 102, 255);
   public static final ColorRGBA TRANSPARENT = new ColorRGBA(0, 0, 0, 0);
   private final int r;
   private final int g;
   private final int b;
   private final int a;

   public ColorRGBA(int r, int g, int b, float a) {
      this(r, g, b, Math.round(a));
   }

   public ColorRGBA(int r, int g, int b, int a) {
      this.r = r;
      this.g = g;
      this.b = b;
      this.a = a;
   }

   public ColorRGBA(int r, int g, int b) {
      this(r, g, b, 255);
   }

   public ColorRGBA(int rgba) {
      this(rgba >> 16 & 0xFF, rgba >> 8 & 0xFF, rgba & 0xFF, rgba >> 24 & 0xFF);
   }

   public ColorRGBA withAlpha(float alpha) {
      return new ColorRGBA(this.r, this.g, this.b, Math.round(alpha));
   }

   public ColorRGBA mulAlpha(float alpha) {
      return this.withAlpha((float)this.a * alpha / 255.0F);
   }

   public int getRGB() {
      return (this.a & 0xFF) << 24 | (this.r & 0xFF) << 16 | (this.g & 0xFF) << 8 | this.b & 0xFF;
   }

   public ColorRGBA mix(ColorRGBA other, float amount) {
      return new ColorRGBA(
         Math.round((float)this.r + (float)(other.r - this.r) * amount),
         Math.round((float)this.g + (float)(other.g - this.g) * amount),
         Math.round((float)this.b + (float)(other.b - this.b) * amount),
         Math.round((float)this.a + (float)(other.a - this.a) * amount)
      );
   }

   public ColorRGBA darker(float factor) {
      return new ColorRGBA(Math.round((float)this.r * factor), Math.round((float)this.g * factor), Math.round((float)this.b * factor), this.a);
   }
}
