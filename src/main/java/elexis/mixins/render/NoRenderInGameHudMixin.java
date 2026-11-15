package elexis.mixins.render;

import elexis.modules.impl.render.NoRneder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class NoRenderInGameHudMixin {

    @Inject(method = "renderBossBarHud", at = @At("HEAD"), cancellable = true)
    private void noRender$bossBar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (NoRneder.is("Boss bar")) ci.cancel();
    }

    @Inject(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("HEAD"), cancellable = true
    )
    private void noRender$scoreboard(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (NoRneder.is("Scoreboard")) ci.cancel();
    }

    @Inject(method = "renderTitleAndSubtitle", at = @At("HEAD"), cancellable = true)
    private void noRender$titles(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (NoRneder.is("Titles")) ci.cancel();
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void noRender$vignette(DrawContext context, Entity entity, CallbackInfo ci) {
        if (NoRneder.is("Vignette")) ci.cancel();
    }

    @Inject(method = "renderAirBubbles", at = @At("HEAD"), cancellable = true, require = 0)
    private void noRender$airBubbles(DrawContext context, PlayerEntity player, int left, int top, int unused, CallbackInfo ci) {
        if (NoRneder.is("Air bubbles")) ci.cancel();
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void noRender$overlay(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
        if (texture == null) return;
        String path = texture.getPath();
        if (NoRneder.is("Block overlay") && (path.contains("pumpkin") || path.contains("powder_snow"))) {
            ci.cancel();
        }
    }
}
