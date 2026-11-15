package elexis.modules.impl.movement;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.player.MoveUtil;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionAdd(name = "Strafe", alias = "Strafe", category = Category.Movement, description = "Ускоряет и выравнивает стрейф")
@Environment(EnvType.CLIENT)
public class Strafe extends Function {
    private final ModeSetting mode = new ModeSetting("Режим", "Матрикс", "Матрикс", "Грим");
    private final SliderSetting speed = new SliderSetting("Скорость", 0.42F, 0.0F, 1.0F, 0.01F);

    public Strafe() {
        addSettings(mode, speed);
    }

    @EventHandler
    public void onUpdate(EventUpdate.Fast event) {
        if (nullCheck()) {
            return;
        }
        float[] movement = MoveUtil.getMovementFromKeys();
        if (movement[0] == 0.0F && movement[1] == 0.0F) {
            if (mode.is("Матрикс")) {
                mc.player.setVelocity(0.0D, mc.player.getVelocity().y, 0.0D);
            }
            return;
        }
        double direction = MoveUtil.direction(mc.player.getYaw(), movement[0], movement[1]);
        double motion = speed.get() * (mode.is("Матрикс") ? 1.5D : 1.0D);
        mc.player.setVelocity(-Math.sin(direction) * motion, mc.player.getVelocity().y, Math.cos(direction) * motion);
    }
}
