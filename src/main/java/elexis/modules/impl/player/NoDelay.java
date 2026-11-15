package elexis.modules.impl.player;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.mixins.accessors.MinecraftClientAccessor;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionAdd(name = "NoDelay", alias = "NoDelay", category = Category.Player, description = "Fast block placement")
@Environment(EnvType.CLIENT)
public class NoDelay extends Function {
    @EventHandler
    public void onUpdate(EventUpdate.Fast event) {
        if (mc != null) {
            ((MinecraftClientAccessor) mc).Elexis$setItemUseCooldown(0);
        }
    }
}
