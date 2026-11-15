package elexis.mixins.render;

import elexis.Elexis;
import elexis.modules.impl.render.CustomWorld;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRendering.class)
public class CustomWorldSkyMixin {

    @Inject(method = "updateRenderState", at = @At("RETURN"), require = 0)
    private void customWorld$skyAngle(ClientWorld world, float tickDelta, Camera camera, SkyRenderState state, CallbackInfo ci) {
        elexis.modules.api.FunctionManager fm = Elexis.getFunctionManager();
        if (fm == null) return;
        CustomWorld mod = (CustomWorld) fm.getFunctionByName("CustomWorld");
        if (mod == null || !mod.isState()) return;

        long customTime = CustomWorld.getCustomTime();

        float timeOfDay = (customTime % 24000L) / 24000f;
        float skyAngle = MathHelper.clamp(MathHelper.cos(timeOfDay * (float) (Math.PI * 2.0)) * -2.0f + 0.5f, 0.0f, 1.0f);
        float sunAngleRad = timeOfDay * (float) (Math.PI * 2.0);

        state.sunAngle  = sunAngleRad;
        state.moonAngle = sunAngleRad + (float) Math.PI;
        state.starAngle = sunAngleRad;
        state.shouldRenderSkyDark = false;

        boolean isNight = customTime >= 12000 && customTime < 24000;
        state.starBrightness = isNight ? 1.0f : 0.0f;
    }
}
