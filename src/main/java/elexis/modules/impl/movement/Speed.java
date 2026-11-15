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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@FunctionAdd(name = "Speed", alias = "Speed", category = Category.Movement, description = "Increases movement speed")
@Environment(EnvType.CLIENT)
public final class Speed extends Function {
    private final ModeSetting mode = new ModeSetting("Mode", "AresMine", "AresMine", "Vanilla");
    private final SliderSetting boost = new SliderSetting("AresMine boost", 1.0F, 0.2F, 2.0F, 0.05F)
            .hidden(() -> !mode.is("AresMine"));
    private final SliderSetting vanillaBoost = new SliderSetting("Vanilla boost", 100F, 100F, 3.0F, 0.05F)
            .hidden(() -> !mode.is("Vanilla"));

    public Speed() {
        addSettings(mode, boost, vanillaBoost);
    }

    @EventHandler
    public void onUpdate(EventUpdate.Fast event) {
        if (nullCheck()) return;

        if (mode.is("AresMine")) {
            if (!isMoving() || !isCollidingWithPlayer()) return;
            float[] movement = MoveUtil.getMovementFromKeys();
            double direction = MoveUtil.direction(mc.player.getYaw(), movement[0], movement[1]);
            double speed = boost.get();
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(-Math.sin(direction) * speed, velocity.y, Math.cos(direction) * speed);
            return;
        }

        if (mode.is("Vanilla")) {
            if (!isMoving()) return;
            float[] movement = MoveUtil.getMovementFromKeys();
            double direction = MoveUtil.direction(mc.player.getYaw(), movement[0], movement[1]);
            Vec3d velocity = mc.player.getVelocity();
            double currentHoriz = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            double targetSpeed = 0.2873 * vanillaBoost.get(); // vanilla base speed ~0.2873 b/t
            if (currentHoriz < targetSpeed) {
                mc.player.setVelocity(
                        -Math.sin(direction) * targetSpeed,
                        velocity.y,
                        Math.cos(direction) * targetSpeed
                );
            }
        }
    }

    private boolean isMoving() {
        float[] movement = MoveUtil.getMovementFromKeys();
        return movement[0] != 0.0F || movement[1] != 0.0F;
    }

    private boolean isCollidingWithPlayer() {
        Box box = mc.player.getBoundingBox().expand(0.08D, 0.0D, 0.08D);
        for (net.minecraft.entity.Entity entity : mc.world.getOtherEntities(mc.player, box)) {
            if (!entity.isAlive()) continue;
            if (entity instanceof net.minecraft.entity.LivingEntity) return true;
        }
        return false;
    }
}
