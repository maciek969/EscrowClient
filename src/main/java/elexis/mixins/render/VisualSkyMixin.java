package elexis.mixins.render;

import elexis.modules.impl.render.visual.VisualPostProcessor;
import net.minecraft.client.render.SkyRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRendering.class)
public class VisualSkyMixin {
    @Inject(method="renderTopSky",at=@At("RETURN"))
    private void elexis$proceduralSky(int color,CallbackInfo ci) {VisualPostProcessor.renderSky();}
}
