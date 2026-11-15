package yabloko.utility.mixin.accessors;

import net.minecraft.item.ItemStack;

public interface DrawContextAccessor {
   void callDrawItemBar(ItemStack var1, int var2, int var3);

   void callDrawCooldownProgress(ItemStack var1, int var2, int var3);
}
