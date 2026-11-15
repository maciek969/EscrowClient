package elexis.modules.impl.combat.auraProcess.rotationProcess.impl;

import elexis.client.events.impl.entity.EventLook;
import elexis.client.events.impl.render.EventRotation;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.impl.combat.auraProcess.rotationProcess.Component;
import net.minecraft.util.math.MathHelper;

public class FreeLookUtil extends Component {
    public static boolean active;
    public static float freeYaw;
    public static float freePitch;

    @EventHandler
    public void onLook(EventLook event) {
        if (active) {
            rotateTowards(event.getYaw(), event.getPitch());
            event.cancel();
        }
    }

    @EventHandler
    public void onRotation(EventRotation event) {
        if (active) {
            event.setYaw(freeYaw);
            event.setPitch(freePitch);
        } else {
            sync(event.getYaw(), event.getPitch());
        }
    }

    public static void sync(float yaw, float pitch) {
        freeYaw = yaw;
        freePitch = pitch;
    }

    public static void rotateTowards(double yawDelta, double pitchDelta) {
        freePitch = MathHelper.clamp((float) (freePitch + pitchDelta * 0.15D), -90.0F, 90.0F);
        freeYaw = (float) (freeYaw + yawDelta * 0.15D);
    }
}
