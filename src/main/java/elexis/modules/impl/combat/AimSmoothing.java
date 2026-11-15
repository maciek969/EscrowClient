package elexis.modules.impl.combat;

/** Time-based angular approach with a speed limit and an exponential finish. */
public final class AimSmoothing {
    private AimSmoothing() {}

    /**
     * Returns the angular distance to advance during {@code seconds}.
     * Invalid or nonpositive inputs produce no movement. Callers can separately
     * cap elapsed time after a pause without making normal motion depend on FPS.
     */
    public static double step(double distanceDegrees, double speedDegreesPerSecond,
                              double smoothing, double seconds) {
        if (!Double.isFinite(distanceDegrees) || !Double.isFinite(speedDegreesPerSecond)
                || !Double.isFinite(smoothing) || !Double.isFinite(seconds)
                || distanceDegrees <= 0.0 || speedDegreesPerSecond <= 0.0
                || smoothing <= 0.0 || seconds <= 0.0) return 0.0;

        double rate = 20.0 / Math.max(1.0, smoothing);
        double threshold = speedDegreesPerSecond / rate;
        double movement;
        if (distanceDegrees > threshold) {
            double linearDistance = distanceDegrees - threshold;
            double linearSeconds = linearDistance / speedDegreesPerSecond;
            if (seconds <= linearSeconds) {
                movement = speedDegreesPerSecond * seconds;
            } else {
                movement = linearDistance
                        + threshold * -Math.expm1(-rate * (seconds - linearSeconds));
            }
        } else {
            // expm1 retains very small frame advances near the target.
            movement = distanceDegrees * -Math.expm1(-rate * seconds);
        }
        return Math.max(0.0, Math.min(distanceDegrees,
                Math.min(speedDegreesPerSecond * seconds, movement)));
    }
}
