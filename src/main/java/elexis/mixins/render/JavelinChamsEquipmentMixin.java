package elexis.mixins.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import elexis.modules.impl.render.Chams;
import elexis.modules.impl.render.javelin.chams.ChamsPass;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static elexis.client.utils.client.IMinecraft.mc;

@Mixin(EquipmentRenderer.class)
public abstract class JavelinChamsEquipmentMixin {
    @WrapOperation(method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
            at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"))
    private <S> void elexis$captureArmor(RenderCommandQueue queue, Model<? super S> model, S state,
                                        MatrixStack matrices, RenderLayer layer, int light, int overlay, int tint,
                                        Sprite sprite, int outline, ModelCommandRenderer.CrumblingOverlayCommand crumbling,
                                        Operation<Void> original, @Local(index = 19) Identifier texture) {
        original.call(queue, model, state, matrices, layer, light, overlay, tint, sprite, outline, crumbling);
        if (!(state instanceof PlayerEntityRenderState player)) return;
        var module = Chams.active();
        if (module != null && module.glowing() && ChamsPass.available(module) && module.targets(mc.world.getEntityById(player.id))) {
            queue.submitModel(model, state, matrices, ChamsPass.armorLayer(texture), light, OverlayTexture.DEFAULT_UV,
                    0xFFFFFFFF, null, 0, null);
        }
    }
}
