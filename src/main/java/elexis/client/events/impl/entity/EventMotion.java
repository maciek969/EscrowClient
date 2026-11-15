package elexis.client.events.impl.entity;

import elexis.client.events.api.Event;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.Box;
import org.joml.Vector2f;

@Getter
@Setter
public class EventMotion extends Event {
    private float yaw;
    private float pitch;
    private double posX;
    private double posY;
    private double posZ;
    private boolean onGround;
    private Box aabbFrom;
    private Runnable postMotion;
    private boolean restoreRotation;

    public EventMotion(float yaw, float pitch, double posX, double posY, double posZ, boolean onGround, Box aabbFrom, Runnable postMotion) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.onGround = onGround;
        this.aabbFrom = aabbFrom;
        this.postMotion = postMotion;
    }

    public void setRotate(Vector2f vector) {
        this.yaw = vector.x;
        this.pitch = vector.y;
    }
}
