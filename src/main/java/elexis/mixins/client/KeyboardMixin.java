package elexis.mixins.client;

import elexis.Elexis;
import elexis.client.events.impl.client.EventKey;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static elexis.client.utils.client.IMinecraft.mc;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(at = @At("HEAD"), method = "onKey")
    public void onPressKey(long window, int action, KeyInput input, CallbackInfo ci) {
        int key = input.key();

        if (key != GLFW.GLFW_KEY_UNKNOWN && window == mc.getWindow().getHandle()) {
            Elexis.getEventBus().post(new EventKey(mc.currentScreen, InputUtil.Type.KEYSYM, key, action));
        }
    }
}
