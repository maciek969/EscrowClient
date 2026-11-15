package elexis.commands.commands.defaults;

import elexis.Elexis;
import elexis.client.utils.client.ILogger;
import elexis.client.utils.config.StaffStorage;
import elexis.commands.Command;
import elexis.commands.argument.IArgConsumer;
import elexis.commands.datatypes.StaffDataType;
import elexis.commands.datatypes.TabPlayerDataType;
import elexis.commands.exception.CommandException;
import elexis.commands.helpers.Paginator;
import elexis.commands.helpers.TabCompleteHelper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static elexis.commands.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffCommand extends Command implements ILogger {
    final StaffStorage staffRepository;

    protected StaffCommand(Elexis elexisInstance) {
        super("staff");
        staffRepository = elexisInstance.getStaffStorage();
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        switch (arg) {
            case "add" -> handleAddStaff(args);
            case "remove" -> handleRemove(args);
            case "clear" -> handleClearStaffs(args);
            case "list" -> handleList(label, args);
        }
    }

    void handleAddStaff(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String name = args.getString();

        if (staffRepository.exists(name)) {
            logDirect("This moderator is already in the list!", Formatting.RED);
            return;
        }

        logDirect("Moderator added " + name, Formatting.GRAY);
        staffRepository.add(name);
    }
    
    void handleRemove(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        String name = args.getString();
        if (staffRepository.exists(name)) {
            logDirect(Formatting.GREEN + "Moderator " + Formatting.RED + name + Formatting.GREEN + " was removed from the list!");
            staffRepository.remove(name);
        } else {
            logDirect("Moderator with nickname '" + name + "' not found!");
        }
    }
    
    void handleList(String label, IArgConsumer args) throws CommandException {
        args.requireMax(1);

        Paginator.paginate(
                args, new Paginator<>(Elexis.getInstance().getStaffStorage().getStaffs()),
                () -> logDirect("Moderator list:"),
                staff -> {
                    String names = staff.getName();
                    MutableText namesComponent = Text.literal(names);
                    namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
                    return namesComponent;
                },
                FORCE_COMMAND_PREFIX + label
        );
    }
    
    void handleClearStaffs(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        staffRepository.clear();
        logDirect(Formatting.GREEN + "All moderator nicknames removed.");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny()) {
            String arg = args.getString();
            if (args.hasExactlyOne()) {
                if (arg.equalsIgnoreCase("add")) {
                    return args.tabCompleteDatatype(TabPlayerDataType.INSTANCE);
                } else if (arg.equalsIgnoreCase("remove")) {
                    return args.tabCompleteDatatype(StaffDataType.INSTANCE);
                }
            } else {
                return new TabCompleteHelper()
                        .sortAlphabetically()
                        .prepend("add", "remove", "list", "clear")
                        .filterPrefix(arg)
                        .stream();
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Lets you add moderators to the moderation list";
    }
    
    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "This command lets you add/remove moderators from the list",
                "",
                "Usage:",
                "> staff add <name> - Adds a moderator",
                "> staff remove <name> - Removes a moderator from the list",
                "> staff list - Returns the moderator nickname list",
                "> staff clear - Clears the moderator list."
        );
    }
}