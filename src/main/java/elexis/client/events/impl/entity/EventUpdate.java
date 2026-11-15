package elexis.client.events.impl.entity;

import elexis.client.events.api.Event;

public class EventUpdate extends Event {
    public static class Slow extends EventUpdate {}
    public static class Fast extends EventUpdate {}
    public static class Fastest extends EventUpdate {}
}