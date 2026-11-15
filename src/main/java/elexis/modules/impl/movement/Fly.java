package elexis.modules.impl.movement;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionAdd(name = "Fly", alias = "Fly", category = Category.Movement, description = "Полет")
@Environment(EnvType.CLIENT)
public class Fly extends Function {
    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla");
    private final SliderSetting speed = new SliderSetting("Speed", 0.6F, 0.1F, 3.0F, 0.1F);
    private final SliderSetting verticalSpeed = new SliderSetting("Vertical Speed", 0.45F, 0.1F, 2.0F, 0.05F);

    public Fly() {
        this.addSettings(this.mode, this.speed, this.verticalSpeed);
    }

    @Override
    public void onDisable() {
        if (!nullCheck()) {
            mc.player.getAbilities().flying = false;
        }
        super.onDisable();
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck() || !this.mode.is("Vanilla")) {
            return;
        }

        double y = 0.0;
        if (mc.options.jumpKey.isPressed()) {
            y += this.verticalSpeed.get();
        }
        if (mc.options.sneakKey.isPressed()) {
            y -= this.verticalSpeed.get();
        }

        double[] motion = calculateDirection(this.speed.get());
        mc.player.setVelocity(motion[0], y, motion[1]);
        mc.player.fallDistance = 0.0F;
    }

    private static double[] calculateDirection(double distance) {
        float forward = mc.player.input.getMovementInput().y;
        float sideways = mc.player.input.getMovementInput().x;
        float yaw = mc.player.getYaw();
        if (forward != 0.0F) {
            if (sideways > 0.0F) {
                yaw += forward > 0.0F ? -45.0F : 45.0F;
            } else if (sideways < 0.0F) {
                yaw += forward > 0.0F ? 45.0F : -45.0F;
            }
            sideways = 0.0F;
            forward = forward > 0.0F ? 1.0F : -1.0F;
        }

        double sinYaw = Math.sin(Math.toRadians(yaw + 90.0F));
        double cosYaw = Math.cos(Math.toRadians(yaw + 90.0F));
        return new double[]{
                forward * distance * cosYaw + sideways * distance * sinYaw,
                forward * distance * sinYaw - sideways * distance * cosYaw
        };
    }
}
