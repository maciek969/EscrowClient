package yabloko.utility.game.player;

import net.minecraft.entity.LivingEntity;

public final class PlayerIntersectionUtil {
   private PlayerIntersectionUtil() {
   }

   public static float getHealth(LivingEntity entity) {
      return entity != null ? entity.getHealth() : 0.0F;
   }
}
