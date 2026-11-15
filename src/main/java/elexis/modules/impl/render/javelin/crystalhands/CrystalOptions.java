package elexis.modules.impl.render.javelin.crystalhands;

public record CrystalOptions(
   CrystalShape shape,
   CrystalAnimation animation,
   float effectSpeed,
   float lineWidth,
   float brightness,
   boolean throughWalls,
   boolean explosionEnabled,
   CrystalExplosion explosion,
   float explosionSeconds
) {
}
