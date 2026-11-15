package elexis.ui.gui.elements;

import elexis.client.ClientColors;
import elexis.client.utils.text.RussianText;
import elexis.client.utils.render.main.newrender.core.Renderer2D;
import elexis.client.utils.render.main.newrender.text.FontObject;
import elexis.client.utils.render.main.newrender.text.FontRegistry;
import elexis.modules.api.settings.api.Setting;
import elexis.modules.api.settings.impl.*;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.Locale;

public abstract class SettingComponent<T extends Setting<?>> {
    public T setting;
    public float x, y, width, height;

    public SettingComponent(T setting, float width, float height) {
        this.setting = setting;
        this.width   = width;
        this.height  = height;
    }

    public abstract void draw(Renderer2D render, float x, float y, int mouseX, int mouseY, float alpha);
    public abstract boolean mouseClicked(double mouseX, double mouseY, int button);
    public void keyPressed(int key) {}
    public void setLayoutWidth(float w) { this.width = w; }

    public boolean isHovered(double mx, double my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    protected float baseline(Renderer2D render, FontObject font, float size, float rowTop, float rowH) {
        return rowTop + rowH / 2f + FontRegistry.centeredBaselineOffset(font, 'A', size);
    }

    protected String fitText(Renderer2D render, String text, float size, float maxWidth) {
        if (text == null) return "";
        if (render.measureText(FontRegistry.SF_MEDIUM, text, size).width <= maxWidth) return text;
        String e = "...";
        for (int i = text.length() - 1; i > 0; i--) {
            String c = text.substring(0, i) + e;
            if (render.measureText(FontRegistry.SF_MEDIUM, c, size).width <= maxWidth) return c;
        }
        return e;
    }

    protected String formatFloat(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001f) return Integer.toString(Math.round(value));
        return String.format(Locale.US, "%.1f", value);
    }

    protected static int withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a))).getRGB();
    }

    protected static int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        return ((int)(a1 + (a2 - a1) * t) << 24) | ((int)(r1 + (r2 - r1) * t) << 16)
             | ((int)(g1 + (g2 - g1) * t) << 8)  |  (int)(b1 + (b2 - b1) * t);
    }
}

// ─── Boolean — pretty toggle switch ──────────────────────────────────
class BooleanComp extends SettingComponent<BooleanSetting> {
    private static final float H       = 30f;
    private static final float KNOB_W  = 32f;
    private static final float KNOB_H  = 16f;

    // Toggle animation 0→1
    private float toggleAnim;
    private long  lastNanos = System.nanoTime();

    public BooleanComp(BooleanSetting s, float w) {
        super(s, w, H);
        toggleAnim = s.get() ? 1f : 0f;
    }

    @Override
    public void draw(Renderer2D render, float x, float y, int mouseX, int mouseY, float alpha) {
        this.x = x; this.y = y;
        int a = (int) alpha;

        // Animation
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        float target = setting.get() ? 1f : 0f;
        toggleAnim += (target - toggleAnim) * Math.min(1f, dt * 14f);

        float ts = 13f;
        float ty = baseline(render, FontRegistry.SF_MEDIUM, ts, y, H);
        render.text(FontRegistry.SF_MEDIUM, x, ty, ts,
                fitText(render, RussianText.tr(setting.getName()), ts, width - KNOB_W - 10f),
                new Color(200, 200, 210, a).getRGB());

        // Toggle track
        float tx = x + width - KNOB_W;
        float ty2 = y + (H - KNOB_H) / 2f;
        float r = KNOB_H / 2f;

        // Track — interpolate color from gray to the theme color
        Color mainC = ClientColors.getMainColor();
        int offColor = new Color(50, 50, 65, a).getRGB();
        int onColor  = withAlpha(mainC, a);
        int trackColor = lerpColor(offColor, onColor, toggleAnim);
        render.rect(tx, ty2, KNOB_W, KNOB_H, r, trackColor);

        // Toggle circle
        float circleD = KNOB_H - 4f;
        float circleX = tx + 2f + (KNOB_W - circleD - 4f) * toggleAnim;
        float circleY = ty2 + 2f;
        render.rect(circleX, circleY, circleD, circleD, circleD / 2f,
                new Color(255, 255, 255, a).getRGB());

        // Shadow under the circle
        render.shadow(circleX, circleY, circleD, circleD, circleD / 2f,
                3f, 0f, new Color(0, 0, 0, (int)(a * 0.3f)).getRGB());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (isHovered(mx, my) && b == 0) { setting.set(!setting.get()); return true; }
        return false;
    }
}

