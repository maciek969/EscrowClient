package elexis.modules.impl.player;

import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

@FunctionAdd(name = "NoServerRotate", alias = "NoServerRotate", category = Category.Player, description = "Блокирует принудительный поворот камеры сервером")
@Environment(EnvType.CLIENT)
public class NoServerRotate extends Function {
    @EventHandler
    public void onPacket(EventReceivePacket event) {
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            event.cancel();
        }
    }
}
