package elexis.mixins.render;

import elexis.modules.impl.render.CustomWorld;
import net.minecraft.client.render.Camera;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class CustomWorldFogMixin {

    @Inject(
            method = "getFogColor",
            at = @At("RETURN"),
            cancellable = true
    )
    private void Elexis$customFogColor(Camera camera,float tickDelta,ClientWorld world,int viewDistance,float skyDarkness,
                                     CallbackInfoReturnable<Vector4f> cir) {
        CustomWorld module = CustomWorld.getInstance();
        if (module == null || !module.isState() || !CustomWorld.useFog.get() || !CustomWorld.fogColorMode.is("Custom")
                || camera.getSubmersionType()!=CameraSubmersionType.NONE) {
            return;
        }
        int color=CustomWorld.fogColor.get();
        cir.setReturnValue(new Vector4f(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,cir.getReturnValue().w));
    }

    // Preserve applyFog's ring-buffer upload. Cancelling it leaves GPU fog data uninitialized.
    @ModifyArgs(method="applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
    private void Elexis$customFogDistance(Args args) {
        var module=CustomWorld.getInstance();
        if(module==null || !module.isState() || !CustomWorld.useFog.get()) return;
        float scale=Math.max(0.05f,Math.min(1f,CustomWorld.fogDistance.get()));
        float end=Math.max(16f,(float)args.get(6)*scale);
        float start=Math.max(0f,Math.min(end-1f,(float)args.get(5)*scale));
        args.set(5,start);args.set(6,end);
    }
}
