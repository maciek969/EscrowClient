package elexis.modules.impl.movement;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.ModeSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;

@FunctionAdd(name = "NoFallDamage", alias = "NoFallDamage", category = Category.Movement, description = "Останавливает падение, чтобы снизить урон")
@Environment(EnvType.CLIENT)
public class NoFallDamage extends Function {
    private final ModeSetting mode = new ModeSetting("Режим", "SpookyTime", "SpookyTime");

    public NoFallDamage() {
        addSettings(mode);
    }

    @EventHandler
    public void onUpdate(EventUpdate.Fast event) {
        if (nullCheck() || !mode.is("SpookyTime")) {
            return;
        }
        if (mc.player.fallDistance > 0.0F && distanceToGround() > 4.0D) {
            mc.player.setVelocity(0.0D, 0.0D, 0.0D);
        }
    }

    private double distanceToGround() {
        BlockPos.Mutable pos = mc.player.getBlockPos().mutableCopy();
        for (int i = 0; i < 64; i++) {
            if (!mc.world.getBlockState(pos).isAir()) {
                return mc.player.getY() - (pos.getY() + 1.0D);
            }
            pos.move(0, -1, 0);
        }
        return 64.0D;
    }
}
