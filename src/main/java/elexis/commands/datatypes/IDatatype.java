package elexis.commands.datatypes;

import elexis.client.utils.client.ILogger;
import elexis.commands.exception.CommandException;
import elexis.commands.exception.CommandNotEnoughArgumentsException;

import java.util.stream.Stream;

public interface IDatatype extends ILogger {
    Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException, CommandNotEnoughArgumentsException;
}
