package elexis.client.events.impl.render;

import elexis.client.events.api.Event;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class EventRotation extends Event {
    float yaw;
    float pitch;
    float partialTicks;
}
