package elexis.modules.impl.render;

import elexis.client.events.impl.entity.EventMotion;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

@FunctionAdd(name = "Gamma", alias = "Gamma", category = Category.Render, description = "Настраивает яркость")
public final class Gamma extends Function {
    private final ModeSetting mode = new ModeSetting("Режим света", "Dynamic", "Dynamic", "Fixed", "Potion");
    private final SliderSetting gamma = new SliderSetting("Гамма", 1.0F, 0.1F, 2.0F, 0.01F).hidden(() -> !mode.is("Fixed"));
    private final BooleanSetting nightVision = new BooleanSetting("Зелье ночного зрения", true).hidden(() -> !mode.is("Potion"));

    public Gamma() {
        addSettings(mode, gamma, nightVision);
    }

    @EventHandler
    public void onMotion(EventMotion event) {
        if (nullCheck()) {
            return;
        }

        if (mode.is("Potion")) {
            resetGamma();
            if (nightVision.get()) {
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 260, 0, false, false, false));
            }
        } else if (mode.is("Fixed")) {
            mc.options.getGamma().setValue((double) gamma.get());
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        } else {
            long time = mc.world.getTimeOfDay() % 24000L;
            float night = Math.abs(time - 12000L) / 12000.0F;
            mc.options.getGamma().setValue(1.0D + night * 1.0D);
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    @Override
    public void onDisable() {
        resetGamma();
        if (mc.player != null) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        super.onDisable();
    }

    private void resetGamma() {
        mc.options.getGamma().setValue(1.0D);
    }
}