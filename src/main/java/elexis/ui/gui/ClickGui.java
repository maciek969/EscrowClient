package elexis.ui.gui;

import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.animations.impl.SimpleAlphaAnimation;
import elexis.client.utils.render.main.newrender.text.FontRegistry;
import elexis.client.utils.text.RussianText;
import elexis.modules.api.Category;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static elexis.client.utils.client.IMinecraft.mc;

public class ClickGui extends Screen {
    private static final float OPEN_SCALE_START = 0.92f;
    private static final float OPEN_Y_OFFSET = 18f;

    static boolean isClosing = false;
    public static boolean opened = false;
    private final SimpleAlphaAnimation alpha = new SimpleAlphaAnimation(200);
    private final List<Panel> panels = new ArrayList<>();
    private float guiScale = 1.0f;
    private float activeGuiScale = 1.0f;
    private static String searchQuery = "";
    private boolean searchFocused = false;

    // panel positions (to pass into mouseClicked/scroll)
    private final List<float[]> panelPositions = new ArrayList<>(); // [x, y]

    public ClickGui() {
        super(Text.of(UUID.randomUUID().toString()));
        for (Category category : Category.values()) {
            panels.add(new Panel(category));
        }
    }

    @Override
    public void init() {
        super.init();
        opened = true;
        isClosing = false;
        alpha.show();
    }

