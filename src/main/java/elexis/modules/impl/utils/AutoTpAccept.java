package elexis.modules.impl.utils;

import elexis.Elexis;
import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.Locale;

@FunctionAdd(name = "AutoTpAccept", alias = "AutoTpAccept", category = Category.Utilities, description = "Automatically accepts teleport requests")
@Environment(EnvType.CLIENT)
public class AutoTpAccept extends Function {
    private final BooleanSetting friendsOnly = new BooleanSetting("Friends only", false);
    private boolean canAccept;
    private long lastAcceptTime;

    public AutoTpAccept() {
        addSettings(friendsOnly);
    }

    @EventHandler
    public void onPacket(EventReceivePacket event) {
        if (!(event.getPacket() instanceof GameMessageS2CPacket packet)) {
            return;
        }
        String message = packet.content().getString();
        if (isTeleportMessage(message)) {
            canAccept = !friendsOnly.get() || containsFriend(message.toLowerCase(Locale.ROOT));
        }
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck() || mc.getNetworkHandler() == null || !canAccept) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastAcceptTime < 2500L) {
            return;
        }
        lastAcceptTime = now;
        canAccept = false;
        mc.getNetworkHandler().sendChatMessage("/tpaccept");
    }

    private boolean isTeleportMessage(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("has requested teleport")
                || lower.contains("РїСЂРѕСЃРёС‚ С‚РµР»РµРїРѕСЂС‚")
                || lower.contains("С…РѕС‡РµС‚ С‚РµР»РµРїРѕСЂС‚")
                || lower.contains("tpaccept")
                || lower.contains("tpa");
    }

    private boolean containsFriend(String lowerMessage) {
        if (Elexis.getInstance() == null || Elexis.getInstance().getFriendStorage() == null || mc.world == null) {
            return true;
        }
        return mc.world.getPlayers().stream()
                .filter(player -> Elexis.getInstance().getFriendStorage().isFriend(player.getName().getString()))
                .anyMatch(player -> lowerMessage.contains(player.getName().getString().toLowerCase(Locale.ROOT)));
    }
}
