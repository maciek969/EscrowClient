package yabloko.client.modules.impl.combat;

import net.minecraft.entity.LivingEntity;

public class Aura {
   public static final Aura INSTANCE = new Aura();

   private Aura() {
   }

   public LivingEntity getTarget() {
      return null;
   }
}
