package elexis.commands.exception;

import elexis.client.utils.client.ILogger;
import elexis.commands.ICommand;
import elexis.commands.argument.ICommandArgument;

import java.util.List;

public class CommandUnhandledException extends RuntimeException implements ICommandException, ILogger {

    public CommandUnhandledException(String message) {
        super(message);
    }

    public CommandUnhandledException(Throwable cause) {
        super(cause);
    }

    @Override
    public void handle(ICommand command, List<ICommandArgument> args) {
    }
}
