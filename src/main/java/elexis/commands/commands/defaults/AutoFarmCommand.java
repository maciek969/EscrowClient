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
        /* */
    }

    private Text formatPos(BlockPos pos) {
        return Text.literal("(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")")
                .formatted(Formatting.AQUA);
    }

    private void sendHelp() {
        logDirect("§8§m                                                ");
        logDirect("§6§l≫ AutoFarm §8— §7cocoa beans");
        logDirect("§f" + FORCE_COMMAND_PREFIX + "autofarm cacao pos1 §8— §7point 1");
        logDirect("§f" + FORCE_COMMAND_PREFIX + "autofarm cacao pos2 §8— §7point 2");
        logDirect("§7After setting both points — the coordinates");
        logDirect("§7are automatically passed to the AutoFarm module");
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
        return "Set coordinates for cocoa auto-farm";
    }

    @Override
    public List<String> getLongDesc() {
        return List.of(
                "§7Configures the region for cocoa bean auto-farm",
                "",
                "§f.autofarm cacao pos1 §8— §7point 1 (where you stand)",
                "§f.autofarm cacao pos2 §8— §7point 2 (where you stand)",
                "",
                "§aCoordinates are automatically passed to the module"
        );
    }
}