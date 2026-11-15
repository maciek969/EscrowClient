package elexis.client.utils.client.tweaks.crosshair;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.util.Identifier;

public interface DrawContextFloatDrawTexture {
    default void Elexis$drawGuiTexture(RenderPipeline pipeline, Identifier texture, float x, float y, int width, int height) {}
}
