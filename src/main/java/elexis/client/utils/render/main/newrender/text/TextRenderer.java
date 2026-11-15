package elexis.client.utils.render.main.newrender.text;

import elexis.client.utils.render.main.newrender.gl.GlBackend;

import java.util.Objects;

public final class TextRenderer {
    private static final float[] IDENTITY_TRANSFORM = new float[]{
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f
    };

    private final GlBackend backend;
    private final MsdfFont font;

    public TextRenderer(GlBackend backend, MsdfFont font) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.font = Objects.requireNonNull(font, "font");
    }

    public void drawText(float x, float y, float size, String text, int rgbaPremul) {
        drawText(x, y, size, text, rgbaPremul, "l", IDENTITY_TRANSFORM);
    }

    public void drawText(float x, float y, float size, String text, int rgbaPremul, float[] transform) {
        drawText(x, y, size, text, rgbaPremul, "l", transform);
    }

    public void drawText(float x, float y, float size, String text, int rgbaPremul, String alignKey) {
        drawText(x, y, size, text, rgbaPremul, alignKey, IDENTITY_TRANSFORM);
    }

    public void drawText(float x, float y, float size, String text, int rgbaPremul, String alignKey, float[] transform) {
        if (size <= 0f) {
            return;
        }
        String content = text == null ? "" : text;
        if (content.isEmpty()) {
            return;
        }

        float[] matrix = (transform != null && transform.length >= 6) ? transform : IDENTITY_TRANSFORM;
        float scale = size / Math.max(1e-6f, font.emSize());
        float lineHeight = font.lineHeight() * scale;
        float baselineY = y;
        String align = alignKey == null ? "l" : alignKey.toLowerCase();
        int color = rgbaPremul;
        int texture = font.textureId();
        float pxRange = font.distanceRange();

        String[] lines = content.split("\\n", -1);
        for (String line : lines) {
            float width = measureLineWidth(line, scale);
            float startX = x;
            if ("c".equals(align)) {
                startX = x - width * 0.5f;
            } else if ("r".equals(align)) {
                startX = x - width;
            }
            drawTextLine(startX, baselineY, scale, line, color, matrix, texture, pxRange);
            baselineY += lineHeight;
        }
    }

    private void drawTextLine(float x,
                              float baseline,
                              float scale,
                              String line,
                              int color,
                              float[] matrix,
                              int texture,
                              float pxRange) {
        if (line.isEmpty()) {
            return;
        }
        float penX = x;
        float baselineY = baseline;
        int prevCodepoint = -1;

        for (int i = 0; i < line.length();) {
            char ch = line.charAt(i);
            if (ch == '\\' && i + 9 < line.length() && line.charAt(i + 1) == 'c') {
                i += 10;
                continue;
            }

            int cp = line.codePointAt(i);
            int cpLen = Character.charCount(cp);
            i += cpLen;

            MsdfFont.Glyph glyph = font.glyph(cp);
            int glyphCode = cp;
            if (glyph == null) {
                glyph = font.glyph('?');
                glyphCode = '?';
                if (glyph == null) {
                    continue;
                }
            }

            if (prevCodepoint != -1) {
                penX += font.kerning(prevCodepoint, glyphCode) * scale;
            }

            if (glyph.renderable) {
                float x0 = penX + glyph.planeLeft * scale;
                float y0 = baselineY - glyph.planeTop * scale;
                float x1 = penX + glyph.planeRight * scale;
                float y1 = baselineY - glyph.planeBottom * scale;
                float width = x1 - x0;
                float height = y1 - y0;
                if (width > 0f && height > 0f) {
                    backend.enqueueMsdfGlyph(texture, pxRange,
                            x0, y0, width, height,
                            // После flip в MsdfFont нужно поменять v0 и v1 местами
                            glyph.u0, glyph.v1, glyph.u1, glyph.v0,
                            color, matrix);
                }
            }

            penX += glyph.advance * scale;
            prevCodepoint = glyphCode;
        }
    }

    public TextMetrics measureText(String text, float size) {
        if (size <= 0f) {
            return new TextMetrics(0f, 0f);
        }
        String content = text == null ? "" : text;
        if (content.isEmpty()) {
            return new TextMetrics(0f, 0f);
        }
        float scale = size / Math.max(1e-6f, font.emSize());
        float lineHeight = font.lineHeight() * scale;
        String[] lines = content.split("\\n", -1);
        float maxWidth = 0f;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, measureLineWidth(line, scale));
        }
        float height = Math.max(lineHeight * lines.length, lineHeight);
        return new TextMetrics(maxWidth, height);
    }

    private float measureLineWidth(String line, float scale) {
        if (line.isEmpty()) {
            return 0f;
        }
        float penX = 0f;
        int prevCodepoint = -1;
        for (int i = 0; i < line.length();) {
            char ch = line.charAt(i);
            if (ch == '\\' && i + 9 < line.length() && line.charAt(i + 1) == 'c') {
                i += 10;
                continue;
            }
            int cp = line.codePointAt(i);
            int cpLen = Character.charCount(cp);
            i += cpLen;

            MsdfFont.Glyph glyph = font.glyph(cp);
            int glyphCode = cp;
            if (glyph == null) {
                glyph = font.glyph('?');
                glyphCode = '?';
                if (glyph == null) {
                    continue;
                }
            }

            if (prevCodepoint != -1) {
                penX += font.kerning(prevCodepoint, glyphCode) * scale;
            }
            penX += glyph.advance * scale;
            prevCodepoint = glyphCode;
        }
        return penX;
    }

    public static final class TextMetrics {
        public final float width;
        public final float height;

        public TextMetrics(float width, float height) {
            this.width = width;
            this.height = height;
        }
    }
}
