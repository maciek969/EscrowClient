package elexis.modules.impl.render.javelin.crystalhands;

public enum CrystalAnimation {
   ROTATION("Spin"),
   PULSE("Pulse"),
   PENDULUM("Pendulum"),
   RESONANCE("Resonance"),
   GYROSCOPE("Gyroscope"),
   FLOAT("Levitation"),
   TUMBLE("Roll");

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
