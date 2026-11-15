package elexis.client.events.impl.client;

import elexis.client.events.api.Event;

public class TabCompleteEvent extends Event {
    public final String prefix;
    public String[] completions;

    public TabCompleteEvent(String prefix) {
        this.prefix = prefix;
        this.completions = null;
    }
}