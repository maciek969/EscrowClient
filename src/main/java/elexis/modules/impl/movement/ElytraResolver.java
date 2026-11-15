package elexis.modules.impl.movement;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

@FunctionAdd(name = "ElytraResolver", alias = "Elytra Resolver", category = Category.Movement, description = "Steers the player away from the nearest threat in flight")
public final class ElytraResolver extends Function {
    private final NumberSetting distance = new NumberSetting("Distance", 6.0F, 4.0F, 8.0F, 0.1F);
    private final BooleanSetting autoFirework = new BooleanSetting("Auto firework", true);

    public ElytraResolver() {
        addSettings(distance, autoFirework);
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck() || !mc.player.isGliding()) {
            return;
        }

        PlayerEntity nearest = mc.world.getPlayers().stream()
                .filter(player -> player != mc.player && player.isAlive())
                .min((a, b) -> Double.compare(a.distanceTo(mc.player), b.distanceTo(mc.player)))
                .orElse(null);

        if (nearest == null || nearest.distanceTo(mc.player) > distance.getCurrent()) {
            return;
        }

        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d nearestPos = new Vec3d(nearest.getX(), nearest.getY(), nearest.getZ());
        Vec3d away = playerPos.subtract(nearestPos).normalize().multiply(0.25D);
        mc.player.addVelocity(away.x, 0.02D, away.z);
    }
}
