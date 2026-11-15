package elexis.commands;

import elexis.commands.argparser.IArgParserManager;

public interface ICommandSystem {
    IArgParserManager getParserManager();
}
