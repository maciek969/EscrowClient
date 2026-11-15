package elexis.commands.commands.defaults;

import elexis.Elexis;
import elexis.client.utils.config.BlockESPStorage;
import elexis.commands.Command;
import elexis.commands.argument.IArgConsumer;
import elexis.commands.datatypes.BlockDataType;
import elexis.commands.exception.CommandException;
import elexis.commands.helpers.Paginator;
import elexis.commands.helpers.TabCompleteHelper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static elexis.commands.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlockESPCommand extends Command {

    BlockESPStorage blockESPStorage = Elexis.getInstance().getBlockESPStorage();

    public BlockESPCommand() {
        super("blockesp");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String action = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";

        switch (action) {
            case "add" -> handleAdd(args);
            case "remove" -> handleRemove(args);
            case "list" -> handleList(label);
            default -> logDirect(Formatting.RED + "Неизвестная подкоманда. Используй: add, remove, list");
        }
    }

    private void handleAdd(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String input = args.getString();

        Block block = findBlockByName(input);
        if (block == null) {
            logDirect(Formatting.RED + "Блок \"" + input + "\" не существует!");
            return;
        }

        if (blockESPStorage.getBlocks().contains(block)) {
            logDirect(Formatting.GRAY + "Блок \"" + Formatting.RED + getBlockName(block) + Formatting.GRAY + "\" уже в списке!");
            return;
        }

        blockESPStorage.add(block);
        logDirect("Блок \"" + Formatting.GREEN + getBlockName(block) + Formatting.WHITE + "\" добавлен в BlockESP!");
    }

    private void handleRemove(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String input = args.getString();

        Block block = findBlockByName(input);
        if (block == null) {
            logDirect(Formatting.RED + "Блок \"" + input + "\" не существует!");
            return;
        }

        if (!blockESPStorage.getBlocks().contains(block)) {
            logDirect(Formatting.GRAY + "Блок \"" + Formatting.RED + getBlockName(block) + Formatting.GRAY + "\" и так не в списке!");
            return;
        }

        blockESPStorage.remove(block);
        logDirect("Блок \"" + Formatting.GREEN + getBlockName(block) + Formatting.WHITE + "\" удалён из BlockESP!");
    }

    private void handleList(String label) throws CommandException {
        List<Block> blocks = blockESPStorage.getBlocks();

        if (blocks.isEmpty()) {
            logDirect(Formatting.GRAY + "Нет пользовательских блоков в BlockESP.");
            logDirect(Formatting.DARK_GRAY + "Автоматически подсвечиваются: камни, руды, обсидиан.");
            return;
        }

        Paginator.paginate(
                null, new Paginator<>(blocks),
                () -> logDirect(Formatting.DARK_AQUA + "BlockESP — пользовательские блоки:"),
                block -> Text.literal("§7• §f" + getBlockName(block)),
                FORCE_COMMAND_PREFIX + label
        );
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            return Stream.of("add", "remove", "list");
        }

        if (args.hasExactlyOne()) {
            String prefix = args.getString().toLowerCase(Locale.US);
            return new TabCompleteHelper()
                    .prepend("add", "remove", "list")
                    .filterPrefix(prefix)
                    .sortAlphabetically()
                    .stream();
        }

        if (args.hasExactly(2)) {
            String action = args.getString().toLowerCase(Locale.US);

            if ("add".equals(action)) {
                return args.tabCompleteDatatype(BlockDataType.INSTANCE);
            }

            if ("remove".equals(action)) {
                String currentInput = args.getString();
                return blockESPStorage.getBlocks().stream()
                        .map(this::getBlockName)
                        .map(name -> name.replace(" ", "_"))
                        .filter(name -> name.toLowerCase(Locale.US).contains(currentInput.toLowerCase(Locale.US)))
                        .sorted(String.CASE_INSENSITIVE_ORDER);
            }
        }

        return Stream.empty();
    }

    private Block findBlockByName(String input) {
        String search = input.toLowerCase().replace("_", " ");
        return Registries.BLOCK.stream()
                .filter(b -> !b.equals(Registries.BLOCK.get(Registries.BLOCK.getDefaultId())))
                .filter(b -> b.getName().getString().toLowerCase().replace(" ", "_").contains(search))
                .findFirst()
                .orElse(null);
    }

    private String getBlockName(Block block) {
        return block.getName().getString().replace(" ", "_");
    }

    @Override
    public String getShortDesc() {
        return "Управление подсветкой блоков (BlockESP)";
    }

    @Override
    public List<String> getLongDesc() {
        return List.of(
                "Позволяет добавлять и удалять блоки для подсветки в мире",
                "",
                "Использование:",
                "> boxesp add <block> — добавить блок",
                "> boxesp remove <block> — удалить блок",
                "> boxesp list — показать список добавленных блоков"
        );
    }
}