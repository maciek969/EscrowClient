package elexis.client.utils.math.time;

public class TimerUtil {
    private long lastMs = System.currentTimeMillis();

    public void reset() {
        lastMs = System.currentTimeMillis();
    }

    public boolean isReached(long time) {
        return System.currentTimeMillis() - lastMs >= time;
    }

    public boolean finished(double delay) {
        return System.currentTimeMillis() - lastMs >= delay;
    }

    public boolean passed(long ms) {
        return System.currentTimeMillis() - lastMs >= ms;
    }

    public long elapsedTime() {
        return System.currentTimeMillis() - lastMs;
    }
}
