package elexis.mixins.render;

import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
    // Slot 13 is the vanilla night-vision contribution in the pinned 1.21.11 update method.
    @ModifyVariable(method = "update", at = @At("STORE"), index = 13)
    private float Elexis$fullBright(float original) {
        return elexis.modules.impl.render.FullBright.lightStrength(original);
    }
}
