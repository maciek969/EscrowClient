package elexis.commands.datatypes;

import elexis.commands.exception.CommandException;
import elexis.commands.exception.CommandNotEnoughArgumentsException;

public interface IDatatypeFor<T> extends IDatatype  {
    T get(IDatatypeContext datatypeContext) throws CommandException, CommandNotEnoughArgumentsException;
}
