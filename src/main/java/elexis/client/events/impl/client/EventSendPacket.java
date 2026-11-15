package elexis.client.events.impl.client;

import elexis.client.events.api.Event;
import lombok.Getter;
import net.minecraft.network.packet.Packet;

@Getter
public class EventSendPacket extends Event {
    private final Packet<?> packet;

    public EventSendPacket(Packet<?> packet) {
        this.packet = packet;
    }
}