// ─── Slider — pretty with highlight ──────────────────────────────────────────
class SliderComp extends SettingComponent<SliderSetting> {
    private static final float H       = 44f;
    private static final float TRACK_H = 4f;
    private static final float KNOB_R  = 6f;
    private boolean dragging = false;

    public SliderComp(SliderSetting s, float w) { super(s, w, H); }

    @Override
    public void draw(Renderer2D render, float x, float y, int mouseX, int mouseY, float alpha) {
        this.x = x; this.y = y;
        int a = (int) alpha;

        float range = setting.max - setting.min;
        float pct   = range == 0 ? 0 : (setting.get() - setting.min) / range;

        if (dragging) {
            if (GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), 0) == 0) {
                dragging = false;
            } else {
                float p = Math.min(1f, Math.max(0f, ((float) mouseX - x) / width));
                float v = setting.min + range * p;
                setting.set((float)(Math.round(v / setting.increment) * setting.increment));
            }
        }

        float ts = 13f;
        float textRowH = H * 0.5f;
        float ty = baseline(render, FontRegistry.SF_MEDIUM, ts, y, textRowH);

        String valStr = formatFloat(setting.get());
        float valW = render.measureText(FontRegistry.SF_MEDIUM, valStr, ts).width;

        render.text(FontRegistry.SF_MEDIUM, x, ty, ts,
                fitText(render, RussianText.tr(setting.getName()), ts, width - valW - 8f),
                new Color(200, 200, 210, a).getRGB());

        // Value — theme color
        Color mainC = ClientColors.getMainColor();
        render.text(FontRegistry.SF_MEDIUM, x + width - valW, ty, ts,
                valStr, withAlpha(mainC, a));

        // Track
        float trackY = y + H - TRACK_H - 6f;
        // Track background
        render.rect(x, trackY, width, TRACK_H, TRACK_H / 2f,
                new Color(50, 50, 65, (int)(a * 0.5f)).getRGB());

        // Filled part — gradient
        if (pct > 0) {
            Color secC = ClientColors.getSecondaryColor();
            render.gradient(x, trackY, width * pct, TRACK_H, TRACK_H / 2f,
                    withAlpha(secC, a),
                    withAlpha(mainC, a),
                    withAlpha(mainC, a),
                    withAlpha(secC, a));
        }

        // Circle
        float kx = x + width * pct - KNOB_R;
        float ky = trackY + TRACK_H / 2f - KNOB_R;
        // Shadow under the circle
        render.shadow(kx, ky, KNOB_R * 2, KNOB_R * 2, KNOB_R,
                4f, 0f, new Color(0, 0, 0, (int)(a * 0.4f)).getRGB());
        // Circle
        render.rect(kx, ky, KNOB_R * 2, KNOB_R * 2, KNOB_R,
                new Color(255, 255, 255, a).getRGB());
        // Inner circle of the theme color
        float innerR = KNOB_R - 2.5f;
        render.rect(kx + 2.5f, ky + 2.5f, innerR * 2, innerR * 2, innerR,
                withAlpha(mainC, a));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        float trackY = y + H - TRACK_H - 6f;
        if (mx >= x && mx <= x + width && my >= trackY - 6 && my <= trackY + TRACK_H + 6 && b == 0) {
            dragging = true;
            return true;
        }
        return false;
    }
}

// ─── Mode — pretty chips for each mode ─────────────────────────────────
class ModeComp extends SettingComponent<ModeSetting> {
    private static final float HEADER_H = 26f;
    private static final float CHIP_H   = 22f;
    private static final float CHIP_GAP = 5f;
    private static final float CHIP_TS  = 11f;
    private static final float CHIP_PX  = 8f;

    // Animations for each chip
    private float[] chipAnims;
    private long lastNanos = System.nanoTime();

    public ModeComp(ModeSetting s, float w) {
        super(s, w, 26f + 27f); // initial height, recalculated on draw
        chipAnims = new float[s.strings.length];
        for (int i = 0; i < s.strings.length; i++) {
            chipAnims[i] = s.strings[i].equals(s.get()) ? 1f : 0f;
        }
    }

