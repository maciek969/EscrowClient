package elexis.modules.impl.render.javelin.crystalhands;

public enum CrystalShape {
   CLASSIC("Классика"),
   VORTEX("Вихрь"),
   CONSTELLATION("Созвездие"),
   CONTOUR("Контур"),
   FACETS("Грани"),
   CORE("Ядро");

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
