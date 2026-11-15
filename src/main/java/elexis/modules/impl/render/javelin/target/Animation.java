package elexis.modules.impl.render.javelin.target;

/** Time based replacement for Javelin's UI-independent easing state. */
public final class Animation {
    private long duration;
    private Easing easing;
    private double value, start, target;
    private long changedAt;

    public Animation(long duration, Easing easing) {
        this.duration = duration;
        this.easing = easing;
    }

    public void animate(boolean visible) {
        double next = visible ? 1 : 0;
        if (target != next) {
            update();
            start = value;
            target = next;
            changedAt = System.nanoTime();
        }
    }

    public void update() {
        double progress = Math.min(1, (System.nanoTime() - changedAt) / (Math.max(1, duration) * 1_000_000d));
        value = start + (target - start) * easing.apply(progress);
    }

    public double get() { return value; }
    public void setDuration(long duration) { this.duration = duration; }
    public void setEasing(Easing easing) { this.easing = easing; }
    public void setValue(double value) {
        this.value = this.start = this.target = value;
        changedAt = System.nanoTime();
    }
}
