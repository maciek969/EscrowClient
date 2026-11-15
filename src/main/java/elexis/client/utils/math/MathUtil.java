package elexis.client.utils.math;

import elexis.client.utils.client.IMinecraft;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class MathUtil implements IMinecraft {
    public static final double TO_DEGREES = 180d / Math.PI;
    public static final double TO_RADIANS = Math.PI / 180d;
    public static final float TO_RADIANS_F = (float)Math.PI / 180f;


    public static final double TWO_PI = Math.PI * 2;
    public static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    public static final float RAD_TO_DEG = (float) (180.0 / Math.PI);

    public static long ARROW = GLFW.glfwCreateStandardCursor(221185);
    public static long HAND = GLFW.glfwCreateStandardCursor(221188);
    public static long RESIZEH = GLFW.glfwCreateStandardCursor(221189);
    public static long IBEAM = GLFW.glfwCreateStandardCursor(221186);

    public static boolean mouseIn(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static void scale(MatrixStack stack, float x, float y, float scale) {
        scale(stack, x, y, scale, scale);
    }

    public static void scale(MatrixStack stack, float x, float y, double scaleX, double scaleY) {
        stack.translate(scaledX(x), scaledY(y), 0);
        stack.scale((float) scaleX, (float) scaleY, 1);
        stack.translate(-scaledX(x), -scaledY(y), 0);
    }

    public static void scale(MatrixStack stack, float x, float y, float scaleX, float scaleY) {
        stack.translate(scaledX(x), scaledY(y), 0);
        stack.scale(scaleX, scaleY, 1);
        stack.translate(-scaledX(x), -scaledY(y), 0);
    }

    public static float scaledX(float f) {
        var window = MinecraftClient.getInstance().getWindow();
        if (window != null) {
            return f / ((float) window.getScaledWidth() / window.getWidth());
        } else {
            return 0;
        }

    }

    public static float scaledY(float f) {
        var window = MinecraftClient.getInstance().getWindow();
        if (window != null) {
            return f / ((float) window.getScaledHeight() / window.getHeight());
        } else {
            return 0;
        }

    }

    public static double getScaleFactor() {
        return mc.getWindow().getScaleFactor();
    }

    public static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    public static Double interpolate(double old, double value, double interpolation) {
        return (old + (value - old) * interpolation);
    }

    public static float linearInterpolate(float start, float end, float speed) {
        return start + Math.signum(end - start) * Math.min(Math.abs(end - start), speed * 70);
    }

    public float interpolate(float prev, float orig) {
        return MathHelper.lerp(mc.getRenderTickCounter().getTickProgress(false), prev, orig);
    }

    public double interpolate(double prev, double orig) {
        return MathHelper.lerp(mc.getRenderTickCounter().getTickProgress(false), prev, orig);
    }

    public static double roundToDecimal(double n, int point) {
        if (point == 0) {
            return Math.floor(n);
        }
        double factor = Math.pow(10, point);
        return Math.round(n * factor) / factor;
    }

    public static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float normalizeAngle(float angle) {
        while (angle > Math.PI) angle -= (float) TWO_PI;
        while (angle < -Math.PI) angle += (float) TWO_PI;
        return angle;
    }

    public static float toRadians(float degrees) {
        return degrees * DEG_TO_RAD;
    }

    public static float toDegrees(float radians) {
        return radians * RAD_TO_DEG;
    }

    public static double distanceTo(double posX, double posY, double posZ,
                                    double targetX, double targetY, double targetZ) {
        double dx = targetX - posX;
        double dy = targetY - posY;
        double dz = targetZ - posZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static float randomRange(float min, float max) {
        return min + (float) (Math.random() * (max - min));
    }

    public static boolean isInRange(float value, float min, float max) {
        return value >= min && value <= max;
    }

    public static float angleBetween(float x1, float y1, float x2, float y2) {
        return (float) Math.atan2(y2 - y1, x2 - x1);
    }

    public static boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
    }

    public static boolean isHovered(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
    }

    public static double interpolateRotation(double oldValue, double newValue, double interpolationValue) {
        return (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static float interpolate(float current, float old, float scale) {
        return old + (current - old) * scale;
    }

    public static float lerpAngle(float from, float to, float factor) {
        float diff = ((to - from + 180f) % 360f) - 180f;
        if (diff < -180f) diff += 360f;
        return from + diff * factor;
    }

    public static int calcResolution(int value) {
        Window rs = mc.getWindow();
        return (int) (value * rs.getScaleFactor() / 2);
    }

    public static Vec3d getRotationVector(float pitch, float yaw) {
        float f = pitch * ((float)Math.PI / 180F);
        float g = -yaw * ((float)Math.PI / 180F);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d((double)(i * j), (double)(-k), (double)(h * j));
    }

    public static int random(int min, int max) {
        return ThreadLocalRandom.current().nextInt() * (max - min) + min;
    }

    public static double random(double min, double max) {
        return ThreadLocalRandom.current().nextDouble() * (max - min) + min;
    }

    public static float random(float min, float max) {
        return (float) (Math.random() * (max - min) + min);
    }

    public double round(double num, double increment) {
        double rounded = Math.round(num / increment) * increment;
        return Math.round(rounded * 100.0) / 100.0;
    }

    public int getRandom(int min, int max) {
        return (int) getRandom((float) min, (float) max + 1);
    }

    public float getRandom(float min, float max) {
        return (float) getRandom(min, (double) max);
    }

    public double getRandom(double min, double max) {
        if (min == max) {
            return min;
        } else {
            if (min > max) {
                double d = min;
                min = max;
                max = d;
            }

            return ThreadLocalRandom.current().nextDouble(min, max);
        }
    }

    public float interpolateSmooth(double smooth, float prev, float orig) {
        return (float) MathHelper.lerp(0.1f / smooth, prev, orig);
    }

    public float angleDifference(float a, float b) {
        return MathHelper.wrapDegrees(a - b);
    }

    public int floorNearestMulN(int x, int n) {
        return n * (int) Math.floor((double) x / (double) n);
    }

    public float convertFastestToDefault(float value) {
        return value / 6;
    }

    public float convertFastestToDefault(float value, float factor) {
        return value / factor;
    }

    public float convertLerpFastestToDefault(float value) {
        return value / 6500;
    }
}
