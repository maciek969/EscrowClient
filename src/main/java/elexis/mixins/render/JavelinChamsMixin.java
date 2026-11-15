package elexis.mixins.render;

import elexis.modules.impl.render.Chams;
import elexis.modules.impl.render.javelin.chams.ChamsPass;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static elexis.client.utils.client.IMinecraft.mc;

@Mixin(LivingEntityRenderer.class)
public abstract class JavelinChamsMixin {
    @Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
    private void elexis$chamsLayer(LivingEntityRenderState state, boolean showBody, boolean translucent, boolean outline,
                                   CallbackInfoReturnable<RenderLayer> cir) {
        if (!(state instanceof PlayerEntityRenderState player)) return;
        var module = Chams.active();
        if (module != null && ChamsPass.available(module) && module.targets(mc.world.getEntityById(player.id))) {
            cir.setReturnValue(ChamsPass.layer(module, player.skinTextures.body().texturePath()));
        }
    }

    @Inject(method = "getMixColor", at = @At("RETURN"), cancellable = true)
    private void elexis$chamsColor(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        if (!(state instanceof PlayerEntityRenderState player)) return;
        var module = Chams.active();
        if (module != null && ChamsPass.available(module) && module.targets(mc.world.getEntityById(player.id))) {
            cir.setReturnValue(module.tint(player));
        }
    }
}
