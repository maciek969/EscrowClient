package elexis.mixins.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import elexis.modules.impl.render.CustomCrystal;
import elexis.modules.impl.render.javelin.crystalhands.CrystalEffects;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.*;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(EndCrystalEntityRenderer.class)
public abstract class JavelinCrystalRendererMixin {
    @Inject(method="updateRenderState(Lnet/minecraft/entity/decoration/EndCrystalEntity;Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;F)V", at=@At("TAIL"))
    private void javelin$state(EndCrystalEntity entity, EndCrystalEntityRenderState state, float delta, CallbackInfo ci) {
        if (!CustomCrystal.on()) return;
        state.baseVisible &= CustomCrystal.base();
        if (!CustomCrystal.beam()) state.beamOffset = null;
    }

    @Redirect(method="render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"))
    private void javelin$scale(MatrixStack matrices, float x, float y, float z) {
        float scale = CustomCrystal.scale();
        matrices.scale(x * scale, y * scale, z * scale);
    }

    @WrapOperation(method="render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"))
    private <S> void javelin$texture(OrderedRenderCommandQueue queue, Model<? super S> model, S state, MatrixStack matrices,
            RenderLayer layer, int light, int overlay, int outline, ModelCommandRenderer.CrumblingOverlayCommand crumbling, Operation<Void> original) {
        if (CustomCrystal.astralActive() && CrystalEffects.replaces((EndCrystalEntityRenderState)state)) return;
        Identifier texture = CustomCrystal.textureOverride();
        if (texture != null) layer = CustomCrystal.shaderActive() ? RenderLayers.entityTranslucentEmissive(texture) : RenderLayers.entityTranslucent(texture);
        original.call(queue, model, state, matrices, layer, light, overlay, outline, crumbling);
    }

    @Inject(method="getYOffset",at=@At("RETURN"),cancellable=true)
    private static void javelin$bob(float age, CallbackInfoReturnable<Float> result) {
        if (CustomCrystal.on()) result.setReturnValue(result.getReturnValue() * CustomCrystal.bob());
    }
}
