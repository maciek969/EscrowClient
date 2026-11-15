package elexis.client.events.impl.client;

import elexis.client.events.api.Event;
import lombok.Getter;
import net.minecraft.util.math.BlockPos;

@Getter
public class EventWorldEmit extends Event {
    private final int eventId;
    private final int data;
    private final BlockPos pos;

    public EventWorldEmit(int eventId, int data, BlockPos pos) {
        this.eventId = eventId;
        this.data = data;
        this.pos = pos;
    }
}