    private static float calcH(ModeSetting s, float w) {
        if (FontRegistry.SF_MEDIUM == null) return 26f + 27f;
        // Count how many rows the chips will take
        float lineX = 0f;
        int lines = 1;
        for (String mode : s.strings) {
            float cw = FontRegistry.SF_MEDIUM.getWidth(RussianText.tr(mode), CHIP_TS) + CHIP_PX * 2f;
            if (lineX > 0 && lineX + cw + CHIP_GAP > w) {
                lines++;
                lineX = 0f;
            }
            lineX += cw + CHIP_GAP;
        }
        return HEADER_H + lines * (CHIP_H + CHIP_GAP) + 4f;
    }

    @Override
    public void setLayoutWidth(float w) {
        super.setLayoutWidth(w);
        this.height = calcH(setting, w);
    }

    @Override
    public void draw(Renderer2D render, float x, float y, int mouseX, int mouseY, float alpha) {
        this.x = x; this.y = y;
        this.height = calcH(setting, width);
        int a = (int) alpha;

        // Animations
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        if (chipAnims == null || chipAnims.length != setting.strings.length) {
            chipAnims = new float[setting.strings.length];
        }
        for (int i = 0; i < setting.strings.length; i++) {
            float target = setting.strings[i].equals(setting.get()) ? 1f : 0f;
            chipAnims[i] += (target - chipAnims[i]) * Math.min(1f, dt * 14f);
        }

        // Name
        float ts = 13f;
        float ty = baseline(render, FontRegistry.SF_MEDIUM, ts, y, HEADER_H);
        render.text(FontRegistry.SF_MEDIUM, x, ty, ts,
                fitText(render, RussianText.tr(setting.getName()), ts, width),
                new Color(200, 200, 210, a).getRGB());

        // Chips
        Color mainC = ClientColors.getMainColor();
        Color secC  = ClientColors.getSecondaryColor();
        float cx = x;
        float cy = y + HEADER_H;

        for (int i = 0; i < setting.strings.length; i++) {
            String mode = setting.strings[i];
            float cw = render.measureText(FontRegistry.SF_MEDIUM, RussianText.tr(mode), CHIP_TS).width + CHIP_PX * 2f;

            if (cx > x && cx + cw > x + width) {
                cx = x;
                cy += CHIP_H + CHIP_GAP;
            }

            float anim = chipAnims[i];

            // Chip background — interpolate from dark to the theme color
            int bgOff = new Color(40, 40, 55, (int)(a * 0.4f)).getRGB();
            int bgOn  = withAlpha(mainC, (int)(a * 0.35f));
            int bgColor = lerpColor(bgOff, bgOn, anim);
            render.rect(cx, cy, cw, CHIP_H, CHIP_H / 2f, bgColor);

            // Border when active
            if (anim > 0.01f) {
                int outlineColor = withAlpha(mainC, (int)(a * anim * 0.6f));
                render.rectOutline(cx, cy, cw, CHIP_H, CHIP_H / 2f, outlineColor, 1f);
            }

            // Text
            int tcOff = new Color(140, 140, 155, a).getRGB();
            int tcOn  = new Color(240, 240, 248, a).getRGB();
            int textColor = lerpColor(tcOff, tcOn, anim);
            float textY2 = cy + CHIP_H / 2f + FontRegistry.centeredBaselineOffset(FontRegistry.SF_MEDIUM, 'A', CHIP_TS);
            render.text(FontRegistry.SF_MEDIUM, cx + CHIP_PX, textY2, CHIP_TS, RussianText.tr(mode), textColor);

            cx += cw + CHIP_GAP;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (mx < x || mx > x + width) return false;
        float cx = x;
        float cy = y + HEADER_H;

        for (int i = 0; i < setting.strings.length; i++) {
            String mode = setting.strings[i];
            float cw = FontRegistry.SF_MEDIUM.getWidth(RussianText.tr(mode), CHIP_TS) + CHIP_PX * 2f;
            if (cx > x && cx + cw > x + width) { cx = x; cy += CHIP_H + CHIP_GAP; }
            if (mx >= cx && mx <= cx + cw && my >= cy && my <= cy + CHIP_H && b == 0) {
                setting.set(mode);
                return true;
            }
            cx += cw + CHIP_GAP;
        }
        return false;
    }
}

// ─── ModeList — chips with multi-select ─────────────────────────────────────────
class ModeListComp extends SettingComponent<ModeListSetting> {
    private static final float HEADER_H = 26f;
    private static final float CHIP_H   = 22f;
    private static final float CHIP_GAP = 5f;
    private static final float CHIP_TS  = 11f;
    private static final float CHIP_PX  = 8f;

