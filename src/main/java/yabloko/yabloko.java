package yabloko;

import java.util.Collections;
import java.util.List;
import yabloko.base.theme.Theme;
import yabloko.client.modules.api.Module;

public class yabloko {
    private static final yabloko INSTANCE = new yabloko();
    private final ThemeManager themeManager = new ThemeManager();
    private final ModuleManager moduleManager = new ModuleManager();

    private yabloko() {}

    public static yabloko getInstance() {
        return INSTANCE;
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    private final DiscordManager discordManager = new DiscordManager();
    private final ServerHandler serverHandler = new ServerHandler();

    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    public ServerHandler getServerHandler() {
        return serverHandler;
    }

    public static class ThemeManager {
        private final Theme currentTheme = new Theme();

        public Theme getCurrentTheme() {
            return currentTheme;
        }
    }

    public static class ModuleManager {
        public List<Module> getModules() {
            return Collections.emptyList();
        }
    }

    public static class DiscordManager {
        public net.minecraft.util.Identifier getAvatarId() {
            return null;
        }
    }

    public static class ServerHandler {
        public String getServer() {
            return "";
        }
    }
}

