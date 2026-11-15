package elexis.mixins.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import elexis.modules.impl.render.CustomWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.world.ClientWorld$Properties")
public class CustomWorldTimeMixin {
    @ModifyReturnValue(method = "getTimeOfDay", at = @At("RETURN"))
    private long Elexis$customTime(long original) {
        var ambience = elexis.modules.impl.render.visual.SceneVisuals.active("Ambience", elexis.modules.impl.render.visual.SceneVisuals.Ambience.class);
        if (ambience != null && ambience.changeTime.get()) return ambience.time.get().longValue();
        CustomWorld module = CustomWorld.getInstance();
        if (module == null) {
            return original;
        }
        return module.getTime(original);
    }
}
