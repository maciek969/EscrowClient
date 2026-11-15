package elexis.client.utils.render.main.oldrender.msdf;

import elexis.Elexis;
import elexis.client.utils.render.main.oldrender.helper.ServerPrefix;
import elexis.client.utils.render.main.oldrender.helper.TextUtility;
import elexis.client.utils.render.main.oldrender.system.TextureUse;
import elexis.client.utils.render.main.oldrender.system.sys2d.TextureAtlas;
import elexis.client.utils.render.main.oldrender.util.ResourceProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.GlTextureView;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class MsdfFont {
    private final String name;
    private final AbstractTexture texture;
    private final FontData.AtlasData atlas;
    private final FontData.MetricsData metrics;
    public final Map<Integer, MsdfGlyph> glyphs;
    public final Map<Integer, Map<Integer, Float>> kernings;

    private MsdfFont(String name, AbstractTexture texture, FontData.AtlasData atlas, FontData.MetricsData metrics,
                     Map<Integer, MsdfGlyph> glyphs, Map<Integer, Map<Integer, Float>> kernings) {
        this.name = name;
        this.texture = texture;
        this.atlas = atlas;
        this.metrics = metrics;
        this.glyphs = glyphs;
        this.kernings = kernings;
    }

    public void applyGlyphs(String text, float size, float thickness, float spacing, float x, float y,
                            Color color1, Color color2, Color color3, Color color4, TextureUse tex, float smooth) {
        var system = Elexis.getInstance().testRender.getCrenderSystem();
        system.msdfRange(25).smoothness(smooth, smooth);
        text = TextUtility.smallToNormal(text);
        int prevChar = -1;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int[] rw = ServerPrefix.rwFrom(String.valueOf(ch));
            if (rw[0] != -1 && rw[1] != -1) {
                Identifier id = Identifier.of("elexis", "images/ui/rw_prefix.png");
                TextureAtlas.UV uv = system.getAtlas().getOrCreate(id.toString(), id);
                if (uv != null) {
                    TextureAtlas atlas = system.getAtlas();
                    float widthRw = uv.width / 4F;
                    float heightRw = uv.height / 16F;
                    float aspect = widthRw / heightRw;
                    float sizeRw = size + 1;
                    Elexis.getInstance().testRender.textureRaw(id, x, y - sizeRw / 2F - 2, sizeRw * aspect, sizeRw, 0, new Vector4f(0),
                            new Vector4f((widthRw * rw[0]) / atlas.atlasWidth, (heightRw * rw[1]) / atlas.atlasHeight, widthRw / atlas.atlasWidth, heightRw / atlas.atlasHeight),
                            Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
                }
                continue;
            }

            int code = ch;
            MsdfGlyph glyph = this.glyphs.get(code);
            if (glyph == null) continue;
            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                x += kerning.getOrDefault(code, 0.0f) * size;
            }
            x += glyph.apply(size, x, y, color1, color2, color3, color4, tex) + thickness + spacing;
            prevChar = code;
        }
    }

    public float getHeight(float size) {
        return this.metrics.lineHeight() * size;
    }

    public float getWidth(String text, float size) {
        text = TextUtility.smallToNormal(text);
        int prevChar = -1;
        float width = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int[] rw = ServerPrefix.rwFrom(String.valueOf(ch));
            if (rw[0] != -1 && rw[1] != -1) {
                width += size * 4.5F;
                continue;
            }
            MsdfGlyph glyph = this.glyphs.get((int) ch);
            if (glyph == null) continue;
            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                width += kerning.getOrDefault((int) ch, 0.0f) * size;
            }
            width += glyph.getWidth(size) + 0.3F;
            prevChar = ch;
        }
        return width;
    }

    public FontData.MetricsData getMetrics() { return metrics; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String name = "?";
        private Identifier dataIdentifier;
        private Identifier atlasIdentifier;

        public Builder name(String name) { this.name = name; return this; }
        public Builder data(String dataFileName) { this.dataIdentifier = Identifier.of("elexis", "fonts/" + dataFileName + ".json"); return this; }
        public Builder atlas(String atlasFileName) { this.atlasIdentifier = Identifier.of("elexis", "fonts/" + atlasFileName + ".png"); return this; }

        public MsdfFont build(TextureUse textureUse) {
            FontData data = ResourceProvider.fromJsonToInstance(this.dataIdentifier, FontData.class);
            AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(this.atlasIdentifier);
            float aWidth = data.atlas().width();
            float aHeight = data.atlas().height();
            Map<Integer, MsdfGlyph> glyphs = data.glyphs().stream().collect(Collectors.toMap(FontData.GlyphData::unicode, glyphData -> new MsdfGlyph(glyphData, aWidth, aHeight)));
            Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
            data.kernings().forEach(kerning -> {
                Map<Integer, Float> map = kernings.computeIfAbsent(kerning.leftChar(), ignored -> new HashMap<>());
                map.put(kerning.rightChar(), kerning.advance());
            });
            var system = Elexis.getInstance().testRender.getCrenderSystem();
            system.putTex(textureUse, ((GlTextureView) texture.getGlTextureView()).texture().getGlId());
            if (system.getAtlas().getUV(textureUse.name().toLowerCase()) == null) {
                system.getAtlas().add(textureUse, this.atlasIdentifier);
            }
            return new MsdfFont(this.name, texture, data.atlas(), data.metrics(), glyphs, kernings);
        }
    }
}
