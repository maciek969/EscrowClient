package elexis.modules.impl.movement;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;

@FunctionAdd(name = "AirStuck", alias = "AirStuck", category = Category.Movement, description = "Зависание в воздухе")
@Environment(EnvType.CLIENT)
public class AirStuck extends Function {
    private Vec3d stuckPosition;

    @Override
    public void onEnable() {
        if (!nullCheck()) {
            this.stuckPosition = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            mc.player.setNoGravity(true);
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (!nullCheck()) {
            mc.player.setNoGravity(false);
        }
        this.stuckPosition = null;
        super.onDisable();
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck()) {
            return;
        }

        if (this.stuckPosition == null) {
            this.stuckPosition = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        }

        mc.player.setNoGravity(true);
        mc.player.setPosition(this.stuckPosition);
        mc.player.setVelocity(Vec3d.ZERO);
        mc.player.fallDistance = 0.0F;
    }
}
