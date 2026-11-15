package elexis.client.events.impl.entity;

import elexis.client.events.api.Event;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
@AllArgsConstructor
public class SlowWalkingEvent extends Event {
    private float strafe;
    private float forward;
}
