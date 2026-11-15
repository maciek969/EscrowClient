package elexis.modules.impl.render;

import elexis.client.events.impl.client.EventKey;
import elexis.client.events.impl.render.EventAspectRatio;
import elexis.client.events.impl.render.EventCamera;
import elexis.client.events.impl.render.EventFov;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BindSetting;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

@FunctionAdd(name = "CameraTweaks", alias = "Camera Tweaks", category = Category.Render)
public class CameraTweaks extends Function {
    float targetFov = 110f;
    float currentFov = 110f;
    static final long ZOOM_DURATION = 150;

    float startFov = 110f;
    long animationStartTime = 0L;
    boolean isZoomActive = false;

    final BooleanSetting aspectRatio = new BooleanSetting("Aspect ratio", false);
    final SliderSetting aspectRatioValue = new SliderSetting("Screen width value", 1.55f, 1f, 2f, 0.01f);
    final BooleanSetting clipSetting = new BooleanSetting("Camera clip", false);
    final SliderSetting distanceSetting = new SliderSetting("Camera distance in F5", 4f, 2f, 4f, 0.1f);
    final BindSetting zoomSetting = new BindSetting("Zoom", GLFW.GLFW_KEY_C);

    public CameraTweaks() {
        addSettings(aspectRatio, aspectRatioValue, clipSetting, distanceSetting, zoomSetting);
        setState(true);
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (zoomSetting.get() == -1) return;

        if (e.isKeyDown(zoomSetting.get())) {
            if (!isZoomActive) {
                isZoomActive = true;
                startFov = currentFov;
                targetFov = Math.max(10f, mc.options.getFov().getValue() - 55f);
                animationStartTime = System.currentTimeMillis();
            }
        }

        if (e.isKeyReleased(zoomSetting.get(), true)) {
            if (isZoomActive) {
                isZoomActive = false;
                startFov = currentFov;
                targetFov = mc.options.getFov().getValue();
                animationStartTime = System.currentTimeMillis();
            }
        }
    }

    @EventHandler
    public void onFov(EventFov e) {
        long now = System.currentTimeMillis();
        long elapsed = now - animationStartTime;

        if (elapsed >= ZOOM_DURATION || animationStartTime == 0L) {
            currentFov = isZoomActive ? targetFov : mc.options.getFov().getValue();
        } else {
            float progress = MathHelper.clamp((float) elapsed / ZOOM_DURATION, 0f, 1f);

            float smoothProgress = smoothstep(progress);

            currentFov = MathHelper.lerp(smoothProgress, startFov, targetFov);
        }

        e.setFov((int) MathHelper.clamp(currentFov + 1, 10, 170));
        e.cancel();
    }

    @EventHandler
    public void onCamera(EventCamera e) {
        e.setCameraClip(clipSetting.get());
        e.setDistance(distanceSetting.get().floatValue());
        e.cancel();
    }

    @EventHandler
    public void onAspectRatio(EventAspectRatio e) {
        if (aspectRatio.get()) {
            e.setValue(aspectRatioValue.get());
            e.cancel();
        }
    }

    public static float smoothstep(float x) {
        x = MathHelper.clamp(x, 0.0f, 1.0f);
        return x * x * (3.0f - 2.0f * x);
    }
}