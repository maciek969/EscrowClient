package elexis.client.utils.render.main.newrender.text;

import elexis.client.utils.render.main.newrender.core.Renderer2D;
import elexis.client.utils.render.main.newrender.gl.GlBackend;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class FontRegistry {
    private static final Map<String, MsdfFont> REGISTERED_FONTS = new HashMap<>();
    private static final Map<String, FontObject> FONT_OBJECTS = new HashMap<>();

    private static GlBackend backend;
    private static boolean backendConfigured = false;
    private static boolean rendererFontsInitialized = false;

    public static FontObject SF_MEDIUM;
    public static FontObject ICONS;
    public static FontObject WATERMARK_ICON;
    public static FontObject SF_SEMIBOLD;
    public static FontObject INTER_MEDIUM;
    public static FontObject INTER_SEMIBOLD;

    private FontRegistry() {
    }

    public static synchronized void initialize(GlBackend glBackend, Renderer2D renderer) {
        configureBackend(glBackend);
        Objects.requireNonNull(renderer, "renderer");
        if (rendererFontsInitialized) {
            return;
        }
        renderer.registerTextRenderer(SF_MEDIUM, createTextRenderer(SF_MEDIUM));
        renderer.registerTextRenderer(ICONS, createTextRenderer(ICONS));
        renderer.registerTextRenderer(WATERMARK_ICON, createTextRenderer(WATERMARK_ICON));
        renderer.registerTextRenderer(SF_SEMIBOLD, createTextRenderer(SF_SEMIBOLD));
        renderer.registerTextRenderer(INTER_MEDIUM, createTextRenderer(INTER_MEDIUM));
        renderer.registerTextRenderer(INTER_SEMIBOLD, createTextRenderer(INTER_SEMIBOLD));
        rendererFontsInitialized = true;
    }

    public static synchronized FontObject register(String id, String jsonResourcePath, String textureResourcePath) {
        ensureBackendConfigured();
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(jsonResourcePath, "jsonResourcePath");
        Objects.requireNonNull(textureResourcePath, "textureResourcePath");
        if (REGISTERED_FONTS.containsKey(id)) {
            throw new IllegalStateException("Font already registered: " + id);
        }
        MsdfFont font = MsdfFont.load(backend, jsonResourcePath, textureResourcePath);
        REGISTERED_FONTS.put(id, font);
        FontObject fontObject = new FontObject(id);
        FONT_OBJECTS.put(id, fontObject);
        return fontObject;
    }

    public static synchronized TextRenderer createTextRenderer(FontObject fontObject) {
        ensureBackendConfigured();
        MsdfFont msdfFont = resolve(fontObject);
        return new TextRenderer(backend, msdfFont);
    }

    /**
     * Computes the vertical baseline offset required to align the visual center of the specified glyph
     * with the provided anchor position when rendering at the given size. The result represents the
     * amount that must be added to the anchor's Y coordinate in order to place the glyph's bounding box
     * center at that anchor.
     *
     * @param fontObject font containing the glyph
     * @param codepoint  Unicode codepoint of the glyph to analyze
     * @param size       rendering size in pixels
     * @return baseline offset measured in pixels; zero when the glyph is unavailable
     */
    public static synchronized float centeredBaselineOffset(FontObject fontObject, int codepoint, float size) {
        ensureBackendConfigured();
        if (fontObject == null || size <= 0f) {
            return 0f;
        }
        MsdfFont font = resolve(fontObject);
        MsdfFont.Glyph glyph = font.glyph(codepoint);
        if (glyph == null || !glyph.renderable) {
            return 0f;
        }
        float emSize = Math.max(1.0e-6f, font.emSize());
        float scale = size / emSize;
        return (glyph.planeTop + glyph.planeBottom) * 0.5f * scale;
    }

    public static synchronized FontObject get(String id) {
        ensureBackendConfigured();
        FontObject fontObject = FONT_OBJECTS.get(id);
        if (fontObject == null) {
            throw new IllegalArgumentException("Font not registered: " + id);
        }
        return fontObject;
    }

    static synchronized MsdfFont resolve(FontObject fontObject) {
        ensureBackendConfigured();
        MsdfFont font = REGISTERED_FONTS.get(fontObject.id);
        if (font == null) {
            throw new IllegalStateException("Font not registered: " + fontObject.id);
        }
        return font;
    }

    private static void configureBackend(GlBackend glBackend) {
        Objects.requireNonNull(glBackend, "backend");
        if (backendConfigured) {
            if (backend != glBackend) {
                throw new IllegalStateException("FontRegistry already initialized with a different backend instance");
            }
            return;
        }
        backend = glBackend;
        backendConfigured = true;
        registerBuiltinFonts();
    }

    private static void registerBuiltinFonts() {
        SF_MEDIUM = register(
                "inter_medium",
                "assets/elexis/fonts/sfprodisplaymedium.json",
                "assets/elexis/fonts/sfprodisplaymedium.png");

        ICONS = register(
                "icons",
                "assets/elexis/fonts/icons.json",
                "assets/elexis/fonts/icons.png");

        WATERMARK_ICON = register(
                "watermark_icon",
                "assets/elexis/fonts/icon.json",
                "assets/elexis/fonts/icon.png");

        SF_SEMIBOLD = register(
                "sfprodisplaysemibold",
                "assets/elexis/fonts/sfprodisplaysemibold.json",
                "assets/elexis/fonts/sfprodisplaysemibold.png");

        INTER_MEDIUM = register(
                "inter_medium_legacy",
                "assets/elexis/fonts/medium.json",
                "assets/elexis/fonts/medium.png");

        INTER_SEMIBOLD = register(
                "inter_semibold_legacy",
                "assets/elexis/fonts/semibold.json",
                "assets/elexis/fonts/semibold.png");

    }

    private static void ensureBackendConfigured() {
        if (!backendConfigured || backend == null) {
            throw new IllegalStateException("FontRegistry.initialize(backend, renderer) must be called before use");
        }
    }
}
