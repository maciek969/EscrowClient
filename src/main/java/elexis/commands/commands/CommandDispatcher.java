package elexis.commands.commands;

import elexis.Elexis;
import elexis.client.events.impl.client.EventChat;
import elexis.client.events.impl.client.TabCompleteEvent;
import elexis.client.utils.client.ILogger;
import elexis.client.utils.eventbus.EventBus;
import elexis.client.utils.eventbus.EventHandler;
import elexis.commands.argument.ICommandArgument;
import elexis.commands.commands.argument.ArgConsumer;
import elexis.commands.commands.argument.CommandArguments;
import elexis.commands.commands.manager.CommandRepository;
import elexis.commands.exception.CommandNotEnoughArgumentsException;
import elexis.commands.exception.CommandNotFoundException;
import elexis.commands.helpers.TabCompleteHelper;
import elexis.commands.manager.ICommandManager;
import elexis.modules.impl.utils.UnHook;
import net.minecraft.util.Pair;

import java.util.List;
import java.util.stream.Stream;

import static elexis.commands.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class CommandDispatcher implements ILogger {
    private final ICommandManager manager;
    public static String prefix = ".";

    public CommandDispatcher(EventBus eventBus) {
        this.manager = Elexis.getInstance().getCommandRepository();
        eventBus.subscribe(this);
    }

    @EventHandler
    public void onChat(EventChat event) {
        if (UnHook.unhooked) {
            return;
        }

        String msg = event.getMessage();

        boolean forceRun = msg.startsWith(FORCE_COMMAND_PREFIX);
        if (msg.startsWith(prefix) || forceRun) {
            event.cancel();
            String commandStr = msg.substring(forceRun ? FORCE_COMMAND_PREFIX.length() : prefix.length());
            if (!runCommand(commandStr) && !commandStr.trim().isEmpty()) {
                new CommandNotFoundException(CommandRepository.expand(commandStr).getLeft()).handle(null, null);
            }
        } else if (runCommand(msg)) {
            event.cancel();
        }
    }

    public boolean runCommand(String msg) {
        if (msg.isEmpty()) {
            return this.runCommand("help");
        }
        Pair<String, List<ICommandArgument>> pair = CommandRepository.expand(msg);
        String command = pair.getLeft();
        String rest = msg.substring(pair.getLeft().length());
        ArgConsumer argc = new ArgConsumer(this.manager, pair.getRight());
       /* if (!argc.hasAny()) {
            Settings.Setting setting = settings.byLowerName.get(command.toLowerCase(Locale.US));
            if (setting != null) {
                logRanCommand(command, rest);
                if (setting.getValueClass() == Boolean.class) {
                    this.manager.execute(String.format("set toggle %s", setting.getName()));
                } else {
                    this.manager.execute(String.format("set %s", setting.getName()));
                }
                return true;
            }
        } else if (argc.hasExactlyOne()) {
            for (Settings.Setting setting : settings.allSettings) {
                if (setting.isJavaOnly()) {
                    continue;
                }
                if (setting.getName().equalsIgnoreCase(pair.getA())) {
                    logRanCommand(command, rest);
                    try {
                        this.manager.execute(String.format("set %s %s", setting.getName(), argc.getString()));
                    } catch (CommandNotEnoughArgumentsException ignored) {
                    } // The operation is safe
                    return true;
                }
            }
        }*/

        // If the command exists, then handle echoing the input

        return this.manager.execute(pair);
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (UnHook.unhooked) {
            return;
        }

        String eventPrefix = event.prefix;
        if (!eventPrefix.startsWith(prefix)) {
            return;
        }

        String msg = eventPrefix.substring(prefix.length());
        List<ICommandArgument> args = CommandArguments.from(msg, true);
        Stream<String> stream = tabComplete(msg);
        if (args.size() == 1) {
            stream = stream.map(x -> prefix + x);
        }
        event.completions = stream.toArray(String[]::new);
    }

    public Stream<String> tabComplete(String msg) {
        try {
            List<ICommandArgument> args = CommandArguments.from(msg, true);
            ArgConsumer argc = new ArgConsumer(this.manager, args);
            if (argc.hasAtMost(2)) {
                if (argc.hasExactly(1)) {
                    return new TabCompleteHelper()
                            .addCommands(this.manager)
                            .filterPrefix(argc.getString())
                            .stream();
                }
          /*      Settings.Setting setting = settings.byLowerName.get(argc.getString().toLowerCase(Locale.US));
                if (setting != null && !setting.isJavaOnly()) {
                    if (setting.getValueClass() == Boolean.class) {
                        TabCompleteHelper helper = new TabCompleteHelper();
                        if ((Boolean) setting.value) {
                            helper.append("true", "false");
                        } else {
                            helper.append("false", "true");
                        }
                        return helper.filterPrefix(argc.getString()).stream();
                    } else {
                        return Stream.of(SettingsUtil.settingValueToString(setting));
                    }
                }*/
            }
            return this.manager.tabComplete(msg);
        } catch (CommandNotEnoughArgumentsException ignored) { // Shouldn't happen, the operation is safe
            return Stream.empty();
        }
    }
}