package elexis.ui.gui;

import elexis.Elexis;
import elexis.client.ClientColors;
import elexis.client.utils.render.main.newrender.core.Renderer2D;
import elexis.client.utils.render.main.newrender.text.FontRegistry;
import elexis.client.utils.text.RussianText;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.ui.gui.elements.FunctionComponent;
import lombok.Getter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Panel {
    public static final float WIDTH    = 220f;
    public static final float HEIGHT   = 520f;
    private static final float HEADER_H      = 52f;
    private static final float ITEM_GAP      = 5f;
    private static final float SCROLL_SPEED  = 30f;
    private static final float SCROLL_ANIM   = 14f;
    private static final float CORNER        = 14f;

    public Category category;

    @Getter
    private final List<FunctionComponent> components = new ArrayList<>();

    private float scrollOffset       = 0f;
    private float targetScrollOffset = 0f;
    private long  lastScrollNanos    = System.nanoTime();
    private boolean isDragging       = false;
    private float dragStartScroll    = 0f;

    public Panel(Category category) {
        this.category = category;
        refreshComponents();
    }

    public void refreshComponents() {
        components.clear();
        for (Function f : Elexis.getFunctionManager().getFunctions()) {
            if (f.getCategory() == category) {
                components.add(new FunctionComponent(f, WIDTH - 16f));
            }
        }
    }

    // в”Ђв”Ђ Scroll helpers в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    private float totalContentHeight() {
        float h = 0f;
        for (FunctionComponent c : components) if (matchesSearch(c)) h += c.getFullHeight() + ITEM_GAP;
        return h > 0f ? h - ITEM_GAP : 0f;
    }

    private float getMaxScroll() {
        return Math.max(0f, totalContentHeight() - (HEIGHT - HEADER_H - 8f));
    }

    private void tickScroll() {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastScrollNanos) / 1_000_000_000f);
        lastScrollNanos = now;
        float max = getMaxScroll();
        targetScrollOffset = Math.max(0f, Math.min(max, targetScrollOffset));
        float diff = targetScrollOffset - scrollOffset;
        if (Math.abs(diff) > 0.01f) scrollOffset += diff * Math.min(1f, dt * SCROLL_ANIM);
        else scrollOffset = targetScrollOffset;
        scrollOffset = Math.max(0f, Math.min(max, scrollOffset));
    }

    // в”Ђв”Ђ Draw в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    public void drawPanel(Renderer2D render, float x, float y, float alpha, int mouseX, int mouseY) {
        float a01 = alpha / 255f;
        int   ai  = (int) alpha;

        // РўРµРЅСЊ РїРѕРґ РїР°РЅРµР»СЊСЋ
        render.shadow(x, y, WIDTH, HEIGHT, CORNER, 18f, 2f,
                new Color(0, 0, 0, (int)(a01 * 80)).getRGB());

        // Blur С„РѕРЅ
        render.prepareBlur(14f);
        render.blur(x, y, WIDTH, HEIGHT, CORNER);

        // РћСЃРЅРѕРІРЅРѕР№ С„РѕРЅ
        render.rect(x, y, WIDTH, HEIGHT, CORNER,
                new Color(8, 8, 12, (int)(a01 * 210)).getRGB());

        // РўРѕРЅРєР°СЏ РѕР±РІРѕРґРєР°
        render.rectOutline(x, y, WIDTH, HEIGHT, CORNER,
                new Color(255, 255, 255, (int)(a01 * 18)).getRGB(), 1f);

        Color c1 = ClientColors.getMainColor();
        Color c2 = ClientColors.getSecondaryColor();

        // в”Ђв”Ђ Р—Р°РіРѕР»РѕРІРѕРє в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

        // Р¤РѕРЅ Р·Р°РіРѕР»РѕРІРєР°
        render.rect(x, y, WIDTH, HEADER_H, CORNER, CORNER, 0f, 0f,
                new Color(15, 15, 20, (int)(a01 * 180)).getRGB());

        // Р Р°Р·РґРµР»РёС‚РµР»СЊ Р·Р°РіРѕР»РѕРІРєР°
        render.gradient(x + 12f, y + HEADER_H - 1f, WIDTH - 24f, 1f, 0f,
                new Color(255, 255, 255, 0).getRGB(),
                new Color(255, 255, 255, (int)(a01 * 35)).getRGB(),
                new Color(255, 255, 255, (int)(a01 * 35)).getRGB(),
                new Color(255, 255, 255, 0).getRGB());

        // РРєРѕРЅРєР° РєР°С‚РµРіРѕСЂРёРё
        String icon = category.getIcon();
        float iconSize = 16f;
        float iconX = x + 16f;
        float iconY = y + HEADER_H / 2f + FontRegistry.centeredBaselineOffset(FontRegistry.ICONS, 'A', iconSize);
        render.text(FontRegistry.ICONS, iconX, iconY, iconSize, icon,
                withAlpha(c1, ai));

        // РќР°Р·РІР°РЅРёРµ РєР°С‚РµРіРѕСЂРёРё
        String title = RussianText.tr(category.name());
        float titleSize = 15f;
        float titleX = x + 16f + iconSize + 8f;
        float titleY = y + HEADER_H / 2f + FontRegistry.centeredBaselineOffset(FontRegistry.SF_SEMIBOLD, 'A', titleSize);
        render.text(FontRegistry.SF_SEMIBOLD, titleX, titleY, titleSize, title,
                new Color(240, 240, 248, ai).getRGB());

        // РЎС‡С‘С‚С‡РёРє РјРѕРґСѓР»РµР№ (СЃРїСЂР°РІР°)
        long enabled = components.stream().filter(c -> matchesSearch(c) && c.function.isState()).count();
        long visibleCount = components.stream().filter(this::matchesSearch).count();
        String countStr = enabled + "/" + visibleCount;
        float countSize = 11f;
        float countW = render.measureText(FontRegistry.SF_MEDIUM, countStr, countSize).width;
        float countX = x + WIDTH - countW - 14f;
        float countY = y + HEADER_H / 2f + FontRegistry.centeredBaselineOffset(FontRegistry.SF_MEDIUM, 'A', countSize);
        render.text(FontRegistry.SF_MEDIUM, countX, countY, countSize, countStr,
                new Color(120, 120, 135, (int)(a01 * 180)).getRGB());

        // в”Ђв”Ђ РљРѕРЅС‚РµРЅС‚ в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

        float contentY = y + HEADER_H;
        float contentH = HEIGHT - HEADER_H;
        float totalH   = totalContentHeight();
        float maxScroll = getMaxScroll();
        tickScroll();

        render.pushClipRect((int) x, (int) contentY, (int) WIDTH, (int) contentH);
        try {
            float curY = contentY + 6f - scrollOffset;
            for (FunctionComponent comp : components) {
                if (!matchesSearch(comp)) continue;
                comp.draw(render, x + 8f, curY, mouseX, mouseY, alpha);
                curY += comp.getFullHeight() + ITEM_GAP;
            }
        } finally {
            render.popClipRect();
        }

        // РЎРєСЂРѕР»Р»Р±Р°СЂ вЂ” С‚РѕРЅРєРёР№, СЃ РіСЂР°РґРёРµРЅС‚РѕРј
        if (totalH > contentH) {
            float bw  = 2.5f;
            float bx  = x + WIDTH - bw - 4f;
            float ratio = contentH / totalH;
            float bh  = Math.max(24f, contentH * ratio);
            float by  = contentY + (maxScroll > 0 ? (scrollOffset / maxScroll) * (contentH - bh) : 0f);

            // РўСЂРµРє
            render.rect(bx, contentY + 6f, bw, contentH - 12f, bw / 2f,
                    new Color(255, 255, 255, (int)(a01 * 8)).getRGB());

            // РџРѕР»Р·СѓРЅРѕРє СЃ РіСЂР°РґРёРµРЅС‚РѕРј С†РІРµС‚Р° С‚РµРјС‹
            render.gradient(bx, by, bw, bh, bw / 2f,
                    withAlpha(c1, (int)(a01 * 180)),
                    withAlpha(c1, (int)(a01 * 180)),
                    withAlpha(c2, (int)(a01 * 120)),
                    withAlpha(c2, (int)(a01 * 120)));
        }

        // РќРёР¶РЅРёР№ fade-out
        render.gradient(x, y + HEIGHT - 28f, WIDTH, 28f,
                CORNER, CORNER, CORNER, CORNER,
                new Color(0, 0, 0, 0).getRGB(),
                new Color(0, 0, 0, 0).getRGB(),
                new Color(8, 8, 12, (int)(a01 * 180)).getRGB(),
                new Color(8, 8, 12, (int)(a01 * 180)).getRGB());
    }

    // в”Ђв”Ђ Input в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    public boolean mouseScrolled(double mx, double my, double px, double py, double amount) {
        if (mx < px || mx > px + WIDTH || my < py || my > py + HEIGHT) return false;
        float max = getMaxScroll();
        tickScroll();
        float contentY = (float) py + HEADER_H;
        float curY = contentY - scrollOffset;
        for (FunctionComponent comp : components) {
            if (!matchesSearch(comp)) continue;
            if (my >= curY && my <= curY + comp.getFullHeight()) {
                if (comp.mouseScrolled(mx, my, amount)) return true;
                break;
            }
            curY += comp.getFullHeight() + ITEM_GAP;
        }
        if (totalContentHeight() <= HEIGHT - HEADER_H) return false;
        targetScrollOffset = Math.max(0f, Math.min(max, targetScrollOffset - (float) amount * SCROLL_SPEED));
        return true;
    }

    public boolean mouseDragged(double mx, double my, int btn, double px, double py, double dx, double dy) {
        if (isDragging && btn == 0) {
            float max = getMaxScroll();
            targetScrollOffset = Math.max(0f, Math.min(max, dragStartScroll - (float) dy * 1.5f));
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double mx, double my, int btn, double px, double py) {
        if (mx < px || mx > px + WIDTH || my < py + HEADER_H || my > py + HEIGHT) return false;
        tickScroll();
        float contentY = (float) py + HEADER_H;
        float curY = contentY - scrollOffset;
        for (FunctionComponent comp : components) {
            if (!matchesSearch(comp)) continue;
            if (my >= curY && my <= curY + comp.getFullHeight()) {
                if (comp.isBindListening()) return comp.mouseButtonPressed(mx, my, btn);
                return comp.mouseClicked(mx, my, btn);
            }
            curY += comp.getFullHeight() + ITEM_GAP;
        }
        return false;
    }

    public void mouseReleased(int btn) {
        if (btn == 0) isDragging = false;
    }

    public FunctionComponent findHoveredComponent(double mx, double my, double px, double py) {
        if (mx < px || mx > px + WIDTH || my < py + HEADER_H || my > py + HEIGHT) return null;
        tickScroll();
        float contentY = (float) py + HEADER_H;
        float curY = contentY - scrollOffset;
        for (FunctionComponent comp : components) {
            if (!matchesSearch(comp)) continue;
            if (my >= curY && my <= curY + comp.getFullHeight()) return comp;
            curY += comp.getFullHeight() + ITEM_GAP;
        }
        return null;
    }

    public void keyPressed(int key) {
        for (FunctionComponent comp : components) comp.keyPressed(key);
    }

    public float getPanelScrollOffset() { return scrollOffset; }
    public float getHeaderH()           { return HEADER_H; }
    public void  resetScroll()          { scrollOffset = 0f; targetScrollOffset = 0f; }

    // в”Ђв”Ђ Helpers в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    private boolean matchesSearch(FunctionComponent comp) {
        String query = ClickGui.getSearchQuery();
        return query.isEmpty() || comp.function.getAlias().toLowerCase(java.util.Locale.ROOT).contains(query);
    }

    private static int withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a))).getRGB();
    }
}
