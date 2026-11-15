package elexis.client.utils.render.animations.impl;

import elexis.client.utils.render.animations.EasingFunction;
import elexis.client.utils.render.animations.Easings;

public class SimpleAlphaAnimation {

    float target = 0f;
    long startTime = 0;
    int DURATION_MS;

    private EasingFunction easing = Easings.EASE_OUT_CUBIC;

    public SimpleAlphaAnimation(int duration) {
        this.DURATION_MS = duration;
    }

    public void setDuration(int duration) {
        this.DURATION_MS = Math.max(1, duration);
    }

    public void show() {
        this.target = 255f;
        this.startTime = System.currentTimeMillis();
    }

    public void hide() {
        this.target = 0f;
        this.startTime = System.currentTimeMillis();
    }

    public int get() {
        long now = System.currentTimeMillis();
        long elapsed = now - startTime;

        if (elapsed >= DURATION_MS) {
            return (int) target;
        }

        float progress = elapsed / (float) DURATION_MS;
        progress = easing.ease(progress);

        if (target == 255f) {
            return (int) (progress * 255f);
        } else {
            return (int) (255f - progress * 255f);
        }
    }

    public float getProgress() {
        return get() / 255f;
    }

    public boolean isFinished() {
        long now = System.currentTimeMillis();
        return (now - startTime) >= DURATION_MS;
    }
}
