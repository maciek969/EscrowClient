package elexis.commands.commands.defaults;

import elexis.Elexis;
import elexis.client.utils.config.FriendStorage;
import elexis.commands.Command;
import elexis.commands.argument.IArgConsumer;
import elexis.commands.datatypes.FriendDataType;
import elexis.commands.datatypes.TabPlayerDataType;
import elexis.commands.exception.CommandException;
import elexis.commands.helpers.Paginator;
import elexis.commands.helpers.TabCompleteHelper;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static elexis.commands.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class FriendCommand extends Command {
    final FriendStorage friendRepository;

    protected FriendCommand(Elexis elexisInstance) {
        super("friend");
        friendRepository = elexisInstance.getFriendStorage();
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        switch (arg) {
            case "add" -> handleAddFriend(args);
            case "remove" -> handleRemove(args);
            case "clear" -> handleClear(args);
            case "list" -> handleList(label, args);
        }
    }

    void handleAddFriend(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String name = args.getString();

        if (friendRepository.exists(name)) {
            logDirect("This nickname is already in the list!", Formatting.RED);
            return;
        }

        logDirect("Nickname added " + name, Formatting.GRAY);
        friendRepository.add(name);
    }

    void handleRemove(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        String name = args.getString();
        if (friendRepository.exists(name)) {
            logDirect(Formatting.GREEN + "Nickname " + Formatting.RED + name + Formatting.GREEN + " was removed from the list!");
            friendRepository.remove(name);
        } else {
            logDirect("Player with nickname '" + name + "' not found!");
        }
    }

    void handleList(String label, IArgConsumer args) throws CommandException {
        args.requireMax(1);

        Paginator.paginate(
                args, new Paginator<>(Elexis.getInstance().getFriendStorage().getFriends()),
                () -> logDirect("Friend nickname list:"),
                friend -> {
                    String names = friend.getName();
                    MutableText namesComponent = Text.literal(names);
                    namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
                    return namesComponent;
                },
                FORCE_COMMAND_PREFIX + label
        );
    }

    void handleClear(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        friendRepository.clear();
        logDirect(Formatting.GREEN + "All friend nicknames removed.");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny()) {
            String arg = args.getString();
            if (args.hasExactlyOne()) {
                if (arg.equalsIgnoreCase("add")) {
                    return args.tabCompleteDatatype(TabPlayerDataType.INSTANCE);
                } else if (arg.equalsIgnoreCase("remove")) {
                    return args.tabCompleteDatatype(FriendDataType.INSTANCE);
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
        return "Lets you manage the friends list";
    }

    
    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "This command lets you add/remove friends from the list",
                "",
                "Usage:",
                "> friend add <name> - Adds a name to the friends list.",
                "> friend remove <name> - Removes a name from the friends list.",
                "> friend list - Returns the friends list",
                "> friend clear - Clears the friends list."
        );
    }
}
