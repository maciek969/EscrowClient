package elexis.mixins.client;

import elexis.Elexis;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static elexis.client.utils.client.IMinecraft.mc;

@Mixin(HandledScreen.class)
public abstract class JavelinHandledScreenMixin {
    @Shadow protected Slot focusedSlot;

    @Inject(method = "render", at = @At("TAIL"))
    private void elexis$scrollItems(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        var manager = Elexis.getFunctionManager();
        if (manager == null || !manager.getItemScroller().isState()) return;
        var scroller = manager.getItemScroller();
        if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            scroller.drag((HandledScreen<?>)(Object)this, focusedSlot);
        } else scroller.endDrag();
    }
}
