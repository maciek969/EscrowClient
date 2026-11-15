package elexis.mixins.render;

import elexis.modules.impl.render.visual.RainWeather;
import net.minecraft.client.render.WeatherRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherRendering.class)
public class RainWeatherMixin {
    @Inject(method="renderPrecipitation",at=@At("HEAD"),cancellable=true)
    private void elexis$replaceRainSheets(CallbackInfo ci) {if(RainWeather.replacesVanilla()) ci.cancel();}
    @Inject(method="addParticlesAndSound",at=@At("HEAD"),cancellable=true)
    private void elexis$replaceRainSound(CallbackInfo ci) {if(RainWeather.replacesVanilla()) ci.cancel();}
}
