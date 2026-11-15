package elexis.commands.commands.defaults;

import elexis.Elexis;
import elexis.client.utils.config.ConfigStorage;
import elexis.commands.Command;
import elexis.commands.argument.IArgConsumer;
import elexis.commands.datatypes.ConfigFileDataType;
import elexis.commands.exception.CommandException;
import elexis.commands.helpers.Paginator;
import elexis.commands.helpers.TabCompleteHelper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static elexis.commands.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigCommand extends Command {
    final ConfigStorage configStorage = Elexis.getInstance().getConfigStorage();

    protected ConfigCommand(Elexis elexisInstance) {
        super("config", "cfg");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String action = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";

        if (action.equals("load") || action.contains("load")) {
            args.requireMin(1);
            String name = args.getString();

            if (configStorage.findConfig(name) != null) {
                try {
                    configStorage.loadConfiguration(name);
                    logDirect(String.format("Config %s loaded!", name));
                } catch (Exception e) {
                    logDirect(String.format("Error loading config! Details: %s", e.getMessage()), Formatting.RED);
                }
            } else {
                logDirect(String.format("Config %s not found!", name), Formatting.RED);
            }
        }

        if (action.equals("save") || action.contains("save")) {
            args.requireMin(1);
            String name = args.getString();

            try {
                ConfigStorage.saveConfiguration(name);
                logDirect(String.format("Config %s saved!", name));
            } catch (Exception e) {
                logDirect(String.format("Error saving config! Details: %s", e.getMessage()), Formatting.RED);
            }
        }

        if (action.equals("list") || action.contains("list")) {
            Paginator.paginate(
                    args, new Paginator<>(
                            getConfigs()),
                    () -> logDirect("Config list:"),
                    config -> {
                        MutableText namesComponent = Text.literal(config);
                        namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
                        return namesComponent;
                    },
                    FORCE_COMMAND_PREFIX + label
            );
        }

        if (action.equals("dir") || action.contains("dir")) {
            try {
                Runtime.getRuntime().exec("explorer " + configStorage.CONFIG_DIR.getAbsolutePath());
            } catch (IOException e) {
                logDirect("Configs folder not found!" + e.getMessage());
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            return Stream.empty();
        }

        if (args.hasExactlyOne()) {
            String prefix = args.getString().toLowerCase(Locale.US);
            return new TabCompleteHelper()
                    .sortAlphabetically()
                    .prepend("load", "save", "list", "dir")
                    .filterPrefix(prefix)
                    .stream();
        }

        if (args.hasExactly(2)) {
            String action = args.getString();
            action = action.toLowerCase(Locale.US);

            if (action.equals("load") || action.equals("save")) {
                return args.tabCompleteDatatype(ConfigFileDataType.INSTANCE);
            }
        }

        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Lets you work with configs in the client";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "This command lets you load/save configs",
                "",
                "Usage:",
                "> config load <name> - Loads a config.",
                "> config save <name> - Saves a config.",
                "> config list - Returns the list of configs",
                "> config dir - Opens the configs folder."
        );
    }

    public List<String> getConfigs() {
        List<String> configs = new ArrayList<>();
        File[] configFiles = configStorage.CONFIG_DIR.listFiles();

        if (configFiles != null) {
            for (File configFile : configFiles) {
                if (configFile.isFile() && configFile.getName().endsWith(".json")) {
                    String configName = configFile.getName().replace(".json", "");
                    configs.add(configName);
                }
            }
        }

        return configs;
    }
}