package elexis.mixins.render;

import elexis.client.utils.client.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static elexis.client.utils.player.ContainerManager.isContainerScreen;

@Mixin(Screen.class)
public class ScreenMixin implements IMinecraft {
    @Unique private long Elexis$openedAt;
    @Unique private boolean Elexis$animationPushed;

    @Inject(method = "init(II)V", at = @At("TAIL"))
    private void Elexis$screenAnimationStart(int width, int height, CallbackInfo ci) {
        Elexis$openedAt = System.nanoTime();
    }

    @Inject(method = "renderWithTooltip", at = @At("HEAD"))
    private void Elexis$screenAnimation(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        var cursor=elexis.modules.impl.render.visual.SceneVisuals.active("Cursor",elexis.modules.impl.render.visual.OverlayVisuals.Cursor.class);
        if(cursor!=null) cursor.refreshNativeCursor();
        var module = elexis.modules.impl.render.visual.SceneVisuals.active("BetterMinecraft", elexis.modules.impl.render.visual.GuiVisuals.BetterMinecraft.class);
        Object screen = this;
        if (module == null || screen instanceof elexis.ui.gui.ClickGui
                || module.inventoryOnly.get() && !(screen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen)) return;
        float progress = Math.min(1f, (System.nanoTime() - Elexis$openedAt) / (module.duration.get() * 1_000_000f));
        if (progress >= 1f) return;
        float remaining = (float) Math.pow(1f-progress,3);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0f, remaining * 14f);
        Elexis$animationPushed = true;
    }

    @Inject(method = "renderWithTooltip", at = @At("RETURN"))
    private void Elexis$screenAnimationEnd(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (Elexis$animationPushed) { context.getMatrices().popMatrix(); Elexis$animationPushed = false; }
    }
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void renderBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (mc.world == null) return;

        if (isContainerScreen(mc.currentScreen)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderInGameBackground", at = @At("HEAD"), cancellable = true)
    private void renderInGameBackground(DrawContext context, CallbackInfo ci) {
        if (mc.world == null) return;

        if (isContainerScreen(mc.currentScreen)) {
            ci.cancel();
        }
    }
}
