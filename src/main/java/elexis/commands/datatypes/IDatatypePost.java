package elexis.commands.datatypes;

import elexis.commands.exception.CommandException;

public interface IDatatypePost<T, O> extends IDatatype {
    T apply(IDatatypeContext datatypeContext, O original) throws CommandException;
}
