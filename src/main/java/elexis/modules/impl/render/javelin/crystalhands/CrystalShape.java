package elexis.modules.impl.render.javelin.crystalhands;

public enum CrystalShape {
   CLASSIC("Classic"),
   VORTEX("Vortex"),
   CONSTELLATION("Constellation"),
   CONTOUR("Outline"),
   FACETS("Edges"),
   CORE("Core");

   public final String label;

   CrystalShape(String label) {
      this.label = label;
   }

   public static CrystalShape from(String label) {
      for (CrystalShape value : values()) {
         if (value.label.equals(label)) {
            return value;
         }
      }

      return CLASSIC;
   }
}
