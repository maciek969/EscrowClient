package elexis.commands.commands;

import elexis.commands.ICommandSystem;
import elexis.commands.argparser.IArgParserManager;
import elexis.commands.commands.argparser.ArgParserManager;

public enum CommandSystem implements ICommandSystem {
    INSTANCE;

    @Override
    public IArgParserManager getParserManager() {
        return ArgParserManager.INSTANCE;
    }
}
