package elexis.commands.commands.defaults;

import elexis.Elexis;
import elexis.client.utils.client.ILogger;
import elexis.client.utils.config.GPSStorage;
import elexis.commands.Command;
import elexis.commands.argument.IArgConsumer;
import elexis.commands.exception.CommandException;
import elexis.commands.helpers.Paginator;
import elexis.commands.helpers.TabCompleteHelper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static elexis.commands.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class GpsCommand extends Command implements ILogger {
    final GPSStorage wayRepository;

    protected GpsCommand(Elexis elexisInstance) {
        super("gps");
        wayRepository = elexisInstance.getGpsStorage();
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            handleListWays(label, args);
            return;
        }

        String arg = args.getString().toLowerCase(Locale.US);
        switch (arg) {
            case "add" -> handleAddWay(args);
            case "remove" -> handleRemoveWay(args);
            case "clear" -> handleClearWays(args);
            case "list" -> handleListWays(label, args);
            default -> handleQuickWay(arg, args);
        }
    }

    void handleQuickWay(String firstArg, IArgConsumer args) throws CommandException {
        args.requireExactly(1);
        int x;
        int z;
        try {
            x = Integer.parseInt(firstArg);
            z = args.getAs(Integer.class);
        } catch (NumberFormatException exception) {
            logDirect("Использование: .gps <x> <z>", Formatting.RED);
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        int y = mc.player == null ? 64 : (int) Math.round(mc.player.getY());
        String name = "GPS";
        if (wayRepository.exists(name)) {
            wayRepository.remove(name);
        }
        wayRepository.add(name, x, y, z);
        logDirect("GPS метка поставлена: (" + x + ", " + z + ")", Formatting.GRAY);
    }

    void handleAddWay(IArgConsumer args) throws CommandException {
        args.requireMin(3);
        String name = args.getString();
        int x = args.getArgs().get(0).getAs(Integer.class);
        int y = args.getArgs().get(1).getAs(Integer.class);
        int z = args.getArgs().get(2).getAs(Integer.class);

        if (wayRepository.exists(name)) {
            logDirect("Метка с таким именем уже есть в списке!", Formatting.RED);
            return;
        }

        wayRepository.add(name, x, y, z);
        logDirect("Добавлена метка " + name + ", координаты: (" + x + ", " + y + ", " + z + ")", Formatting.GRAY);
    }

    void handleRemoveWay(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        String name = args.getString();
        if (wayRepository.exists(name)) {
            wayRepository.remove(name);
            logDirect("Метка " + name + " удалена.", Formatting.GREEN);
        } else {
            logDirect("Метка с названием '" + name + "' не найдена.", Formatting.RED);
        }
    }

    void handleListWays(String label, IArgConsumer args) throws CommandException {
        args.requireMax(1);
        Paginator.paginate(args, new Paginator<>(wayRepository.getPoints()),
                () -> logDirect("Список меток:"),
                way -> Text.literal("Название: " + way.getName()).formatted(Formatting.GRAY)
                        .append(Text.literal(" Координаты: (" + way.getX() + ", " + way.getY() + ", " + way.getZ() + ")").formatted(Formatting.WHITE)),
                FORCE_COMMAND_PREFIX + label);
    }

    void handleClearWays(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        wayRepository.clear();
        logDirect("Все GPS метки удалены.", Formatting.GREEN);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            return new TabCompleteHelper().prepend("x").stream();
        }
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper().prepend("z").stream();
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Позволяет ставить GPS метки в мире";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "С помощью этой команды можно ставить GPS метку или управлять списком меток",
                "",
                "Использование:",
                "> gps <x> <z> - ставит быструю GPS метку",
                "> gps add <name> <x> <y> <z> - добавляет метку",
                "> gps remove <name> - удаляет метку",
                "> gps list - список меток",
                "> gps clear - очищает список меток."
        );
    }
}