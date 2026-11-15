package elexis.commands.commands.defaults;

import elexis.Elexis;
import elexis.client.utils.client.ILogger;
import elexis.commands.Command;
import elexis.commands.argument.IArgConsumer;
import elexis.commands.exception.CommandException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class RCTCommand extends Command implements ILogger {
    // private final RCTRepository repository;

    protected RCTCommand(Elexis elexisInstance) {
        super("rct");
     //   repository = Elexis.getRCTRepository();
    }
    
    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        /* */
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        return Stream.empty();
    }


    @Override
    public String getShortDesc() {
        return "Rejoins the anarchy";
    }


    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Rejoins the anarchy",
                "",
                "Usage:",
                "> rct <anarchy> - Joins <anarchy>",
                "> rct - Rejoins the anarchy you were just on"
        );
    }
}