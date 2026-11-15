package elexis.mixins.accessors;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DrawContext.class)
public interface DrawContextAccessor {
    @Invoker("drawItemBar")
    void Elexis$drawItemBar(ItemStack stack, int x, int y);

    @Invoker("drawCooldownProgress")
    void Elexis$drawCooldownProgress(ItemStack stack, int x, int y);
}
