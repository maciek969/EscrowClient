package elexis.commands.commands.defaults;

import elexis.client.utils.client.ILogger;
import elexis.commands.Command;
import elexis.commands.argument.IArgConsumer;
import elexis.commands.datatypes.AutoFarmActionDataType;
import elexis.commands.datatypes.IDatatypeContext;
import elexis.commands.exception.CommandException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static elexis.commands.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoFarmCommand extends Command implements ILogger {

    BlockPos pos1;
    BlockPos pos2;

    public AutoFarmCommand() {
        super("autofarm");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        /*
        if (!args.hasAny()) {
            sendHelp();
            return;
        }

        String type = args.getString().toLowerCase();

        if (!type.equals("cacao") && !type.equals("cocoa")) {
            logDirect("§cНеизвестный тип: §f" + type + "§c. Используйте: cacao", Formatting.RED);
            sendHelp();
            return;
        }

        if (!args.hasAny()) {
            logDirect("§cУкажите действие: §fpos1 §7или §fpos2", Formatting.RED);
            return;
        }

        String action = args.getDatatypeFor(AutoFarmActionDataType.INSTANCE);

        if (mc.player == null) {
            logDirect("§cИгрок не найден!", Formatting.RED);
            return;
        }

        if ("pos1".equals(action)) {
            pos1 = mc.player.getBlockPos();
            logDirect(Text.literal("§7Позиция 1 установлена: §b").append(formatPos(pos1)));
        } else {
            pos2 = mc.player.getBlockPos();
            logDirect(Text.literal("§7Позиция 2 установлена: §b").append(formatPos(pos2)));
        }

        if (pos1 != null && pos2 != null) {
            AutoFarm autoFarm = Elexis.getInstance().getFunctionManager().getAutoFarm();
            if (autoFarm != null) {
                autoFarm.setCocoaCoords(pos1, pos2);
                logDirect(Text.literal("§a§lКоординаты для какао-фарма установлены!")
                        .append(Text.literal("\n§7От §b").append(formatPos(pos1)))
                        .append(Text.literal(" §7до §b").append(formatPos(pos2)))
                        .formatted(Formatting.GREEN));
            } else {
                logDirect("§cМодуль AutoFarm не найден!", Formatting.RED);
            }
        }

         */
    }

    private Text formatPos(BlockPos pos) {
        return Text.literal("(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")")
                .formatted(Formatting.AQUA);
    }

    private void sendHelp() {
        logDirect("§8§m                                                ");
        logDirect("§6§l≫ AutoFarm §8— §7какао-бобы");
        logDirect("§f" + FORCE_COMMAND_PREFIX + "autofarm cacao pos1 §8— §7точка 1");
        logDirect("§f" + FORCE_COMMAND_PREFIX + "autofarm cacao pos2 §8— §7точка 2");
        logDirect("§7После установки обеих точек — координаты");
        logDirect("§7автоматически передаются в модуль AutoFarm");
        logDirect("§8§m                                                ");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (!args.hasConsumed()) {
            return Stream.of("cacao");
        }

        String first = args.consumedString().toLowerCase(Locale.US);

        if (first.equals("cacao") || first.equals("cocoa")) {
            if (args.hasAny()) {
                return AutoFarmActionDataType.INSTANCE.tabComplete(new IDatatypeContext() {
                    @Override public IArgConsumer getConsumer() { return args; }
                });
            }
            return Stream.of("pos1", "pos2");
        }

        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Установка координат для автофарма какао";
    }

    @Override
    public List<String> getLongDesc() {
        return List.of(
                "§7Настраивает регион для автофарма какао-бобов",
                "",
                "§f.autofarm cacao pos1 §8— §7точка 1 (где стоишь)",
                "§f.autofarm cacao pos2 §8— §7точка 2 (где стоишь)",
                "",
                "§aКоординаты автоматически передаются в модуль"
        );
    }
}