package elexis.modules.impl.utils;

import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.Locale;

@FunctionAdd(name = "AutoAuth", alias = "AutoAuth", category = Category.Utilities, description = "Automatically logs in and registers on servers")
@Environment(EnvType.CLIENT)
public class AutoAuth extends Function {
    private final StringSetting password = new StringSetting("Password", "bee1892");
    private long lastAuthTime;

    public AutoAuth() {
        addSettings(password);
    }

    @EventHandler
    public void onPacket(EventReceivePacket event) {
        if (nullCheck() || mc.getNetworkHandler() == null || !(event.getPacket() instanceof GameMessageS2CPacket packet)) {
            return;
        }

        String message = packet.content().getString();
        String pass = password.get();
        if (message == null || pass == null || pass.length() < 4) {
            return;
        }

        String lower = message.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        if (now - lastAuthTime < 1200L) {
            return;
        }

        if (lower.contains("/login") || lower.contains("login") || lower.contains("войдите")) {
            lastAuthTime = now;
            mc.getNetworkHandler().sendChatCommand("login " + pass);
        } else if (lower.contains("/reg") || lower.contains("/register") || lower.contains("зарегистрируйтесь")) {
            lastAuthTime = now;
            mc.getNetworkHandler().sendChatCommand("reg " + pass + " " + pass);
        }
    }
}
