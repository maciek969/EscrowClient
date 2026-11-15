package elexis.client.utils.render.main.oldrender.system.sys2d;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import elexis.client.utils.render.main.oldrender.system.ClientPipelines;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

@Environment(EnvType.CLIENT)
public class ClientLayer implements SimpleGuiElementRenderState {
    public ClientLayer() {
    }

    public void setupVertices(VertexConsumer vertices) {
        vertices.vertex((float)this.x0(), (float)this.y0(), 0.0f).color(this.col1());
        vertices.vertex((float)this.x0(), (float)this.y1(), 0.0f).color(this.col2());
        vertices.vertex((float)this.x1(), (float)this.y1(), 0.0f).color(this.col2());
        vertices.vertex((float)this.x1(), (float)this.y0(), 0.0f).color(this.col1());
    }

    @Nullable
    private static ScreenRect createBounds(int x0, int y0, int x1, int y1, Matrix3x2f pose, @Nullable ScreenRect scissorArea) {
        ScreenRect screenRect = (new ScreenRect(x0, y0, x1 - x0, y1 - y0)).transformEachVertex(pose);
        return scissorArea != null ? scissorArea.intersection(screenRect) : screenRect;
    }

    public RenderPipeline pipeline() {
        return ClientPipelines.HUD;
    }

    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }

    public Matrix3x2f pose() {
        return new Matrix3x2f();
    }

    public int x0() {
        return 0;
    }

    public int y0() {
        return 0;
    }

    public int x1() {
        return 0;
    }

    public int y1() {
        return 0;
    }

    public int col1() {
        return -1;
    }

    public int col2() {
        return -1;
    }

    @Nullable
    public ScreenRect scissorArea() {
        return ScreenRect.empty();
    }

    @Nullable
    public ScreenRect bounds() {
        return new ScreenRect(this.x0(), this.y0(), this.x1(), this.y1());
    }
}
