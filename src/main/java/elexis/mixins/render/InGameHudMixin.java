package elexis.mixins.render;

import elexis.Elexis;
import elexis.modules.impl.misc.NameProtect;
import elexis.client.utils.render.main.oldrender.system.sys2d.ClientLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.SpectatorHud;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Supplier;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @ModifyArg(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V"),
            index = 1)
    private Text elexis$protectScoreboard(Text text) {
        return NameProtect.display(text);
    }
    @Shadow
    private int overlayRemaining;
    @Shadow
    @Nullable
    private Text overlayMessage;
    @Shadow
    private int heldItemTooltipFade;
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract void render(DrawContext context, RenderTickCounter tickCounter);
    @Shadow
    @Final
    private SpectatorHud spectatorHud;
    @Shadow
    private Pair<InGameHud.BarType, Bar> currentBar;
    @Shadow
    @Final
    private Map<InGameHud.BarType, Supplier<Bar>> bars;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void client$render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ci.cancel();

        if (this.client.currentScreen == null || !(this.client.currentScreen instanceof LevelLoadingScreen)) {
            Elexis.getInstance().testRender.setDrawContext(context);
            elexis.modules.impl.render.hud.HudItemIcons.prepare(context);
            elexis.modules.impl.render.NameTags.prepareIcons(context);
            elexis.modules.impl.render.DamagePop.prepare(context);

            context.state.addSimpleElement(new ClientLayer());

            if (!this.client.options.hudHidden) {
                this.renderMiscOverlays(context, tickCounter);
                this.renderCrosshair(context, tickCounter);
                context.createNewRootLayer();
                if (this.client.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) {
                    this.spectatorHud.renderSpectatorMenu(context);
                } else {
                    renderHotbar(context, tickCounter);
                }

                if (this.client.interactionManager.hasStatusBars()) {
                    this.renderStatusBars(context);
                }

                this.renderMountHealth(context);
                InGameHud.BarType barType = this.getCurrentBarType();
                if (barType != this.currentBar.getKey()) {
                    this.currentBar = Pair.of(barType, this.bars.get(barType).get());
                }

                this.currentBar.getValue().renderBar(context, tickCounter);

                if (this.client.interactionManager.hasExperienceBar() && this.client.player.experienceLevel > 0) {
                    Bar.drawExperienceLevel(context, this.client.textRenderer, this.client.player.experienceLevel);
                }

                this.currentBar.getValue().renderAddons(context, tickCounter);
                if (this.client.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
                    this.renderHeldItemTooltip(context);
                } else if (this.client.player.isSpectator()) {
                    this.spectatorHud.render(context);
                }

                var hud = elexis.modules.impl.render.Interface.active();
                if (hud == null || !hud.elements.get("Effects") || client.inGameHud.getDebugHud().shouldShowDebugHud())
                    this.renderStatusEffectOverlay(context, tickCounter);
                this.renderBossBarHud(context, tickCounter);
            }
                this.renderSleepOverlay(context, tickCounter);
            if (!this.client.options.hudHidden) {
                this.renderDemoTimer(context, tickCounter);
                this.renderDebugHud(context);
                this.renderScoreboardSidebar(context, tickCounter);
                this.renderOverlayMessage(context, tickCounter);
                this.renderTitleAndSubtitle(context, tickCounter);
                this.renderChat(context, tickCounter);
                this.renderPlayerList(context, tickCounter);
                this.renderSubtitlesHud(context, false);
            }
        }
    }

    @Shadow
    protected abstract void renderMiscOverlays(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderCrosshair(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderHotbar(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderStatusBars(DrawContext context);

    @Shadow
    protected abstract void renderMountHealth(DrawContext context);

    @Shadow
    protected abstract InGameHud.BarType getCurrentBarType();

    @Shadow
    protected abstract void renderHeldItemTooltip(DrawContext context);

    @Shadow
    protected abstract void renderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderBossBarHud(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderSleepOverlay(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderDemoTimer(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderDebugHud(DrawContext context);

    @Shadow
    protected abstract void renderScoreboardSidebar(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderOverlayMessage(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderTitleAndSubtitle(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderChat(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderPlayerList(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderSubtitlesHud(DrawContext context, boolean renderDeferred);
}
