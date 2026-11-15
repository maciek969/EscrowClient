package elexis.modules.impl.combat;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

@FunctionAdd(name = "MaceTarget", alias = "Mace Target", category = Category.Combat, description = "Помогает атаковать булавой сверху")
public class MaceTarget extends Function {
    private final NumberSetting height = new NumberSetting("Height", 20.0F, 20.0F, 50.0F, 1.0F);
    private final BooleanSetting airStuck = new BooleanSetting("Air Stuck", false);

    public MaceTarget() {
        addSettings(height, airStuck);
    }

    @EventHandler
    public void onTick(EventUpdate.Slow event) {
        if (nullCheck()) {
            return;
        }

        PlayerEntity target = mc.world.getPlayers().stream()
                .filter(player -> player != mc.player && player.isAlive())
                .min((a, b) -> Double.compare(a.distanceTo(mc.player), b.distanceTo(mc.player)))
                .orElse(null);

        if (target == null || target.distanceTo(mc.player) > 6.0F) {
            return;
        }

        if (airStuck.isEnabled() && mc.player.getY() - target.getY() >= height.getCurrent()) {
            mc.player.setVelocity(0.0D, 0.0D, 0.0D);
        }

        if (mc.player.getMainHandStack().isOf(Items.MACE) && mc.player.fallDistance > 3.0F) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}
