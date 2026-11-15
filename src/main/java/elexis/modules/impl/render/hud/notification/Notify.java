package elexis.modules.impl.render.hud.notification;

import elexis.Elexis;
import elexis.client.ClientColors;
import elexis.client.utils.animations.Direction;
import elexis.client.utils.animations.impl.SmoothStepAnimation;
import elexis.client.utils.render.main.oldrender.system.IconUse;
import elexis.client.utils.render.main.oldrender.system.ShaderUse;
import elexis.client.utils.render.main.oldrender.system.TextureUse;
import elexis.client.utils.render.main.oldrender.system.sys2d.CRenderSystem;
import elexis.client.utils.render.main.oldrender.system.sys2d.TextureAtlas;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;

import java.awt.Color;

public class Notify {
    private final Text text;
    private IconUse icon;
    private final Color accent;
    private final long duration;
    private final String item;

    private float x;
    private float y;
    private float height = 28f;
    private float scale = 1f;
    private float lerpX = -999f;
    private float lerpY = -999f;

    @Getter
    private final SmoothStepAnimation animation;

    private boolean isDemo = false;

    private final IconUse[] demoIcons = new IconUse[]{
            IconUse.WARN, IconUse.INFO, IconUse.CHECK, IconUse.REFRESH
    };

    public Notify markDemo() {
        this.isDemo = true;
        return this;
    }

    public Notify(Text text, IconUse icon, Color accent, long duration) {
        this.text = Text.empty().append(text);
        this.icon = icon;
        this.accent = accent;
        this.duration = System.currentTimeMillis() + duration;
        this.item = null;
        this.animation = new SmoothStepAnimation(300, 1);
        this.animation.reset();
    }

    public Notify(Text text, String item, Color accent, long duration) {
        this.text = Text.empty().append(text);
        this.icon = null;
        this.accent = accent;
        this.item = item;
        this.duration = System.currentTimeMillis() + duration;
        this.animation = new SmoothStepAnimation(300, 1);
        this.animation.reset();
    }

    public Notify bound(float x, float y, float width, float height) {
        return bound(x, y, width, height, 1f);
    }

    public Notify bound(float x, float y, float width, float height, float scale) {
        this.x = x;
        this.y = y;
        this.height = Math.max(20f * scale, height);
        this.scale = scale;
        return this;
    }

    public float getPreferredWidth() {
        String value = text.getString();
        float textSize = 9f * scale;
        float textWidth = Elexis.getInstance().testRender.textWidth(value, TextureUse.SFMEDIUM, textSize);
        return textWidth + 37f * scale;
    }

    public boolean shouldRemove() {
        return System.currentTimeMillis() >= this.duration;
    }

    public void render(int mouseX, int mouseY) {
        if (lerpX == -999f || lerpY == -999f) {
            lerpX = x;
            lerpY = y;
        }
        lerpX = linearFps(lerpX, x, 15f);
        lerpY = linearFps(lerpY, y, 15f);

        if (isDemo) {
            icon = demoIcons[(int) (System.currentTimeMillis() / 1000L) % demoIcons.length];
        }

        if (!isDemo) {
            animation.setDirection(duration - System.currentTimeMillis() > animation.getDuration()
                    ? Direction.FORWARDS
                    : Direction.BACKWARDS);
        }

        boolean hasIcon = icon != null;
        boolean hasItem = item != null;
        float textSize = 6.85f * scale;
        float iconSize = 6.75f * scale;
        float width = Math.max(88f * scale, Elexis.getInstance().testRender.textWidth(text.getString(), TextureUse.SFMEDIUM, textSize) + 21f * scale);
        float left = lerpX - width / 2f;
        float top = lerpY;
        float iconBlock = 14f * scale;
        float round = 4f * scale;
        Color bg1 = new Color(18, 21, 27, 245);
        Color bg2 = new Color(37, 42, 53, 245);
        Color stroke = new Color(255, 255, 255, 18);
        Color iconColor = accent == null ? new Color(82, 190, 255, 255) : accent;

        CRenderSystem system = Elexis.getInstance().testRender.getCrenderSystem();
        float prevAlpha = system.alpha();
        system.alpha(prevAlpha * (float) animation.getOutput());

        Elexis.getInstance().testRender.rect(left, top, width, height, new Vector4f(round), 1f, bg1, bg1, bg2, bg2);
        Elexis.getInstance().testRender.rect(left + iconBlock - 1f * scale, top + 1f * scale, 0.5f * scale, height - 1.5f * scale, new Vector4f(0f), 1f,
                new Color(166, 166, 166, 255), new Color(166, 166, 166, 255), new Color(166, 166, 166, 255), new Color(166, 166, 166, 255));
        Elexis.getInstance().testRender.outline(left, top, width, height, 0.45f * scale, new Vector4f(round), new org.joml.Vector2f(1f), stroke, stroke, stroke, stroke);

        if (hasIcon) {
            Elexis.getInstance().testRender.textCenteredStrict(icon.glyph, left + iconBlock / 2f, top + height / 2f, TextureUse.ICONS, iconSize, iconColor);
        } else if (hasItem) {
            drawItemIcon(system, left + 3.15f * scale, top + 2f * scale, 8f * scale, iconColor);
        }

        drawNotifyText(left + iconBlock + 3f * scale, top + (height - textSize) / 2f - 0.2f * scale, textSize, iconColor);

        system.alpha(prevAlpha);
    }