    public ModeListComp(ModeListSetting s, float w) { super(s, w, 26f + 27f); }

    private static float calcH(ModeListSetting s, float w) {
        if (FontRegistry.SF_MEDIUM == null) return 26f + 27f;
        float lineX = 0f;
        int lines = 1;
        for (BooleanSetting sub : s.get()) {
            float cw = FontRegistry.SF_MEDIUM.getWidth(RussianText.tr(sub.getName()), CHIP_TS) + CHIP_PX * 2f;
            if (lineX > 0 && lineX + cw + CHIP_GAP > w) { lines++; lineX = 0f; }
            lineX += cw + CHIP_GAP;
        }
        return HEADER_H + lines * (CHIP_H + CHIP_GAP) + 4f;
    }

    @Override
    public void setLayoutWidth(float w) {
        super.setLayoutWidth(w);
        this.height = calcH(setting, w);
    }

    @Override
    public void draw(Renderer2D render, float x, float y, int mouseX, int mouseY, float alpha) {
        this.x = x; this.y = y;
        this.height = calcH(setting, width);
        int a = (int) alpha;

        float ts = 13f;
        float ty = baseline(render, FontRegistry.SF_MEDIUM, ts, y, HEADER_H);
        render.text(FontRegistry.SF_MEDIUM, x, ty, ts,
                fitText(render, RussianText.tr(setting.getName()), ts, width),
                new Color(200, 200, 210, a).getRGB());

        Color mainC = ClientColors.getMainColor();
        float cx = x, cy = y + HEADER_H;

        for (BooleanSetting sub : setting.get()) {
            String lbl = RussianText.tr(sub.getName());
            float cw = render.measureText(FontRegistry.SF_MEDIUM, lbl, CHIP_TS).width + CHIP_PX * 2f;
            if (cx > x && cx + cw > x + width) { cx = x; cy += CHIP_H + CHIP_GAP; }

            boolean active = sub.get();
            int bgColor = active
                    ? withAlpha(mainC, (int)(a * 0.35f))
                    : new Color(40, 40, 55, (int)(a * 0.4f)).getRGB();
            render.rect(cx, cy, cw, CHIP_H, CHIP_H / 2f, bgColor);
            if (active) {
                render.rectOutline(cx, cy, cw, CHIP_H, CHIP_H / 2f,
                        withAlpha(mainC, (int)(a * 0.6f)), 1f);
            }
            int textColor = active
                    ? new Color(240, 240, 248, a).getRGB()
                    : new Color(140, 140, 155, a).getRGB();
            float textY2 = cy + CHIP_H / 2f + FontRegistry.centeredBaselineOffset(FontRegistry.SF_MEDIUM, 'A', CHIP_TS);
            render.text(FontRegistry.SF_MEDIUM, cx + CHIP_PX, textY2, CHIP_TS, lbl, textColor);
            cx += cw + CHIP_GAP;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (mx < x || mx > x + width) return false;
        float cx = x, cy = y + HEADER_H;
        for (BooleanSetting sub : setting.get()) {
            String lbl = RussianText.tr(sub.getName());
            float cw = FontRegistry.SF_MEDIUM.getWidth(lbl, CHIP_TS) + CHIP_PX * 2f;
            if (cx > x && cx + cw > x + width) { cx = x; cy += CHIP_H + CHIP_GAP; }
            if (mx >= cx && mx <= cx + cw && my >= cy && my <= cy + CHIP_H && b == 0) {
                sub.set(!sub.get()); return true;
            }
            cx += cw + CHIP_GAP;
        }
        return false;
    }
}


class BindComp extends SettingComponent<BindSetting> {
    private static final float H = 30f;
    private boolean listening = false;

    public BindComp(BindSetting s, float w) { super(s, w, H); }

