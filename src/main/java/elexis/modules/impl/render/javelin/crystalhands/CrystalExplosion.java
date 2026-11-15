package elexis.modules.impl.render.javelin.crystalhands;

public enum CrystalExplosion {
   RISE("Rise"),
   SCATTER("Scatter"),
   IMPULSE("Impulse"),
   COLLAPSE("Collapse"),
   VORTEX("Funnel"),
   SPLIT("Layering"),
   TRAIL("Trail");

   public final String label;

   CrystalExplosion(String label) {
      this.label = label;
   }

   public static CrystalExplosion from(String label) {
      for (CrystalExplosion value : values()) {
         if (value.label.equals(label)) {
            return value;
         }
      }

      return RISE;
   }
}
