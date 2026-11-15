package elexis.client.utils.render.main.oldrender;

import elexis.Elexis;
import elexis.client.utils.client.IMinecraft;
import elexis.client.utils.render.main.oldrender.msdf.Fonts;
import elexis.client.utils.render.main.oldrender.msdf.MsdfFont;
import elexis.client.utils.render.main.oldrender.system.IconUse;
import elexis.client.utils.render.main.oldrender.system.ShaderUse;
import elexis.client.utils.render.main.oldrender.system.TextureUse;
import elexis.client.utils.render.main.oldrender.system.sys2d.CRenderSystem;
import elexis.client.utils.render.main.oldrender.system.sys2d.TextureAtlas;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.GlTextureView;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Getter
public class ClientRenderer implements IMinecraft {
    final MatrixStack stack = new MatrixStack();
    final Fonts fonts = new Fonts();

    public ClientRenderer() {

    }
    ArrayList<Runnable> postTasks = new ArrayList<>();

    public void queueTask(Runnable task) {
        postTasks.add(task);
    }

    public void runTasks() {
        Iterator<Runnable> iterator = postTasks.iterator();
        while (iterator.hasNext()) {
            Runnable item = iterator.next();
            item.run();
            iterator.remove();
        }
    }

    CRenderSystem crenderSystem = new CRenderSystem();
    @Nullable
    @Setter
    DrawContext drawContext;

    public void render() {

        crenderSystem.layerTex(crenderSystem.getAtlas().getGlId()).render(CRenderSystem.RenderLayer.GUI);
    }
    public void prepare() {
        crenderSystem.prepare();
    }

    public void rect(float x, float y, float width, float height, Vector4f round, float smooth, Color topLeft, Color bottomLeft, Color bottomRight, Color topRight) {
        crenderSystem.shader(ShaderUse.RECTANGLE)
                .rect(x, y, width, height)
                .round(round.x, round.y, round.z, round.w)
                .smoothness(smooth, smooth)
                .color(topLeft, bottomLeft, bottomRight, topRight)
                .build();
    }

    public void blur(float x, float y, float width, float height, Vector4f round, float blurRadius, float smooth) {
        CRenderSystem.RenderLayer prev = crenderSystem.layer();
        crenderSystem.layer(CRenderSystem.RenderLayer.HUD);
        crenderSystem.layerTex(crenderSystem.getAtlas().getGlId());
        crenderSystem.shader(ShaderUse.BLUR)
                .uv(x / mc.getWindow().getScaledWidth(), 1 - (y / mc.getWindow().getScaledHeight()), (width / mc.getWindow().getScaledWidth()), -(height / mc.getWindow().getScaledHeight()))
                .rect(x, y, width, height)
                .color(Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE)
                .round(round.x, round.y, round.z, round.w)
                .blur(blurRadius)
                .smoothness(smooth, smooth)
                .build();
        crenderSystem.layer(prev);
    }

    public void text(String text, float x, float y, TextureUse font, float size, Color color) {
        MsdfFont msdfFont = fonts.get(font);
        msdfFont.applyGlyphs(text, size, 0, 0.3f, x, y + msdfFont.getMetrics().baselineHeight() * size, color, color, color, color, font, 1);
    }