    @Override
    public void draw(Renderer2D render, float x, float y, int mouseX, int mouseY, float alpha) {
        this.x = x; this.y = y;
        int a = (int) alpha;
        float ts = 13f;
        float ty = baseline(render, FontRegistry.SF_MEDIUM, ts, y, H);

        String raw = listening ? "..." : bindName(setting.get());
        String keyStr = raw.toLowerCase(Locale.ROOT);

        render.text(FontRegistry.SF_MEDIUM, x, ty, ts,
                fitText(render, RussianText.tr(setting.getName()), ts, width - 60f),
                new Color(200, 200, 210, a).getRGB());

        // Chip for the key
        float ks = 11f;
        float kw = render.measureText(FontRegistry.SF_MEDIUM, keyStr, ks).width;
        float chipPad = 6f;
        float chipW   = kw + chipPad * 2f;
        float chipH   = 18f;
        float chipX   = x + width - chipW;
        float chipY   = y + (H - chipH) / 2f;

        int chipBg = listening
                ? new Color(80, 30, 30, (int)(a * 0.5f)).getRGB()
                : new Color(40, 40, 55, (int)(a * 0.5f)).getRGB();
        render.rect(chipX, chipY, chipW, chipH, chipH / 2f, chipBg);

        Color mainC = ClientColors.getMainColor();
        int chipOutline = listening
                ? new Color(200, 60, 60, (int)(a * 0.6f)).getRGB()
                : withAlpha(mainC, (int)(a * 0.3f));
        render.rectOutline(chipX, chipY, chipW, chipH, chipH / 2f, chipOutline, 1f);

        float ky = y + H / 2f + FontRegistry.centeredBaselineOffset(FontRegistry.SF_MEDIUM, 'A', ks);
        int keyColor = listening
                ? new Color(255, 100, 100, a).getRGB()
                : new Color(200, 200, 215, a).getRGB();
        render.text(FontRegistry.SF_MEDIUM, chipX + chipPad, ky, ks, keyStr, keyColor);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (listening) {
            setting.set(-(b + 1));
            listening = false;
            return true;
        }
        if (isHovered(mx, my) && b == 0) { listening = !listening; return true; }
        return false;
    }

    @Override
    public void keyPressed(int key) {
        if (!listening) return;
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE) setting.set(0);
        else setting.set(key);
        listening = false;
    }

    private String bindName(int bind) {
        if (bind == 0) {
            return "none";
        }
        if (bind < 0) {
            int button = -(bind + 1);
            return switch (button) {
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "MOUSE3";
                case GLFW.GLFW_MOUSE_BUTTON_4 -> "MOUSE4";
                case GLFW.GLFW_MOUSE_BUTTON_5 -> "MOUSE5";
                default -> "MOUSE" + (button + 1);
            };
        }
        String name = GLFW.glfwGetKeyName(bind, 0);
        return name == null ? "key " + bind : name;
    }
}

class ButtonComp extends SettingComponent<ButtonSetting> {
    private static final float H = 30f;

    public ButtonComp(ButtonSetting s, float w) {
        super(s, w, H);
    }

    @Override
    public void draw(Renderer2D render, float x, float y, int mouseX, int mouseY, float alpha) {
        this.x = x;
        this.y = y;
        int a = (int) alpha;
        boolean hovered = isHovered(mouseX, mouseY);
        int bg = hovered ? new Color(85, 85, 105, (int)(a * 0.55f)).getRGB() : new Color(45, 45, 60, (int)(a * 0.48f)).getRGB();
        render.rect(x, y + 4f, width, H - 8f, 6f, bg);
        render.rectOutline(x, y + 4f, width, H - 8f, 6f, withAlpha(ClientColors.getMainColor(), (int)(a * 0.45f)), 1f);
        float ts = 12f;
        float tw = render.measureText(FontRegistry.SF_MEDIUM, RussianText.tr(setting.getName()), ts).width;
        float ty = baseline(render, FontRegistry.SF_MEDIUM, ts, y, H);
        render.text(FontRegistry.SF_MEDIUM, x + width / 2f - tw / 2f, ty, ts, RussianText.tr(setting.getName()), new Color(235, 235, 245, a).getRGB());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (isHovered(mx, my) && b == 0) {
            setting.press();
            return true;
        }
        return false;
    }
}
