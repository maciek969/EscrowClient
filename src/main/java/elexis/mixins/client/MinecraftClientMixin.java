package elexis.mixins.client;

import elexis.Elexis;
import elexis.client.events.impl.entity.EventUpdate;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static elexis.client.utils.client.IMinecraft.mc;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void elexis$handleCrystalAuraMining(boolean breaking, CallbackInfo ci) {
        var manager = Elexis.getFunctionManager();
        if (!breaking && manager != null && manager.getCrystalAura() != null
                && manager.getCrystalAura().handleMiningInput()) {
            ci.cancel();
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void Elexis$preserveTriggerAttackCharge(CallbackInfoReturnable<Boolean> cir) {
        var manager = Elexis.getFunctionManager();
        if (manager != null && manager.getTriggerBot() != null
                && manager.getTriggerBot().shouldBlockManualAttack()) cir.setReturnValue(false);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Mouse;tick()V", shift = At.Shift.AFTER))
    private void Elexis$smoothAimFrame(CallbackInfo ci) {
        var manager = Elexis.getFunctionManager();
        if (manager != null && manager.getAimBot() != null) manager.getAimBot().onFrame();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;blitToScreen()V"))
    private void client$onRender(CallbackInfo ci) {
        Elexis.hookRender();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        Elexis elexisInstance = Elexis.getInstance();
        if (elexisInstance != null && mc != null && mc.getWindow() != null && !elexisInstance.isInitialized()) {
            elexisInstance.ensureRendererInitialized();
        }

        if (mc != null && mc.world != null && mc.player != null) Elexis.getEventBus().post(new EventUpdate.Fast());
        else if(mc!=null && Elexis.getFunctionManager()!=null) {
            elexis.modules.impl.render.visual.VisualPostProcessor.release();
            elexis.modules.impl.render.visual.SceneShockwaves.clear();
            elexis.modules.impl.render.visual.VisualGeometry.release();
            var cat=Elexis.getFunctionManager().getFunctionByName("CatPet");
            if(cat instanceof elexis.modules.impl.render.visual.OverlayVisuals.CatPet pet) pet.releaseWorld();
            var rain=Elexis.getFunctionManager().getFunctionByName("Rain");
            if(rain instanceof elexis.modules.impl.render.visual.SceneVisuals.Rain module) module.weather.clear();
        }
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void onInit(CallbackInfo ci) {
        Elexis elexisInstance = Elexis.getInstance();
        if (elexisInstance != null) {
            elexisInstance.init();
        }
    }
}
