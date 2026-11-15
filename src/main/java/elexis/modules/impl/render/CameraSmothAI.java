package elexis.modules.impl.render;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRotation;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.SliderSetting;
import elexis.modules.impl.combat.HitAura;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;

@FunctionAdd(name = "CameraSmothAI", alias = "CameraSmothAI", category = Category.Render, description = "Smooth combat camera")
@Environment(EnvType.CLIENT)
public final class CameraSmothAI extends Function {
    public final SliderSetting smooth = new SliderSetting("Smooth", 0.55F, 0.05F, 1.0F, 0.05F);
    public final SliderSetting hitLift = new SliderSetting("Hit Lift", 0.35F, 0.0F, 1.5F, 0.05F);
    public final SliderSetting visualRotation = new SliderSetting("Visual Rotation", 0.45F, 0.05F, 1.0F, 0.05F);
    private float smoothedYaw = Float.NaN;
    private float smoothedPitch = Float.NaN;
    private int liftTicks;

    public CameraSmothAI() {
        this.addSettings(smooth, hitLift, visualRotation);
    }

    @Override
    public void onDisable() {
        smoothedYaw = Float.NaN;
        smoothedPitch = Float.NaN;
        liftTicks = 0;
        super.onDisable();
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck()) {
            return;
        }
        if (mc.player.hurtTime > 0) {
            liftTicks = Math.max(liftTicks, 8);
        } else if (liftTicks > 0) {
            liftTicks--;
        }
    }

    @EventHandler
    public void onRotation(EventRotation event) {
        if (nullCheck()) {
            return;
        }

        float targetYaw = event.getYaw();
        float targetPitch = event.getPitch();
        if (HitAura.silentActive && HitAura.target != null) {
            float blend = MathHelper.clamp(visualRotation.get(), 0.05F, 1.0F);
            targetYaw += MathHelper.wrapDegrees(HitAura.spoofYaw - targetYaw) * blend;
            targetPitch += (HitAura.spoofPitch - targetPitch) * blend;
        }

        if (Float.isNaN(smoothedYaw) || Float.isNaN(smoothedPitch)) {
            smoothedYaw = targetYaw;
            smoothedPitch = targetPitch;
        }

        float factor = MathHelper.clamp(smooth.get(), 0.05F, 1.0F);
        smoothedYaw += MathHelper.wrapDegrees(targetYaw - smoothedYaw) * factor;
        smoothedPitch += (targetPitch - smoothedPitch) * factor;

        float lift = 0.0F;
        if (liftTicks > 0 && hitLift.get() > 0.0F) {
            lift = MathHelper.sin((liftTicks / 8.0F) * (float) Math.PI) * hitLift.get() * 8.0F;
        }

        event.setYaw(smoothedYaw);
        event.setPitch(MathHelper.clamp(smoothedPitch - lift, -90.0F, 90.0F));
    }
}