package elexis.mixins.client;

import elexis.Elexis;
import elexis.client.events.impl.client.EventKey;
import elexis.modules.impl.render.Interface;
import elexis.client.events.impl.client.EventMouseScroll;
import elexis.client.events.impl.entity.EventLook;
import elexis.client.events.impl.render.EventFov;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static elexis.client.utils.client.IMinecraft.mc;

@Mixin(Mouse.class)
public class MouseMixin {
    @Shadow
    private double cursorDeltaX;
    @Shadow
    private double cursorDeltaY;

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    public void onMouseButtonHook(long window, MouseInput input, int action, CallbackInfo ci) {
        var client = mc;
        int button = input.button();

        if (button != GLFW.GLFW_KEY_UNKNOWN && window == client.getWindow().getHandle()) {
            Elexis.getEventBus().post(new EventKey(client.currentScreen, InputUtil.Type.MOUSE, button, action));

            var hud = Interface.active();
            if (hud != null && hud.mouse(button, action)) ci.cancel();
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true, require = 0)
    private void Elexis$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (window != mc.getWindow().getHandle()) {
            return;
        }
        EventMouseScroll event = new EventMouseScroll(horizontal, vertical);
        Elexis.getEventBus().post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "updateMouse", at = @At(value = "HEAD"))
    public void onUpdateMouse(double timeDelta, CallbackInfo ci) {
        EventFov event = new EventFov();
        Elexis.getEventBus().post(event);
        if (event.isCancelled()) {
            double slowdown = (double) event.getFov() / MinecraftClient.getInstance().options.getFov().getValue();
            this.cursorDeltaX *= slowdown;
            this.cursorDeltaY *= slowdown;
        }
    }

    @Redirect(
            method = "updateMouse",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"),
            require = 0
    )
    private void Elexis$redirectFreeLookInput(ClientPlayerEntity player, double yawDelta, double pitchDelta) {
        EventLook event = new EventLook(yawDelta, pitchDelta);
        Elexis.getEventBus().post(event);
        if (!event.isCancelled()) {
            player.changeLookDirection(event.getYaw(), event.getPitch());
        }
    }
}
