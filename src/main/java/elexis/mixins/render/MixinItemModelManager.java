package elexis.mixins.render;

import elexis.modules.impl.render.ItemReplacer;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemModelManager.class)
public class MixinItemModelManager {
    @ModifyVariable(
            method = "clearAndUpdate(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/world/World;Lnet/minecraft/util/HeldItemContext;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private ItemStack elexis$replaceItemStack(ItemStack stack, ItemRenderState state, ItemStack originalStack, ItemDisplayContext displayContext, World world, HeldItemContext heldItemContext, int seed) {
        ItemReplacer replacer = ItemReplacer.getInstance();
        if (replacer == null) {
            return stack;
        }
        return replacer.getRenderStack(stack);
    }
}