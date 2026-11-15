package elexis.mixins.render;

import elexis.modules.impl.render.NoRneder;
import elexis.modules.impl.render.NoPlayer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class NoRenderLivingEntityRendererMixin {

    @Inject(
            method = "getShadowRadius(Lnet/minecraft/client/render/entity/state/EntityRenderState;)F",
            at = @At("HEAD"), cancellable = true, require = 0
    )
    private void noRender$shadows(EntityRenderState state, CallbackInfoReturnable<Float> cir) {
        if (NoRneder.is("Тени")) cir.setReturnValue(0f);
    }
}
