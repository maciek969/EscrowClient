package elexis.mixins.render;

import elexis.modules.impl.render.CustomWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class CustomWorldWeatherMixin {
    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true, require = 0)
    private void Elexis$customRain(float delta, CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof net.minecraft.client.world.ClientWorld)) return;
        var ambience = elexis.modules.impl.render.visual.SceneVisuals.active("Ambience", elexis.modules.impl.render.visual.SceneVisuals.Ambience.class);
        if (ambience != null) { cir.setReturnValue(ambience.rain()); return; }
        CustomWorld module = CustomWorld.getInstance();
        if (module == null) {
            return;
        }
        float gradient = module.getRainGradient();
        if (gradient >= 0f) {
            cir.setReturnValue(gradient);
        }
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true, require = 0)
    private void Elexis$customThunder(float delta, CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof net.minecraft.client.world.ClientWorld)) return;
        var ambience = elexis.modules.impl.render.visual.SceneVisuals.active("Ambience", elexis.modules.impl.render.visual.SceneVisuals.Ambience.class);
        if (ambience != null) { cir.setReturnValue(ambience.thunder()); return; }
        CustomWorld module = CustomWorld.getInstance();
        if (module == null) {
            return;
        }
        float gradient = module.getThunderGradient();
        if (gradient >= 0f) {
            cir.setReturnValue(gradient);
        }
    }
}