    private void drawNotifyText(float x, float y, float size, Color accentColor) {
        String value = text.getString();
        int split = value.lastIndexOf(' ');
        if (split > 0) {
            String first = value.substring(0, split);
            String last = value.substring(split + 1);
            boolean stateWord = last.equalsIgnoreCase("enabled") || last.equalsIgnoreCase("disabled") || last.equalsIgnoreCase("loaded")
                    || last.equalsIgnoreCase("Р Р†Р С”Р В»РЎР‹РЎвЂЎРЎвЂР Р…") || last.equalsIgnoreCase("Р Р†Р С”Р В»РЎР‹РЎвЂЎР ВµР Р…") || last.equalsIgnoreCase("Р Р†РЎвЂ№Р С”Р В»РЎР‹РЎвЂЎР ВµР Р…")
                    || last.equalsIgnoreCase("Р Р†РЎвЂ№Р С”Р В»РЎР‹РЎвЂЎРЎвЂР Р…") || last.equalsIgnoreCase("Р В·Р В°Р С–РЎР‚РЎС“Р В¶Р ВµР Р…");
            if (stateWord) {
                Elexis.getInstance().testRender.text(first, x, y, TextureUse.SFMEDIUM, size, accentColor);
                float firstWidth = Elexis.getInstance().testRender.textWidth(first + " ", TextureUse.SFMEDIUM, size);
                Elexis.getInstance().testRender.text(last, x + firstWidth, y, TextureUse.SFMEDIUM, size, ClientColors.FORE_COLOR);
                return;
            }
        }
        Elexis.getInstance().testRender.text(value, x, y, TextureUse.SFMEDIUM, size, ClientColors.FORE_COLOR);
    }

    private void drawItemIcon(CRenderSystem system, float x, float y, float size, Color color) {
        Identifier identifier = Identifier.ofVanilla("textures/item/" + item + ".png");
        if (system.getAtlas().has(identifier.toString())) {
            TextureAtlas.UV uv = system.getAtlas().getUV(identifier.toString());
            system.shader(ShaderUse.TEXTURE)
                    .alpha((float) animation.getOutput())
                    .smoothness(1, 1)
                    .rect(x, y, size, size)
                    .uv(uv.u0, uv.v0, uv.u1, uv.v1)
                    .color(Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE)
                    .build();
        } else {
            system.addPrepare(identifier);
            Elexis.getInstance().testRender.textCenteredStrict(IconUse.CUBE.glyph, x + size / 2f, y + size / 2f, TextureUse.ICONS, size, color);
        }
    }

    private static float linearFps(float start, float end, float speed) {
        return start + (end - start) * Math.min(1f, speed / Math.max(1f, MinecraftClient.getInstance().getCurrentFps()));
    }
}
