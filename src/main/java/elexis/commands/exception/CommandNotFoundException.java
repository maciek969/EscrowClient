package elexis.commands.exception;

import elexis.client.utils.client.ILogger;
import elexis.commands.ICommand;
import elexis.commands.argument.ICommandArgument;

import java.util.List;

public class CommandNotFoundException extends CommandException implements ILogger {

    public final String command;

    public CommandNotFoundException(String command) {
        super(String.format("Command not found: %s", command));
        this.command = command;
    }

    @Override
    public void handle(ICommand command, List<ICommandArgument> args) {
       logDirect(getMessage());
    }
}
