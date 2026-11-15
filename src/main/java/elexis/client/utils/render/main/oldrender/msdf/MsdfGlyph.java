package elexis.client.utils.render.main.oldrender.msdf;

import elexis.Elexis;
import elexis.client.utils.render.main.oldrender.system.ShaderUse;
import elexis.client.utils.render.main.oldrender.system.TextureUse;
import elexis.client.utils.render.main.oldrender.system.sys2d.TextureAtlas;

import java.awt.Color;

public final class MsdfGlyph {
    private final int code;
    private float minU;
    private float maxU;
    private float minV;
    private float maxV;
    private float advance;
    private float topPosition;
    private float width;
    private float height;

    public MsdfGlyph(FontData.GlyphData data, float atlasWidth, float atlasHeight) {
        this.code = data.unicode();
        this.advance = data.advance();
        FontData.BoundsData atlasBounds = data.atlasBounds();
        if (atlasBounds != null) {
            this.minU = atlasBounds.left() / atlasWidth;
            this.maxU = atlasBounds.right() / atlasWidth;
            this.minV = 1.0F - atlasBounds.top() / atlasHeight;
            this.maxV = 1.0F - atlasBounds.bottom() / atlasHeight;
        }
        FontData.BoundsData planeBounds = data.planeBounds();
        if (planeBounds != null) {
            this.width = planeBounds.right() - planeBounds.left();
            this.height = planeBounds.top() - planeBounds.bottom();
            this.topPosition = planeBounds.top();
        }
    }

    public float apply(float size, float x, float y, Color color1, Color color2, Color color3, Color color4, TextureUse textureUse) {
        y -= this.topPosition * size;
        float width = this.width * size;
        float height = this.height * size;
        TextureAtlas.UV uv = Elexis.getInstance().testRender.getCrenderSystem().getAtlas().getUV(textureUse.name().toLowerCase());
        Elexis.getInstance().testRender.getCrenderSystem().shader(ShaderUse.MSDF)
                .texture(0)
                .rect(x, y, width, height)
                .uv(uv.u0 + minU * uv.u1, uv.v0 + minV * uv.v1, (maxU - minU) * uv.u1, (maxV - minV) * uv.v1)
                .color(color1, color2, color3, color4)
                .build();
        return this.advance * size;
    }

    public float getWidth(float size) {
        return this.advance * size;
    }
}
