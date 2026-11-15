package elexis.mixins.render;

import elexis.modules.impl.render.CustomCrystal;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(EndCrystalEntityModel.class)
public abstract class JavelinCrystalModelMixin {
    @ModifyConstant(method="setAngles(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;)V",constant=@Constant(floatValue=3f))
    private float javelin$spin(float speed) { return speed * CustomCrystal.spin(); }
}
