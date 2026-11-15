package elexis.modules.impl.render;

import elexis.Elexis;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.main.newrender.core.Renderer2D;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionAdd(name = "FireLook", alias = "FireLook", category = Category.Render, description = "Stylized fire overlay")
@Environment(EnvType.CLIENT)
public final class FireLook extends Function {
    private final SliderSetting height = new SliderSetting("Height", 34.0F, 14.0F, 70.0F, 1.0F);
    private final SliderSetting alpha = new SliderSetting("Alpha", 82.0F, 20.0F, 150.0F, 1.0F);

    public FireLook() {
        this.addSettings(height, alpha);
    }

    @EventHandler
    public void onRender2D(EventRender.Screen.Hud event) {
        if (nullCheck() || NoRneder.is("Fire") || !mc.player.isOnFire()) {
            return;
        }

        Renderer2D renderer = event.getRenderer();
        float w = event.getViewportWidth();
        float h = event.getViewportHeight();
        float flameH = this.height.get();
        float time = mc.player.age * 0.16F;
        int baseAlpha = Math.round(this.alpha.get());

        for (int i = 0; i < 14; i++) {
            float partW = w / 13.0F;
            float x = i * (w / 13.0F) - partW * 0.25F;
            float wave = (float) Math.sin(time + i * 0.73F) * 5.0F;
            float localH = flameH * (0.55F + (i % 4) * 0.08F) + wave;
            int bottom = ((baseAlpha & 0xFF) << 24) | 0xFF7A00;
            int top = ((Math.max(10, baseAlpha / 3) & 0xFF) << 24) | 0xFFD545;
            renderer.gradient(x, h - localH, partW, localH, 3.0F, top, top, bottom, bottom);
        }
    }

    public static boolean shouldReplaceVanillaFire() {
        if (Elexis.getFunctionManager() == null) {
            return false;
        }
        FireLook fireLook = Elexis.getFunctionManager().getFireLook();
        return fireLook != null && fireLook.isState();
    }
}