    /**
     * Draws text with a horizontal gradient (left to right).
     */
    public void textGradient(String text, float x, float y, TextureUse font, float size, Color colorLeft, Color colorRight) {
        MsdfFont msdfFont = fonts.get(font);
        float totalWidth = msdfFont.getWidth(text, size);
        if (totalWidth <= 0f) return;
        float baseY = y + msdfFont.getMetrics().baselineHeight() * size;
        float curX = x;
        int prevChar = -1;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            var glyph = msdfFont.glyphs.get((int) ch);
            if (glyph == null) continue;
            var kerning = msdfFont.kernings.get(prevChar);
            if (kerning != null) {
                curX += kerning.getOrDefault((int) ch, 0.0f) * size;
            }
            float t = (curX - x) / totalWidth;
            Color c = lerpColor(colorLeft, colorRight, t);
            msdfFont.applyGlyphs(String.valueOf(ch), size, 0, 0.3f, curX, baseY, c, c, c, c, font, 1);
            curX += glyph.getWidth(size) + 0.3f;
            prevChar = ch;
        }
    }

    /**
     * Draws text with an animated running gradient.
     * offset — value 0..1, moves the wave across the text.
     */
    public void textAnimatedGradient(String text, float x, float y, TextureUse font, float size,
                                     Color[] colors, float offset) {
        if (colors == null || colors.length < 2) return;
        MsdfFont msdfFont = fonts.get(font);
        float totalWidth = msdfFont.getWidth(text, size);
        if (totalWidth <= 0f) return;
        float baseY = y + msdfFont.getMetrics().baselineHeight() * size;
        float curX = x;
        int prevChar = -1;
        int n = colors.length;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            var glyph = msdfFont.glyphs.get((int) ch);
            if (glyph == null) continue;
            var kerning = msdfFont.kernings.get(prevChar);
            if (kerning != null) {
                curX += kerning.getOrDefault((int) ch, 0.0f) * size;
            }
            // t — character position + animation offset, looped 0..1
            float t = ((curX - x) / totalWidth + offset) % 1.0f;
            if (t < 0) t += 1.0f;
            float scaled = t * (n - 1);
            int idx = (int) scaled;
            float frac = scaled - idx;
            Color c = lerpColor(colors[idx % n], colors[(idx + 1) % n], frac);
            msdfFont.applyGlyphs(String.valueOf(ch), size, 0, 0.3f, curX, baseY, c, c, c, c, font, 1);
            curX += glyph.getWidth(size) + 0.3f;
            prevChar = ch;
        }
    }

    public static Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
            (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
            (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t),
            (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
        );
    }

    public void text(Text text, float x, float y, TextureUse font, float size) {
        float offset = 0f;
        for (Text part : text.getWithStyle(text.getStyle())) {
            offset += textPart(part, x + offset, y, font, size);
        }
    }

    private float textPart(Text text, float x, float y, TextureUse font, float size) {
        List<Text> parts = text.getWithStyle(text.getStyle());
        if (parts.size() <= 1) {
            String rendered = Formatting.strip(text.getString());
            if (rendered == null || rendered.isEmpty()) {
                return 0f;
            }

            Color color = text.getStyle().getColor() == null
                    ? Color.WHITE
                    : new Color(text.getStyle().getColor().getRgb(), false);
            this.text(rendered, x, y, font, size, color);
            return textWidth(rendered, font, size);
        }

        float offset = 0f;
        for (Text part : parts) {
            offset += textPart(part, x + offset, y, font, size);
        }
        return offset;
    }

    public void text(IconUse icon, float x, float y, TextureUse font, float size, Color color) {
        MsdfFont msdfFont = fonts.get(font);
        msdfFont.applyGlyphs(icon.glyph, size, 0, 0.3f, x, y + msdfFont.getMetrics().baselineHeight() * size, color, color, color, color, font, 1);
    }

    public float textWidth(String text, TextureUse font, float size) {
        return fonts.get(font).getWidth(text, size);
    }

    public float textHeight(TextureUse font, float size) {
        return fonts.get(font).getHeight(size);
    }

    public void textCentered(String text, float x, float y, TextureUse font, float size, Color color) {
        float textWidth = textWidth(text, font, size);
        text(text, x - textWidth / 2f, y, font, size, color);
    }

    public void textCenteredStrict(String text, float x, float y, TextureUse font, float size, Color color) {
        float textWidth = textWidth(text, font, size);
        text(text, x - textWidth / 2f - 1, y - textHeight(font, size) / 2f, font, size, color);
    }

    public void textCenteredPixelPerfect(String text, float centerX, float centerY, TextureUse font, float size, Color color) {
        float drawX = Math.round(centerX - textWidth(text, font, size) / 2f);
        float drawY = Math.round(centerY - textHeight(font, size) / 2f);
        text(text, drawX, drawY, font, size, color);
    }

    public void outline(float x, float y, float width, float height, float thickness, Vector4f round, Vector2f smooth, Color color1, Color color2, Color color3, Color color4) {
        crenderSystem.shader(ShaderUse.OUTLINE)
                .rect(x, y, width, height)
                .round(round.x, round.y, round.z, round.w)
                .smoothness(smooth.x, smooth.y)
                .thickness(thickness)
                .color(color1, color2, color3, color4)
                .build();
    }

    public void drawHudRect(float x, float y, float width, float height) {
        Color bg = new Color(12, 12, 12, 160);
        rect(x, y, width, height, new Vector4f(10), 1, bg, bg, bg, bg);
    }

    public void backIcon(IconUse icon, float x, float y, float rectSize, float iconXOff, float iconYOff, float iconSize, Color color1, Color color2) {
        float rectX = Math.round(x + 4f);
        float rectY = Math.round(y + 4f);
        // No background rect — icon only
        float centerX = rectX + rectSize / 2f + iconXOff;
        float centerY = rectY + rectSize / 2f + iconYOff;
        textCenteredPixelPerfect(icon.glyph, centerX, centerY, TextureUse.ICONS, iconSize, color1);
    }

    public void backIcon(IconUse icon, float x, float y) {
        backIcon(icon, x, y, 12, 0f, 0f, 8f, new Color(201, 31, 210, 255), new Color(101, 0, 110, 255));
    }

    public void texture(Identifier id, float x, float y, float width, float height, float smooth, Vector4f round, Color color1, Color color2, Color color3, Color color4) {
        CRenderSystem system = Elexis.getInstance().testRender.getCrenderSystem();
        TextureAtlas.UV uv = system.getAtlas().getOrCreate(id.toString(), id);
        if (uv == null) return;
        float a = 1e-3F;
        system.shader(ShaderUse.TEXTURE).smoothness(smooth, smooth).uv(uv.u0 + a, uv.v0 + a, uv.u1 - a, uv.v1 - a)
                .color(color1, color2, color3, color4).rect(x, y, width, height).build();
    }

    public void texture(Identifier id, float x, float y, float width, float height, float smooth, Vector4f round, Color color) {
        CRenderSystem system = Elexis.getInstance().testRender.getCrenderSystem();
        TextureAtlas.UV uv = system.getAtlas().getOrCreate(id.toString(), id);
        if (uv == null) return;
        float a = 1e-3F;
        system.shader(ShaderUse.TEXTURE).smoothness(smooth, smooth).uv(uv.u0 + a, uv.v0 + a, uv.u1 - a, uv.v1 - a)
                .color(color, color, color, color).rect(x, y, width, height).build();
    }

    public void texture(Identifier id, float x, float y, float width, float height, float smooth, Vector4f uv2, Vector4f round, Color color1, Color color2, Color color3, Color color4) {
        CRenderSystem system = Elexis.getInstance().testRender.getCrenderSystem();
        TextureAtlas.UV uv = system.getAtlas().getOrCreate(id.toString(), id);
        if (uv == null) return;
        float a = 1e-3F;

        float ux = uv.u0 + a;
        float uy = uv.v0 + a;
        float uw = uv.u1 - a;
        float uh = uv.v1 - a;

        system.shader(ShaderUse.TEXTURE).smoothness(smooth, smooth)
                .uv(ux + (uv2.x / uv.width), uy + (uv2.y / uv.height), uw - ((1F - uv2.z) / uv.width), uh - (1F - uv2.z) / uv.height)
                .round(round.x, round.y, round.z, round.w)
                .color(color1, color2, color3, color4).rect(x, y, width, height).build();
    }

    public void textureRaw(Identifier id, float x, float y, float width, float height, float smooth, Vector4f round, Color color1, Color color2, Color color3, Color color4) {
        CRenderSystem system = Elexis.getInstance().testRender.getCrenderSystem();
        TextureAtlas.UV uv = system.getAtlas().getOrCreate(id.toString(), id);
        if (uv == null) return;
        system.shader(ShaderUse.TEXTURE).smoothness(smooth, smooth).uv(uv.u0, uv.v0, uv.u1, uv.v1).round(round.x, round.y, round.z, round.w)
                .color(color1, color2, color3, color4).rect(x, y, width, height).build();
    }
    public void textureRaw(Identifier id, float x, float y, float width, float height, float smooth, Vector4f round, Vector4f tex, Color color1, Color color2, Color color3, Color color4) {
        CRenderSystem system = Elexis.getInstance().testRender.getCrenderSystem();
        TextureAtlas.UV uv = system.getAtlas().getOrCreate(id.toString(), id);
        if (uv == null) return;
        system.shader(ShaderUse.TEXTURE).smoothness(smooth, smooth).uv(uv.u0 + tex.x, uv.v0 + tex.y, tex.z, tex.w).round(round.x, round.y, round.z, round.w)
                .color(color1, color2, color3, color4).rect(x, y, width, height).build();
    }

    public void textureRegion(Identifier id, float x, float y, float width, float height, float smooth, Vector4f round,
                              float regionX, float regionY, float regionWidth, float regionHeight,
                              Color color1, Color color2, Color color3, Color color4) {
        textureRegion(id, x, y, width, height, smooth, round, regionX, regionY, regionWidth, regionHeight,
                color1, color2, color3, color4, false);
    }

    public boolean textureRegionBottomLeft(Identifier id, float x, float y, float width, float height, float smooth, Vector4f round,
                                           float regionX, float regionY, float regionWidth, float regionHeight,
                                           Color color1, Color color2, Color color3, Color color4) {
        return textureRegion(id, x, y, width, height, smooth, round, regionX, regionY, regionWidth, regionHeight,
                color1, color2, color3, color4, false);
    }

    public boolean textureRegionTopLeft(Identifier id, float x, float y, float width, float height, float smooth, Vector4f round,
                                        float regionX, float regionY, float regionWidth, float regionHeight,
                                        Color color1, Color color2, Color color3, Color color4) {
        return textureRegion(id, x, y, width, height, smooth, round, regionX, regionY, regionWidth, regionHeight,
                color1, color2, color3, color4, true);
    }
    public boolean textureRegionDirectTopLeft(Identifier id, float x, float y, float width, float height, float smooth, Vector4f round,
                                              float regionX, float regionY, float regionWidth, float regionHeight,
                                              Color color1, Color color2, Color color3, Color color4) {
        try {
            AbstractTexture texture = mc.getTextureManager().getTexture(id);
            int glId = ((GlTextureView) texture.getGlTextureView()).texture().getGlId();
            int[] size = crenderSystem.getAtlas().getSize(glId);
            int textureWidth = size[0] > 0 ? size[0] : 64;
            int textureHeight = size[1] > 0 ? size[1] : 64;

            float u = regionX / textureWidth;
            float v = regionY / textureHeight;
            float regionU = regionWidth / textureWidth;
            float regionV = regionHeight / textureHeight;
            float a = 1e-4F;

            crenderSystem.layerTex(glId);
            crenderSystem.shader(ShaderUse.TEXTURE).smoothness(smooth, smooth)
                    .uv(u + a, v + a, regionU - a * 2f, regionV - a * 2f)
                    .round(round.x, round.y, round.z, round.w)
                    .color(color1, color2, color3, color4)
                    .rect(x, y, width, height).build();
            return true;
        } catch (Exception ignored) {
            crenderSystem.addPrepare(id);
            return false;
        }
    }

    public boolean refreshAtlasTexture(Identifier id) {
        try {
            AbstractTexture texture = mc.getTextureManager().getTexture(id);
            int glId = ((GlTextureView) texture.getGlTextureView()).texture().getGlId();
            int[] size = crenderSystem.getAtlas().getSize(glId);
            if (size[0] <= 0 || size[1] <= 0) return false;
            String name = id.toString();
            if (crenderSystem.getAtlas().has(name)) {
                crenderSystem.getAtlas().updateTextureFromGL(name, glId);
            } else {
                crenderSystem.getAtlas().addTextureFromGL(name, glId);
            }
            return true;
        } catch (Exception ignored) {
            crenderSystem.addPrepare(id);
            return false;
        }
    }


    private boolean textureRegion(Identifier id, float x, float y, float width, float height, float smooth, Vector4f round,
                                  float regionX, float regionY, float regionWidth, float regionHeight,
                                  Color color1, Color color2, Color color3, Color color4, boolean topLeftSource) {
        CRenderSystem system = Elexis.getInstance().testRender.getCrenderSystem();
        TextureAtlas atlas = system.getAtlas();
        TextureAtlas.UV uv = atlas.getOrCreate(id.toString(), id);
        if (uv == null) return false;

        float sourceY = topLeftSource ? uv.height - regionY - regionHeight : regionY;
        float u = uv.u0 + regionX / atlas.atlasWidth;
        float v = uv.v0 + sourceY / atlas.atlasHeight;
        float regionU = regionWidth / atlas.atlasWidth;
        float regionV = regionHeight / atlas.atlasHeight;
        float a = 1e-4F;

        system.layerTex(atlas.getGlId());
        system.shader(ShaderUse.TEXTURE).smoothness(smooth, smooth)
                .uv(u + a, v + a, regionU - a * 2f, regionV - a * 2f)
                .round(round.x, round.y, round.z, round.w)
                .color(color1, color2, color3, color4)
                .rect(x, y, width, height).build();
        return true;
    }
    public void texture(Item item, float x, float y, float width, float height, float smooth, Vector4f round, Color color1, Color color2, Color color3, Color color4) {
        textureRaw(item instanceof BlockItem ? Registries.ITEM.getId(item).withPrefixedPath("textures/block/").withSuffixedPath(".png") : Registries.ITEM.getId(item).withPrefixedPath("textures/item/").withSuffixedPath(".png"), x, y, width, height, smooth, round, color1, color2, color3, color4);
    }

    public void toCamera(MatrixStack matrixStack) {
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        matrixStack.translate(-cam.x, -cam.y, -cam.z);
    }
    public void toWorld(MatrixStack matrixStack) {
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        matrixStack.translate(cam.x, cam.y, cam.z);
    }
}
