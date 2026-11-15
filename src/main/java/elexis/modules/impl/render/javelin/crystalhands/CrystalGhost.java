package elexis.modules.impl.render.javelin.crystalhands;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public record CrystalGhost(
   double x,
   double y,
   double z,
   float age,
   int seed,
   float scale,
   int rgb,
   Identifier texture,
   boolean emissive,
   CrystalOptions options,
   float effectTime,
   float riseHeight,
   long born
) {

   public float progress(long now) {
      return MathHelper.clamp((float)(now - this.born) / (this.options.explosionSeconds() * 1.0E9F), 0.0F, 1.0F);
   }
}
