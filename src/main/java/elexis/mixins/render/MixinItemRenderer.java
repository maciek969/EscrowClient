package elexis.mixins.render;

import elexis.modules.impl.render.ItemReplacer;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(HeldItemRenderer.class)
public abstract class MixinItemRenderer {
    @ModifyArg(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/item/ItemModelManager;clearAndUpdate(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/world/World;Lnet/minecraft/util/HeldItemContext;I)V"
            ),
            index = 1
    )
    private ItemStack elexis$replaceSwordModel(ItemStack stack) {
        ItemReplacer replacer = ItemReplacer.getInstance();
        return replacer == null ? stack : replacer.getRenderStack(stack);
    }
}