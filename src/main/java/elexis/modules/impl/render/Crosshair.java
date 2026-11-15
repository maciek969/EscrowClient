package elexis.modules.impl.render;

import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.SliderSetting;

@FunctionAdd(
        name = "Crosshair",
        alias = "Crosshair",
        category = Category.Render,
        description = "Changes the crosshair"
)
public class Crosshair extends Function {
    private final SliderSetting gap = new SliderSetting("Gap", 1.5F, 0.1F, 6.0F, 0.1F);
    private final SliderSetting expand = new SliderSetting("Expand Power", 3.0F, 0.6F, 6.0F, 0.1F);
    private final SliderSetting length = new SliderSetting("Length", 3.0F, 0.4F, 6.0F, 0.1F);
    private final SliderSetting thickness = new SliderSetting("Width", 1.0F, 0.5F, 2.0F, 0.1F);
    private final ModeSetting centerDot = new ModeSetting("Center Dot", "No", "Yes", "No");

    private float animationSize;

    public Crosshair() {
        addSettings(gap, expand, length, thickness, centerDot);
    }

    @EventHandler
    public void onRender(EventRender.Screen.Hud event) {
        if (nullCheck() || mc.options.hudHidden) {
            return;
        }

        float x = event.getViewportWidth() / 2.0F;
        float y = event.getViewportHeight() / 2.0F;
        renderClassicCrosshair(event, x, y);
    }

    private void renderClassicCrosshair(EventRender.Screen.Hud event, float x, float y) {
        animationSize = fast(animationSize, 1.0F - mc.player.getAttackCooldownProgress(0.0F), 10.0F);

        float lineThickness = thickness.get();
        float lineLength = length.get();
        float lineGap = gap.get() + expand.get() * animationSize;
        int color = mc.targetedEntity != null ? 0xFFFF0000 : 0xFFFFFFFF;
        float halfThickness = lineThickness / 2.0F;

        drawOutlined(event, x - halfThickness, y - lineGap - lineLength, lineThickness, lineLength, color);
        drawOutlined(event, x - halfThickness, y + lineGap, lineThickness, lineLength, color);
        drawOutlined(event, x - lineGap - lineLength, y - halfThickness, lineLength, lineThickness, color);
        drawOutlined(event, x + lineGap, y - halfThickness, lineLength, lineThickness, color);

        if (centerDot.is("Yes")) {
            drawOutlined(event, x - 0.5F, y - 0.5F, 1.0F, 1.0F, color);
        }
    }

    private void drawOutlined(EventRender.Screen.Hud event, float x, float y, float w, float h, int color) {
        event.getRenderer().rect(x - 0.5F, y - 0.5F, w + 1.0F, h + 1.0F, 0xFF000000);
        event.getRenderer().rect(x, y, w, h, color);
    }

    private float fast(float current, float target, float speed) {
        return current + (target - current) / Math.max(1.0F, speed);
    }
}
