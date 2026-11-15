package elexis.client.events.impl.entity;

import elexis.client.events.api.Event;
import lombok.Getter;
import net.minecraft.entity.Entity;

@Getter
public class EventAttack extends Event {
    private final Entity target;

    public EventAttack(Entity target) {
        this.target = target;
    }
}
