package elexis.commands.commands.defaults;

import elexis.Elexis;
import elexis.client.utils.player.PlayerUtils;
import elexis.commands.Command;
import elexis.commands.argument.IArgConsumer;
import elexis.commands.datatypes.KeyDataType;
import elexis.commands.exception.CommandException;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionManager;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BindCommand extends Command {
    FunctionManager functionManager;

    public BindCommand(Elexis elexisInstance) {
        super("bind");
        functionManager = Elexis.getFunctionManager();
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String action = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";

        switch (action) {
            case "add":
                handleAddBind(args);
                break;
            case "remove":
                handleRemoveBind(args);
                break;
            case "list":
                handleListBinds(args);
                break;
            case "clear":
                handleClearBinds(args);
                break;
            default:
                logDirect("§cUnknown action. Use: §fbind add/remove/list/clear");
                break;
        }
    }

    void handleAddBind(IArgConsumer args) throws CommandException {
        args.requireExactly(2);

        String functionName = args.getString();
        int key = args.getDatatypeFor(KeyDataType.INSTANCE).getValue();

        Function function = functionManager.getFunctionByName(functionName);
        if (function == null) {
            logDirect("§cModule §f" + functionName + "§c not found!");
            return;
        }

        functionManager.getFunctions().stream()
                .filter(f -> f.getBind() == key && f != function)
                .findFirst()
                .ifPresent(existing -> logDirect(Formatting.YELLOW + "Key §b"
                        + PlayerUtils.getBindName(key) + Formatting.YELLOW
                        + " is already bound to module §c" + existing.getName()));

        function.setBind(key);
        logDirect(Formatting.GREEN + "Module §f" + function.getName()
                + Formatting.GREEN + " successfully bound to §f" + PlayerUtils.getBindName(key));
    }

    void handleRemoveBind(IArgConsumer args) throws CommandException {
        args.requireExactly(1);
        String functionName = args.getString();

        Function function = functionManager.getFunctionByName(functionName);
        if (function == null) {
            logDirect("§cModule §f" + functionName + "§c not found!");
            return;
        }

        if (function.getBind() == 0) {
            logDirect("§eModule §f" + function.getName() + "§e has no bound key.");
            return;
        }

        String keyName = PlayerUtils.getBindName(function.getBind());
        function.setBind(0);
        logDirect("§aBind §f" + function.getName() + " §8(§c" + keyName + "§8) §asuccessfully removed.");
    }

    void handleListBinds(IArgConsumer args) throws CommandException {
        args.requireMax(0);

        List<Function> boundFunctions = functionManager.getFunctions().stream()
                .filter(f -> f.getBind() != 0)
                .sorted(Comparator.comparing(f -> f.getName().toLowerCase(Locale.US)))
                .toList();

        if (boundFunctions.isEmpty()) {
            logDirect("§fNo bound modules. Use §a.bind add <module> <key>");
            return;
        }

        logDirect("§fBound modules:");
        for (Function function : boundFunctions) {
            logDirect(" §7• §f" + function.getName() + " §8→ §c" + PlayerUtils.getBindName(function.getBind()));
        }
    }

    void handleClearBinds(IArgConsumer args) throws CommandException {
        args.requireMax(0);

        int cleared = 0;
        for (Function function : functionManager.getFunctions()) {
            if (function.getBind() != 0) {
                function.setBind(0);
                cleared++;
            }
        }

        logDirect(Formatting.GREEN + "Binds removed successfully: " + cleared);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            String prefix = args.peekString(0).toLowerCase(Locale.US);
            return Stream.of("add", "remove", "list", "clear")
                    .filter(s -> s.startsWith(prefix))
                    .sorted();
        }

        String action = args.peekString(0).toLowerCase(Locale.US);

        if ("add".equals(action)) {
            if (args.hasExactly(2)) {
                String prefix = args.peekString(1).toLowerCase(Locale.US);
                return functionManager.getFunctions().stream()
                        .map(Function::getName)
                        .filter(name -> name.toLowerCase(Locale.US).startsWith(prefix))
                        .sorted(String.CASE_INSENSITIVE_ORDER);
            }
            if (args.hasExactly(3)) {
                return args.tabCompleteDatatype(KeyDataType.INSTANCE);
            }
        }

        if ("remove".equals(action) && args.hasExactly(2)) {
            String prefix = args.peekString(1).toLowerCase(Locale.US);
            return functionManager.getFunctions().stream()
                    .filter(f -> f.getBind() != 0)
                    .map(Function::getName)
                    .filter(name -> name.toLowerCase(Locale.US).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER);
        }

        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Manage module binds.";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Command manages module binds.",
                "",
                "Usage:",
                "> bind add <module> <key> - bind a module to a key.",
                "> bind remove <module> - remove a module bind.",
                "> bind list - show all current binds.",
                "> bind clear - remove all binds."
        );
    }
}