    @EventHandler
    public void onRender(EventRender.Screen.Gui event) {
        if (!opened || mc.currentScreen != this) return;
        var render = event.getRenderer();
        var appearance = elexis.modules.impl.render.visual.SceneVisuals.active("ClickGui", elexis.modules.impl.render.visual.GuiVisuals.ClickGui.class);
        alpha.setDuration(appearance == null ? 200 : appearance.duration.get().intValue());
        float a = (float) alpha.get();
        float progress = alpha.getProgress();

        float sw = (float) mc.getWindow().getWidth();
        float sh = (float) mc.getWindow().getHeight();
        float gap = 6f;
        float totalWidth = (panels.size() * Panel.WIDTH) + ((panels.size() - 1) * gap);
        float maxWidth  = Math.max(1f, sw - 20f);
        float maxHeight = Math.max(1f, sh - 20f);

        guiScale = Math.min(1f, Math.min(maxWidth / totalWidth, maxHeight / Panel.HEIGHT));
        activeGuiScale = guiScale * (OPEN_SCALE_START + ((1f - OPEN_SCALE_START) * progress));

        double mx = toLogicalX(mc.mouse.getX(), sw);
        double my = toLogicalY(mc.mouse.getY(), sh);

        float startX = (sw - totalWidth) / 2f;
        float startY = (sh - Panel.HEIGHT) / 2f - 16f + (1f - progress) * OPEN_Y_OFFSET;

        float backgroundOpacity = appearance == null ? 140f : appearance.background.get();
        if (appearance != null && appearance.blur.get()) render.blur(0f, 0f, sw, sh, 0f, progress);
        int overlayAlpha = Math.min(220, Math.max(0, Math.round(backgroundOpacity * progress)));
        render.rect(0f, 0f, sw, sh, new java.awt.Color(0, 0, 0, overlayAlpha).getRGB());

        // update panel positions
        panelPositions.clear();
        float curX = startX;
        for (Panel panel : panels) {
            panelPositions.add(new float[]{curX, startY});
            curX += Panel.WIDTH + gap;
        }

        render.pushScale(activeGuiScale, activeGuiScale, sw / 2f, sh / 2f);
        try {
            for (int i = 0; i < panels.size(); i++) {
                float[] pos = panelPositions.get(i);
                panels.get(i).drawPanel(render, pos[0], pos[1], a, (int) mx, (int) my);
            }
            drawBottomControls(render, sw, sh, a);
        } finally {
            render.popScale();
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (isClosing) return true;

        float sw = (float) mc.getWindow().getWidth();
        float sh = (float) mc.getWindow().getHeight();
        double rawX = toLogicalX(mc.mouse.getX(), sw);
        double rawY = toLogicalY(mc.mouse.getY(), sh);
        int button = click.button();

        if (handleBottomClick(rawX, rawY, sw, sh, button)) return true;

        for (int i = 0; i < panels.size(); i++) {
            float[] pos = panelPositions.get(i);
            for (var comp : panels.get(i).getComponents()) {
                if (comp.isBindListening()) {
                    comp.mouseButtonPressed(rawX, rawY, button);
                    return true;
                }
            }
            if (panels.get(i).mouseClicked(rawX, rawY, button, pos[0], pos[1])) {
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isClosing) return true;

        float sw = (float) mc.getWindow().getWidth();
        float sh = (float) mc.getWindow().getHeight();
        double rawX = toLogicalX(mc.mouse.getX(), sw);
        double rawY = toLogicalY(mc.mouse.getY(), sh);
        for (int i = 0; i < panels.size(); i++) {
            float[] pos = panelPositions.get(i);
            if (panels.get(i).mouseScrolled(rawX, rawY, pos[0], pos[1], verticalAmount)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (mc.currentScreen != this) return false;
        int keyCode = input.key();

        if (isClosing) return true;

        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { searchFocused = false; return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1).toLowerCase(java.util.Locale.ROOT);
                return true;
            }
            String typed = keyToSearchText(keyCode);
            if (!typed.isEmpty()) {
                searchQuery = (searchQuery + typed).toLowerCase(java.util.Locale.ROOT);
                return true;
            }
        }

        for (Panel panel : panels) {
            for (var comp : panel.getComponents()) {
                if (comp.isBindListening()) {
                    comp.keyPressed(keyCode);
                    return true;
                }
            }
        }

        for (Panel panel : panels) panel.keyPressed(keyCode);

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (!isClosing) {
                isClosing = true;
                alpha.hide();
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (mc.currentScreen != this && opened) close();
        if (isClosing && alpha.isFinished()) { close(); isClosing = false; }
    }

    @Override
    public void close() {
        opened = false;
        activeGuiScale = guiScale;
        super.close();
    }
    public static String getSearchQuery() {
        return searchQuery == null ? "" : searchQuery;
    }

    private String keyToSearchText(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) return Character.toString((char) ('a' + keyCode - GLFW.GLFW_KEY_A));
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) return Character.toString((char) ('0' + keyCode - GLFW.GLFW_KEY_0));
        if (keyCode == GLFW.GLFW_KEY_SPACE) return " ";
        if (keyCode == GLFW.GLFW_KEY_MINUS) return "-";
        return "";
    }

    private void drawBottomControls(elexis.client.utils.render.main.newrender.core.Renderer2D render, float sw, float sh, float alpha) {
        int a = Math.max(0, Math.min(255, (int) alpha));
        float langX = 18f;
        float langY = sh - 42f;
        float langW = 104f;
        float langH = 26f;
        render.rect(langX, langY, langW, langH, 7f, new java.awt.Color(16, 16, 22, (int)(a * 0.86f)).getRGB());
        render.rectOutline(langX, langY, langW, langH, 7f, new java.awt.Color(255, 255, 255, (int)(a * 0.18f)).getRGB(), 1f);
        String lang = RussianText.isRussian() ? "RU" : "English";
        render.text(FontRegistry.SF_MEDIUM, langX + langW / 2f, langY + 17f, 11f, lang, 0xFFFFFFFF, "c");

        float searchW = 260f;
        float searchH = 28f;
        float searchX = (sw - searchW) / 2f;
        float searchY = sh - 76f;
        render.rect(searchX, searchY, searchW, searchH, 8f, new java.awt.Color(16, 16, 22, (int)(a * 0.88f)).getRGB());
        render.rectOutline(searchX, searchY, searchW, searchH, 8f, new java.awt.Color(255, 255, 255, (int)(a * (searchFocused ? 0.35f : 0.16f))).getRGB(), 1f);
        String text = searchQuery.isEmpty() ? RussianText.tr("Search") : searchQuery;
        int color = searchQuery.isEmpty() ? new java.awt.Color(135, 135, 150, a).getRGB() : 0xFFFFFFFF;
        render.text(FontRegistry.SF_MEDIUM, searchX + 12f, searchY + 18f, 11f, text, color);
    }

    private boolean handleBottomClick(double mx, double my, float sw, float sh, int button) {
        if (button != 0) return false;
        float langX = 18f, langY = sh - 42f, langW = 104f, langH = 26f;
        if (mx >= langX && mx <= langX + langW && my >= langY && my <= langY + langH) {
            RussianText.setRussian(!RussianText.isRussian());
            searchFocused = false;
            return true;
        }
        float searchW = 260f, searchH = 28f, searchX = (sw - searchW) / 2f, searchY = sh - 76f;
        searchFocused = mx >= searchX && mx <= searchX + searchW && my >= searchY && my <= searchY + searchH;
        return searchFocused;
    }

    @Override public boolean shouldPause() { return false; }
    @Override protected void applyBlur(DrawContext context) {}
    @Override protected void renderDarkening(DrawContext context) {}

    private double toLogicalX(double scaledX, float sw) {
        return (sw / 2.0) + ((scaledX - (sw / 2.0)) / activeGuiScale);
    }

    private double toLogicalY(double scaledY, float sh) {
        return (sh / 2.0) + ((scaledY - (sh / 2.0)) / activeGuiScale);
    }
}
