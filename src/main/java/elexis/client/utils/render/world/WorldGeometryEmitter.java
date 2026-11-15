package elexis.client.utils.render.world;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Objects;

public final class WorldGeometryEmitter {
    private final Matrix4f positionMatrix=new Matrix4f();
    private final Matrix3f normalMatrix=new Matrix3f();
    private final Vector3f normal=new Vector3f();
    private VertexConsumer consumer;
    private Vec3d cameraPosition;

    public WorldGeometryEmitter(Camera camera, MatrixStack.Entry entry, VertexConsumer consumer) {
        reset(camera,entry,consumer);
    }

    public void reset(Camera camera, MatrixStack.Entry entry, VertexConsumer consumer) {
        Objects.requireNonNull(camera, "camera");
        Objects.requireNonNull(entry, "entry");
        this.consumer = Objects.requireNonNull(consumer, "consumer");
        this.cameraPosition = camera.getCameraPos();
        this.positionMatrix.set(entry.getPositionMatrix());
        this.normalMatrix.set(entry.getNormalMatrix());
    }

    public void emitLine(Vec3d start, Vec3d end, int startColor, int endColor) {
        emitLine(start.x,start.y,start.z,end.x,end.y,end.z,startColor,endColor);
    }

    public void emitLine(Vec3d start, Vec3d end, int color) {
        emitLine(start, end, color, color);
    }

    public void emitLine(double x1,double y1,double z1,double x2,double y2,double z2,int color) {
        emitLine(x1,y1,z1,x2,y2,z2,color,color);
    }

    public void emitLine(double x1,double y1,double z1,double x2,double y2,double z2,int startColor,int endColor) {
        normal.set((float)(x2-x1),(float)(y2-y1),(float)(z2-z1));
        if(normal.lengthSquared()<1e-6f) normal.set(0,1,0);
        normal.normalize();normalMatrix.transform(normal);
        if(normal.lengthSquared()<1e-6f) normal.set(0,1,0);
        normal.normalize();
        writeLineVertex(x1,y1,z1,startColor);writeLineVertex(x2,y2,z2,endColor);
    }

    private void writeLineVertex(double x,double y,double z,int rgbaColor) {
        consumer.vertex(positionMatrix, (float)(x-cameraPosition.x), (float)(y-cameraPosition.y), (float)(z-cameraPosition.z))
                .color((rgbaColor >> 16) & 0xFF, (rgbaColor >> 8) & 0xFF, rgbaColor & 0xFF, (rgbaColor >> 24) & 0xFF)
                .normal(normal.x, normal.y, normal.z);
    }

}
