package elexis.commands.commands.defaults;

import elexis.client.utils.client.ILogger;
import elexis.commands.Command;
import elexis.commands.argument.IArgConsumer;
import elexis.commands.commands.CommandDispatcher;
import elexis.commands.exception.CommandException;
import elexis.commands.helpers.TabCompleteHelper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrefixCommand extends Command implements ILogger {
    protected PrefixCommand() {
        super("prefix");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        if (arg.equals("set")) {
            args.requireMin(1);
            logDirect("Prefix set '" + Formatting.RED + (CommandDispatcher.prefix = args.getString()) + Formatting.GRAY + "'", Formatting.GRAY);
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny()) {
            String arg = args.getString();
            if (arg.equalsIgnoreCase("set")) {
                return new TabCompleteHelper().sortAlphabetically().prepend("name").stream();
            } else {
                return new TabCompleteHelper().sortAlphabetically().prepend("set").filterPrefix(arg).stream();
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Lets you change the command prefix in the client";
    }

    
    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "This command lets you change the command prefix in the client",
                "",
                "Usage:",
                "> prefix set <name> - sets the command prefix"
        );
    }
}
