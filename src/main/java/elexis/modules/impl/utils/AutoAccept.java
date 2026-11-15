package elexis.modules.impl.utils;

import elexis.Elexis;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@FunctionAdd(name = "AutoAccept", alias = "AutoAccept", category = Category.Utilities, description = "Automatically accepts teleportation")
@Environment(EnvType.CLIENT)
public class AutoAccept extends Function {
    private final BooleanSetting friendsOnly = new BooleanSetting("Friends Only", false);
    private long lastAcceptTime;

    public AutoAccept() {
        this.addSettings(this.friendsOnly);
    }

    public void handleServerMessage(String message) {
        if (!this.isState() || nullCheck() || mc.getNetworkHandler() == null || message == null) {
            return;
        }

        String lower = message.toLowerCase();
        boolean teleportRequest = lower.contains("tpa")
                || lower.contains("телепорт")
                || lower.contains("запрос")
                || lower.contains("tpaccept")
                || lower.contains("принять");
        if (!teleportRequest) {
            return;
        }

        if (this.friendsOnly.get() && !this.containsFriendName(lower)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - this.lastAcceptTime < 2500L) {
            return;
        }

        this.lastAcceptTime = now;
        mc.getNetworkHandler().sendChatMessage("/tpaccept");
    }

    private boolean containsFriendName(String lowerMessage) {
        if (Elexis.getInstance() == null || Elexis.getInstance().getFriendStorage() == null) {
            return true;
        }

        return mc.world.getPlayers().stream()
                .filter(player -> Elexis.getInstance().getFriendStorage().isFriend(player.getName().getString()))
                .anyMatch(player -> lowerMessage.contains(player.getName().getString().toLowerCase()));
    }
}
