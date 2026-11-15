package elexis.client.utils.render.easy;

import elexis.Elexis;
import elexis.client.utils.render.main.oldrender.system.sys2d.CRenderSystem;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;

import java.awt.*;

public class TextureRenderer {
    public static void render(Identifier identifier, float x, float y, float width, float height, Vector4f rounding, Color color) {
        Elexis.getInstance().testRender.getCrenderSystem().layerTex(Elexis.getInstance().testRender.getCrenderSystem().getAtlas().getGlId()).layer(CRenderSystem.RenderLayer.GUI);

        Elexis.getInstance().testRender.texture(identifier, x, y, width, height,
                0.85f, new Vector4f(0), rounding,
                color, color, color, color);
    }
}