package elexis.modules.impl.render.javelin.crystalhands;

public record CrystalBurst(float x, float y, float z, float scale, float yaw, float tilt, float opacity, int part) {
   public static final CrystalBurst STILL = new CrystalBurst(0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, -1);
}
