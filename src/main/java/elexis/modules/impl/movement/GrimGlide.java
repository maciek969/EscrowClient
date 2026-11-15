package elexis.modules.impl.movement;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;

@FunctionAdd(name = "GrimGlide", alias = "Grim Glide", category = Category.Movement, description = "Стабилизирует полёт на элитрах")
public class GrimGlide extends Function {
    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck() || !mc.player.isGliding()) {
            return;
        }
        if (mc.player.getVelocity().y < -0.04D) {
            mc.player.setVelocity(mc.player.getVelocity().x, -0.04D, mc.player.getVelocity().z);
        }
    }
}
