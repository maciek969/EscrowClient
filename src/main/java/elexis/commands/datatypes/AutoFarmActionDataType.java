package elexis.commands.datatypes;

import elexis.commands.argument.IArgConsumer;
import elexis.commands.exception.CommandInvalidTypeException;
import elexis.commands.exception.CommandNotEnoughArgumentsException;

import java.util.stream.Stream;

/**
 * DataType для команды .autofarm cacao <pos1|pos2>
 */
public enum AutoFarmActionDataType implements IDatatypeFor<String> {
    INSTANCE;

    private static final String[] ACTIONS = {"pos1", "pos2"};

    @Override
    public Stream<String> tabComplete(IDatatypeContext datatypeContext) {
        IArgConsumer consumer = datatypeContext.getConsumer();
        try {
            String input = consumer.peekString().toLowerCase();
            return Stream.of(ACTIONS)
                    .filter(action -> action.startsWith(input))
                    .sorted();
        } catch (CommandNotEnoughArgumentsException e) {
            return Stream.of(ACTIONS);
        }
    }

    @Override
    public String get(IDatatypeContext datatypeContext) throws CommandInvalidTypeException {
        IArgConsumer consumer = datatypeContext.getConsumer();

        String input;
        try {
            input = consumer.getString().toLowerCase();
        } catch (CommandNotEnoughArgumentsException e) {
            throw new CommandInvalidTypeException(
                    null,
                    "pos1 или pos2",
                    "<ничего>"
            );
        }

        if ("pos1".equals(input) || "pos2".equals(input)) {
            return input;
        }

        throw new CommandInvalidTypeException(
                null,
                "pos1 или pos2",
                input
        );
    }
}