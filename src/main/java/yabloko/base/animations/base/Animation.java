package yabloko.base.animations.base;

public class Animation {
    private float value;
    private long duration;
    private Easing easing;

    public Animation(long duration, Easing easing) {
        this.duration = duration;
        this.easing = easing;
    }

    public Animation(long duration, float startValue, Easing easing) {
        this.duration = duration;
        this.easing = easing;
        this.value = startValue;
    }

    public void update(float target) {
        this.value = target;
    }

    public void update(boolean target) {
        this.value = target ? 1.0F : 0.0F;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public void setStartValue(float startValue) {
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setEasing(Easing easing) {
        this.easing = easing;
    }
}

