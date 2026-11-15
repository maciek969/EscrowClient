package elexis.client.utils.math;

import elexis.client.utils.math.time.TimerUtil;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.ThreadLocalRandom;

public final class Mathf {
    public static final Matrix4f lastProjMat = new Matrix4f();
    public static final Matrix4f lastModMat = new Matrix4f();
    public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();

    private Mathf() {
    }

    public static float random(float min, float max) {
        return ThreadLocalRandom.current().nextFloat(min, max);
    }

    public static float randomValue(float min, float max) {
        return random(min, max);
    }

    public static double randomValue(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static double randomWithUpdate(double min, double max, long ms, TimerUtil timer) {
        if (timer.isReached(ms)) {
            timer.reset();
            return randomValue(min, max);
        }
        return randomValue(min, max);
    }

    public static double clamp(double min, double max, double value) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float min, float max, float value) {
        return Math.max(min, Math.min(max, value));
    }

    public static float step(float value, float step) {
        if (step == 0f) {
            return value;
        }
        return Math.round(value / step) * step;
    }

    public static float wrapAngleTo180(float angle) {
        angle %= 360.0f;
        if (angle >= 180.0f) {
            angle -= 360.0f;
        }
        if (angle < -180.0f) {
            angle += 360.0f;
        }
        return angle;
    }

    public static double lerp(double a, double b, double factor) {
        return a + factor * (b - a);
    }

    public static float lerp(float a, float b, float factor) {
        return a + factor * (b - a);
    }

    public static Vec3d worldSpaceToScreenSpace(Vec3d pos) {
        if (net.minecraft.client.MinecraftClient.getInstance().gameRenderer == null
                || net.minecraft.client.MinecraftClient.getInstance().getEntityRenderDispatcher().camera == null) {
            return null;
        }

        Camera camera = net.minecraft.client.MinecraftClient.getInstance().getEntityRenderDispatcher().camera;
        int displayHeight = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHeight();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

        Vector3f target = new Vector3f();
        Vec3d cameraPos = camera.getCameraPos();
        double deltaX = pos.x - cameraPos.x;
        double deltaY = pos.y - cameraPos.y;
        double deltaZ = pos.z - cameraPos.z;

        Vector4f transformed = new Vector4f((float) deltaX, (float) deltaY, (float) deltaZ, 1.0f).mul(lastWorldSpaceMatrix);
        Matrix4f projection = new Matrix4f(lastProjMat);
        Matrix4f modelView = new Matrix4f(lastModMat);
        projection.mul(modelView).project(transformed.x(), transformed.y(), transformed.z(), viewport, target);

        double scale = net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaleFactor();
        return new Vec3d(target.x / scale, (displayHeight - target.y) / scale, target.z);
    }
}
