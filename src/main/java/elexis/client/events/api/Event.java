package elexis.client.events.api;

import lombok.Getter;

public class Event {
    @Getter boolean cancelled = false;

    public void cancel() {
        cancelled = true;
    }

    public void uncancel() {
        cancelled = false;
    }
}