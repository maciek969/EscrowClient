package elexis.mixins.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import elexis.Elexis;
import elexis.client.utils.client.tweaks.crosshair.DrawContextFloatDrawTexture;
import elexis.modules.impl.utils.BetterGameplay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DrawContext.class)
public class DrawContextMixin implements DrawContextFloatDrawTexture {
    @Unique
    public void Elexis$drawGuiTexture(RenderPipeline pipeline, Identifier texture, float x, float y, int width, int height) {
        if (width == 0 || height == 0) return;
        if (!BetterGameplay.list.isSelected("РџСЂРёС†РµР»") && !Elexis.getFunctionManager().getBetterGameplay().isState()) return;

        ((DrawContext) (Object) this).drawGuiTexture(pipeline, texture, Math.round(x), Math.round(y), width, height);
    }
}