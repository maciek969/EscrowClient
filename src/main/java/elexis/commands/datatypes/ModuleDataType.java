package elexis.commands.datatypes;

import elexis.Elexis;
import elexis.commands.exception.CommandException;
import elexis.commands.helpers.TabCompleteHelper;
import elexis.modules.api.Function;

import java.util.List;
import java.util.stream.Stream;

public enum ModuleDataType implements IDatatypeFor<Function>{
    INSTANCE;

    @Override
    public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
        Stream<String> source = getModules()
                .stream()
                .map(Function::getName);

        String context = datatypeContext
                .getConsumer()
                .getString();

        return new TabCompleteHelper()
                .append(source)
                .filterPrefix(context)
                .sortAlphabetically()
                .stream();
    }

    @Override
    public Function get(IDatatypeContext datatypeContext) throws CommandException {
        final String name = datatypeContext.getConsumer().getString();
        return getModules().stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    private List<? extends Function> getModules() {
        return Elexis.getFunctionManager().getFunctions();
    }
}
