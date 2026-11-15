package elexis.modules.impl.utils;

import elexis.client.events.impl.client.EventSendPacket;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;

@FunctionAdd(name = "XCarry", alias = "XCarry", category = Category.Utilities, description = "Does not close the inventory screen with syncId 0 packet")
@Environment(EnvType.CLIENT)
public class XCarry extends Function {
    @EventHandler
    public void onPacket(EventSendPacket event) {
        if (event.getPacket() instanceof CloseHandledScreenC2SPacket packet && packet.getSyncId() == 0) {
            event.cancel();
        }
    }
}
