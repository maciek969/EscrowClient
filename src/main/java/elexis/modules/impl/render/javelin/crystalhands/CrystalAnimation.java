package elexis.modules.impl.render.javelin.crystalhands;

public enum CrystalAnimation {
   ROTATION("Вращение"),
   PULSE("Пульсация"),
   PENDULUM("Маятник"),
   RESONANCE("Резонанс"),
   GYROSCOPE("Гироскоп"),
   FLOAT("Левитация"),
   TUMBLE("Перекат");

   public final String label;

   CrystalAnimation(String label) {
      this.label = label;
   }

   public static CrystalAnimation from(String label) {
      for (CrystalAnimation value : values()) {
         if (value.label.equals(label)) {
            return value;
         }
      }

      return ROTATION;
   }
}
