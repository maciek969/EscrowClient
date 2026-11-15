package elexis.client.events.impl.entity;

import elexis.client.events.api.Event;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.util.math.Vec3d;

@EqualsAndHashCode(callSuper = false)
@Data
@AllArgsConstructor
public class EventMove extends Event {
    private Vec3d movement;

    public EventMove(double x, double y, double z) {
        this.movement = new Vec3d(x, y, z);
    }
}
