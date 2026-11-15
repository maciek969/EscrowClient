package elexis.modules.impl.render.javelin.crystalhands;

public enum CrystalExplosion {
   RISE("Подъём"),
   SCATTER("Разлёт"),
   IMPULSE("Импульс"),
   COLLAPSE("Схлопывание"),
   VORTEX("Воронка"),
   SPLIT("Расслоение"),
   TRAIL("Шлейф");

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
