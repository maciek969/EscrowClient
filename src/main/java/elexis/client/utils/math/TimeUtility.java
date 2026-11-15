package elexis.client.utils.math;

public class TimeUtility {
    private long lastMS = System.currentTimeMillis();

    public boolean reached(long ms) {
        return getTime() >= ms;
    }

    public boolean reached(long ms, boolean reset) {
        boolean reached = reached(ms);
        if (reached && reset) {
            reset();
        }
        return reached;
    }

    public long getTime() {
        return System.currentTimeMillis() - lastMS;
    }

    public void reset() {
        lastMS = System.currentTimeMillis();
    }

    public void setTime(long time) {
        lastMS = time;
    }
}
