package elexis.ui.gui.elements;

import elexis.client.ClientColors;
import elexis.client.utils.player.PlayerUtils;
import elexis.client.utils.render.main.newrender.core.Renderer2D;
import elexis.client.utils.render.main.newrender.text.FontRegistry;
import elexis.client.utils.text.RussianText;
import elexis.modules.api.Function;
import elexis.modules.api.settings.api.Setting;
import elexis.modules.api.settings.impl.BindSetting;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ButtonSetting;
import elexis.modules.api.settings.impl.ModeListSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FunctionComponent {
    public static final float HEADER_H = 36f;
    private static final float CARD_R  = 10f;

    public final Function function;
    public float x, y, width;

    private boolean expanded      = false;
    private boolean bindListening = false;

    // Р С’Р Р…Р С‘Р СР В°РЎвЂ Р С‘РЎРЏ РЎР‚Р В°РЎРѓР С”РЎР‚РЎвЂ№РЎвЂљР С‘РЎРЏ Р Р…Р В°РЎРѓРЎвЂљРЎР‚Р С•Р ВµР С”
    private float expandAnim = 0f;
    private long  lastAnimNanos = System.nanoTime();

    // Р С’Р Р…Р С‘Р СР В°РЎвЂ Р С‘РЎРЏ Р Р†Р С”Р В»РЎР‹РЎвЂЎР ВµР Р…Р С‘РЎРЏ (0=Р Р†РЎвЂ№Р С”Р В», 1=Р Р†Р С”Р В»)
    private float enableAnim = 0f;

    private final List<SettingComponent<?>> settingComponents = new ArrayList<>();

    public FunctionComponent(Function function, float width) {
        this.function = function;
        this.width    = width;
        enableAnim    = function.isState() ? 1f : 0f;

        for (Setting<?> s : function.getSettings()) {
            if      (s instanceof BooleanSetting  b) settingComponents.add(new BooleanComp(b, width));
            else if (s instanceof ButtonSetting   bt) settingComponents.add(new ButtonComp(bt, width));
            else if (s instanceof SliderSetting   sl) settingComponents.add(new SliderComp(sl, width));
            else if (s instanceof ModeSetting     m)  settingComponents.add(new ModeComp(m, width));
            else if (s instanceof BindSetting     bi) settingComponents.add(new BindComp(bi, width));
            else if (s instanceof ModeListSetting ml) settingComponents.add(new ModeListComp(ml, width));
        }
    }

    // РІвЂќР‚РІвЂќР‚ Heights РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚

    public float getFullHeight() {
        if (settingComponents.isEmpty()) return HEADER_H;
        return HEADER_H + (settingsContentH() + 10f) * expandProgress();
    }

    private float settingsContentH() {
        float total = 0f;
        int   vis   = 0;
        float cw    = cardW() - 20f;
        for (SettingComponent<?> c : settingComponents) {
            if (!c.setting.visible.get()) continue;
            c.setLayoutWidth(cw);
            total += c.height;
            vis++;
        }
        if (vis > 1) total += (vis - 1) * 8f;
        return total + 10f;
    }

    // РІвЂќР‚РІвЂќР‚ Draw РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚

    public void draw(Renderer2D render, float x, float y, int mouseX, int mouseY, float alpha) {
        this.x = x;
        this.y = y;

        tickAnims();

        float a01  = alpha / 255f;
        int   ai   = (int) alpha;
        float cw   = cardW();
        float cx   = x + 2f;
        float ep   = expandProgress();
        float fh   = HEADER_H + (settingComponents.isEmpty() ? 0f : (settingsContentH() + 10f) * ep);

        Color mainC = ClientColors.getMainColor();
        Color secC  = ClientColors.getSecondaryColor();

        // Р СћР ВµР Р…РЎРЉ Р С”Р В°РЎР‚РЎвЂљР С•РЎвЂЎР С”Р С‘
        render.shadow(cx, y, cw, fh, CARD_R, 6f, 0f,
                new Color(0, 0, 0, (int)(a01 * 30)).getRGB());

        // Р В¤Р С•Р Р… Р С”Р В°РЎР‚РЎвЂљР С•РЎвЂЎР С”Р С‘ РІР‚вЂќ РЎвЂљР ВµР СР Р…Р ВµР Вµ Р ВµРЎРѓР В»Р С‘ Р Р†РЎвЂ№Р С”Р В»РЎР‹РЎвЂЎР ВµР Р…, РЎвЂЎРЎС“РЎвЂљРЎРЉ РЎРѓР Р†Р ВµРЎвЂљР В»Р ВµР Вµ Р ВµРЎРѓР В»Р С‘ Р Р†Р С”Р В»РЎР‹РЎвЂЎРЎвЂР Р…
        int bgAlpha = (int)(a01 * (function.isState() ? 28 : 18));
        render.rect(cx, y, cw, fh, CARD_R,
                new Color(255, 255, 255, bgAlpha).getRGB());

        // Р вЂєР ВµР Р†Р В°РЎРЏ РЎвЂ Р Р†Р ВµРЎвЂљР Р…Р В°РЎРЏ Р С—Р С•Р В»Р С•РЎРѓР С”Р В° Р С—РЎР‚Р С‘ Р Р†Р С”Р В»РЎР‹РЎвЂЎР ВµР Р…Р С‘Р С‘
        if (enableAnim > 0.01f) {
            float barW = 3f;
            float barH = HEADER_H - 12f;
            float barY = y + 6f;
            int barAlpha = (int)(a01 * enableAnim * 220);
            render.gradient(cx, barY, barW, barH, barW / 2f,
                    withAlpha(mainC, barAlpha),
                    withAlpha(mainC, barAlpha),
                    withAlpha(secC,  barAlpha),
                    withAlpha(secC,  barAlpha));
        }

        // Р С›Р В±Р Р†Р С•Р Т‘Р С”Р В° РІР‚вЂќ РЎвЂ Р Р†Р ВµРЎвЂљР Р…Р В°РЎРЏ Р С—РЎР‚Р С‘ Р Р†Р С”Р В»РЎР‹РЎвЂЎР ВµР Р…Р С‘Р С‘, РЎвЂљРЎвЂР СР Р…Р В°РЎРЏ Р С—РЎР‚Р С‘ Р Р†РЎвЂ№Р С”Р В»РЎР‹РЎвЂЎР ВµР Р…Р С‘Р С‘
        if (enableAnim > 0.01f) {
            int outlineAlpha = (int)(a01 * enableAnim * 60);
            render.rectOutline(cx, y, cw, fh, CARD_R,
                    withAlpha(mainC, outlineAlpha), 1f);
        } else {
            render.rectOutline(cx, y, cw, fh, CARD_R,
                    new Color(255, 255, 255, (int)(a01 * 10)).getRGB(), 1f);
        }

        // РІвЂќР‚РІвЂќР‚ Р вЂ”Р В°Р С–Р С•Р В»Р С•Р Р†Р С•Р С” РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚

        float textSize = 13f;
        float textY    = y + HEADER_H / 2f + FontRegistry.centeredBaselineOffset(FontRegistry.SF_MEDIUM, 'A', textSize);

        // Р ВР СРЎРЏ Р СР С•Р Т‘РЎС“Р В»РЎРЏ
        int nameAlpha;
        if (bindListening) {
            nameAlpha = ai;
        } else {
            nameAlpha = function.isState()
                    ? ai
                    : (int)(a01 * 140);
        }
        int nameColor = function.isState()
                ? new Color(240, 240, 248, nameAlpha).getRGB()
                : new Color(160, 160, 172, nameAlpha).getRGB();

        float nameX = cx + 12f;
        if (enableAnim > 0.01f) nameX += 5f; // Р Р…Р ВµР В±Р С•Р В»РЎРЉРЎв‚¬Р С•Р в„– Р С•РЎвЂљРЎРѓРЎвЂљРЎС“Р С— Р Т‘Р В»РЎРЏ Р С—Р С•Р В»Р С•РЎРѓР С”Р С‘

        render.text(FontRegistry.SF_MEDIUM, nameX, textY, textSize, function.getAlias(), nameColor);

        // Р СџРЎР‚Р В°Р Р†Р В°РЎРЏ РЎвЂЎР В°РЎРѓРЎвЂљРЎРЉ: Р В±Р С‘Р Р…Р Т‘ Р С‘Р В»Р С‘ "bind..."
        float rightX = cx + cw - 12f;

        if (bindListening) {
            String bt = "bind...";
            float bts = 11f;
            float btw = render.measureText(FontRegistry.SF_MEDIUM, bt, bts).width;
            float bty = y + HEADER_H / 2f + FontRegistry.centeredBaselineOffset(FontRegistry.SF_MEDIUM, 'A', bts);
            render.text(FontRegistry.SF_MEDIUM, rightX - btw, bty - 0.5f, bts, bt,
                    new Color(255, 100, 100, ai).getRGB());
            rightX -= btw + 6f;
        } else if (function.getBind() != 0) {
            String bk  = bindName(function.getBind());
            float  bks = 10f;
            float  bkw = render.measureText(FontRegistry.SF_MEDIUM, bk, bks).width;
            float  bky = y + HEADER_H / 2f + FontRegistry.centeredBaselineOffset(FontRegistry.SF_MEDIUM, 'A', bks);
            // Р СљР В°Р В»Р ВµР Р…РЎРЉР С”Р С‘Р в„– РЎвЂЎР С‘Р С— Р Т‘Р В»РЎРЏ Р В±Р С‘Р Р…Р Т‘Р В°
            float chipPad = 5f;
            float chipW   = bkw + chipPad * 2f;
            float chipH   = 16f;
            float chipX   = rightX - chipW;
            float chipY   = y + (HEADER_H - chipH) / 2f;
            render.rect(chipX, chipY, chipW, chipH, 5f,
                    new Color(255, 255, 255, (int)(a01 * 12)).getRGB());
            render.text(FontRegistry.SF_MEDIUM, chipX + chipPad, bky, bks, bk,
                    new Color(160, 160, 175, (int)(a01 * 160)).getRGB());
            rightX = chipX - 6f;
        }

        // Р РЋРЎвЂљРЎР‚Р ВµР В»Р С”Р В° РЎР‚Р В°РЎРѓР С”РЎР‚РЎвЂ№РЎвЂљР С‘РЎРЏ Р Р…Р В°РЎРѓРЎвЂљРЎР‚Р С•Р ВµР С”
        if (!settingComponents.isEmpty()) {
            float arrowSize = 10f;
            float arrowX    = rightX - arrowSize;
            float arrowY    = y + HEADER_H / 2f + FontRegistry.centeredBaselineOffset(FontRegistry.ICONS, 'A', arrowSize);
            // Р ВРЎРѓР С—Р С•Р В»РЎРЉР В·РЎС“Р ВµР С РЎРѓР С‘Р СР Р†Р С•Р В» РЎРѓРЎвЂљРЎР‚Р ВµР В»Р С”Р С‘ Р С‘Р В· Р С‘Р С”Р С•Р Р…Р С•Р С” (j = DOWN, k = UP)
            String arrowGlyph = ep > 0.5f ? "k" : "j";
            int arrowAlpha = (int)(a01 * (ep > 0.5f ? 200 : 120));
            render.text(FontRegistry.ICONS, arrowX, arrowY, arrowSize, arrowGlyph,
                    new Color(200, 200, 210, arrowAlpha).getRGB());
        }

        // РІвЂќР‚РІвЂќР‚ Р СњР В°РЎРѓРЎвЂљРЎР‚Р С•Р в„–Р С”Р С‘ РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚

        if (ep > 0.001f && !settingComponents.isEmpty()) {
            float visH    = (settingsContentH() + 10f) * ep;
            float padX    = 10f;
            float padTop  = 8f;
            float gap     = 8f;
            float cntW    = cw - padX * 2f;
            float curY    = y + HEADER_H + padTop - (1f - ep) * 6f;
            float cntAlpha = alpha * ep;

            // Р В Р В°Р В·Р Т‘Р ВµР В»Р С‘РЎвЂљР ВµР В»РЎРЉ
            render.gradient(cx + 10f, y + HEADER_H, cw - 20f, 1f, 0f,
                    new Color(255, 255, 255, 0).getRGB(),
                    new Color(255, 255, 255, (int)((cntAlpha / 255f) * 30)).getRGB(),
                    new Color(255, 255, 255, (int)((cntAlpha / 255f) * 30)).getRGB(),
                    new Color(255, 255, 255, 0).getRGB());

            render.pushClipRect((int) cx, (int)(y + HEADER_H), (int) Math.ceil(cw), (int) Math.ceil(visH + 2f));
            try {
                boolean first = true;
                for (SettingComponent<?> comp : settingComponents) {
                    if (!comp.setting.visible.get()) continue;
                    if (!first) {
                        render.rect(cx + padX, curY - gap / 2f - 0.5f, cntW, 1f, 0f,
                                new Color(255, 255, 255, (int)((cntAlpha / 255f) * 12)).getRGB());
                    }
                    first = false;
                    comp.x = cx + padX;
                    comp.y = curY;
                    comp.setLayoutWidth(cntW);
                    comp.draw(render, cx + padX, curY, mouseX, mouseY, cntAlpha);
                    curY += comp.height + gap;
                }
            } finally {
                render.popClipRect();
            }
        }
    }

    // РІвЂќР‚РІвЂќР‚ Input РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚

    public boolean mouseClicked(double mx, double my, int btn) {
        if (!inCard(mx, my)) return false;

        if (btn == 0 && inHeader(mx, my)) {
            function.toggle();
            return true;
        }
        if (btn == 2 && inHeader(mx, my)) {
            bindListening = true;
            return true;
        }
        if (btn == 1 && inHeader(mx, my) && !settingComponents.isEmpty()) {
            expanded = !expanded;
            return true;
        }
        if (expanded && !inHeader(mx, my)) {
            for (SettingComponent<?> c : settingComponents) {
                if (!c.setting.visible.get()) continue;
                if (c.mouseClicked(mx, my, btn)) return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!inCard(mx, my) || !expanded) return false;
        for (SettingComponent<?> c : settingComponents) {
            if (!c.setting.visible.get()) continue;
            if (c instanceof SliderComp sl && sl.isHovered(mx, my)) {
                float nv = Math.min(sl.setting.max, Math.max(sl.setting.min,
                        sl.setting.get() + (float) amount * sl.setting.increment));
                sl.setting.set(nv);
                return true;
            }
        }
        return false;
    }

    public void keyPressed(int key) {
        if (bindListening) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE) function.setBind(0);
            else function.setBind(key);
            bindListening = false;
            return;
        }
        for (SettingComponent<?> c : settingComponents) c.keyPressed(key);
    }

    public boolean mouseButtonPressed(double mx, double my, int btn) {
        if (bindListening) {
            function.setBind(-(btn + 1));
            bindListening = false;
            return true;
        }
        return false;
    }

    public boolean isBindListening() { return bindListening; }
    public boolean isExpanded()      { return expanded; }
    public boolean hasSettings()     { return !settingComponents.isEmpty(); }

    public float getCardWidth()  { return cardW(); }
    public float getRowLeft()    { return x + 2f; }
    public float getRowTop()     { return y; }
    public float getRowWidth()   { return cardW(); }
    public float getRowHeight()  { return getFullHeight(); }

    // РІвЂќР‚РІвЂќР‚ Helpers РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚РІвЂќР‚

    private float cardW() { return width - 4f; }

    private float expandProgress() {
        if (settingComponents.isEmpty()) return 0f;
        tickAnims();
        float p = Math.max(0f, Math.min(1f, expandAnim));
        float inv = 1f - p;
        return 1f - inv * inv * inv; // ease-out cubic
    }

    private void tickAnims() {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastAnimNanos) / 1_000_000_000f);
        lastAnimNanos = now;

        // Expand
        float expandTarget = expanded ? 1f : 0f;
        expandAnim += (expandTarget - expandAnim) * Math.min(1f, dt * 16f);
        if (Math.abs(expandTarget - expandAnim) < 0.002f) expandAnim = expandTarget;

        // Enable
        float enableTarget = function.isState() ? 1f : 0f;
        enableAnim += (enableTarget - enableAnim) * Math.min(1f, dt * 12f);
        if (Math.abs(enableTarget - enableAnim) < 0.002f) enableAnim = enableTarget;
    }

    private boolean inCard(double mx, double my) {
        float cx = x + 2f;
        return mx >= cx && mx <= cx + cardW() && my >= y && my <= y + getFullHeight();
    }

    private boolean inHeader(double mx, double my) {
        float cx = x + 2f;
        return mx >= cx && mx <= cx + cardW() && my >= y && my <= y + HEADER_H;
    }

    private static String bindName(int bind) {
        if (bind == 0) return "";
        return PlayerUtils.getBindName(bind).toLowerCase(Locale.ROOT);
    }

    private static int withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a))).getRGB();
    }
}
