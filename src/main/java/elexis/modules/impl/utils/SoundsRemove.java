package elexis.modules.impl.utils;

import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;

@FunctionAdd(name = "SoundsRemove", alias = "SoundsRemove", category = Category.Utilities, description = "Removes selected server sounds")
@Environment(EnvType.CLIENT)
public class SoundsRemove extends Function {
    @EventHandler
    public void onPacket(EventReceivePacket event) {
        if (!(event.getPacket() instanceof PlaySoundS2CPacket packet)) {
            return;
        }
        String soundId = packet.getSound().getIdAsString();
        if (soundId != null && soundId.contains("entity.wither")) {
            event.cancel();
        }
    }
}
