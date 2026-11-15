package elexis.commands.datatypes;

import elexis.Elexis;
import elexis.client.utils.config.GPSStorage;
import elexis.commands.exception.CommandException;
import elexis.commands.helpers.TabCompleteHelper;

import java.util.List;
import java.util.stream.Stream;

public enum GpsDataType implements IDatatypeFor<GPSStorage.GPSPoint> {
    INSTANCE;

    @Override
    public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
        Stream<String> ways = getWay().stream().map(GPSStorage.GPSPoint::getName);
        String context = datatypeContext.getConsumer().getString();
        return new TabCompleteHelper().append(ways).filterPrefix(context).sortAlphabetically().stream();
    }

    @Override
    public GPSStorage.GPSPoint get(IDatatypeContext datatypeContext) throws CommandException {
        String text = datatypeContext.getConsumer().getString();
        return getWay().stream().filter(s -> s.getName().equalsIgnoreCase(text)).findFirst().orElse(null);
    }

    private List<? extends GPSStorage.GPSPoint> getWay() {
        return Elexis.getInstance().getGpsStorage().getPoints();
    }
}
