package elexis.commands.exception;

import elexis.client.utils.client.ILogger;
import elexis.commands.ICommand;
import elexis.commands.argument.ICommandArgument;
import net.minecraft.util.Formatting;

import java.util.List;

public interface ICommandException extends ILogger {

    String getMessage();

    default void handle(ICommand command, List<ICommandArgument> args) {
        logDirect(
                this.getMessage(),
                Formatting.RED
        );
    }
}
