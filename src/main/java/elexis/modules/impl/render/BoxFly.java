package elexis.modules.impl.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.SliderSetting;
import lombok.Getter;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@FunctionAdd(name = "BoxFly", alias = "BoxFly", category = Category.Render)
public class BoxFly extends Function {
    private final SliderSetting amount = new SliderSetting("Amount", 30f, 10f, 100f, 5f);
    private final SliderSetting size = new SliderSetting("Size", 0.15f, 0.05f, 0.5f, 0.01f);
    
    private final List<Box> boxes = new ArrayList<>();
    
    private static final RenderPipeline BOX_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
                    .withLocation(Identifier.of("elexis", "pipeline/boxfly/box"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .build());
    
    private static final RenderPipeline LINES_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
                    .withLocation(Identifier.of("elexis", "pipeline/boxfly/lines"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.DEBUG_LINES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .build());
    
    private static final RenderLayer BOX_LAYER = RenderLayer.of(
            "elexis/boxfly/box",
            RenderSetup.builder(BOX_PIPELINE).expectedBufferSize(262144).build());
    private static final RenderLayer LINES_LAYER = RenderLayer.of(
            "elexis/boxfly/lines",
            RenderSetup.builder(LINES_PIPELINE).expectedBufferSize(262144).build());

    public BoxFly() {
        addSettings(amount, size);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        boxes.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        boxes.clear();
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (mc.player == null || mc.world == null) {
            boxes.clear();
            return;
        }
        
        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        
        Iterator<Box> iterator = boxes.iterator();
        while (iterator.hasNext()) {
            Box box = iterator.next();
            if (box.isDead() || box.pos.distanceTo(playerPos) > 30) {
                iterator.remove();
                continue;
            }
            BlockPos blockPos = new BlockPos((int) box.pos.x, (int) box.pos.y, (int) box.pos.z);
            if (!mc.world.getBlockState(blockPos).isAir()) {
                iterator.remove();
            }
        }

        int max = (int) amount.get().floatValue();
        int toSpawn = max - boxes.size();
        if (toSpawn > 0) {
            float yaw = mc.player.getYaw();
            double lx = -Math.sin(Math.toRadians(yaw));
            double lz = Math.cos(Math.toRadians(yaw));
            
            for (int i = 0; i < toSpawn; i++) {
                double cx, cz;
                if (Math.random() < 0.7) {
                    double frontDistance = 5 + Math.random() * 15;
                    double sideDistance = (Math.random() - 0.5) * 15;
                    cx = playerPos.x + lx * frontDistance + lz * sideDistance;
                    cz = playerPos.z + lz * frontDistance - lx * sideDistance;
                } else {
                    cx = playerPos.x + (Math.random() - 0.5) * 30;
                    cz = playerPos.z + (Math.random() - 0.5) * 30;
                }
                
                double cy = playerPos.y + Math.random() * 17;
                BlockPos spawnPos = new BlockPos((int) cx, (int) cy, (int) cz);
                
                if (!mc.world.getBlockState(spawnPos).isAir()) continue;
                
                boxes.add(new Box(new Vec3d(cx, cy, cz), size.get()));
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender.World event) {
        if (mc.player == null || mc.world == null || boxes.isEmpty()) return;
        
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d cameraPos = camera.getCameraPos();
        MatrixStack matrixStack = event.getMatrixStack();
        BufferAllocator allocator = new BufferAllocator(262144);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        
        try {
            for (Box box : boxes) {
                box.tick();
                if (box.isDead()) continue;
                
                Vec3d relativePos = box.pos.subtract(cameraPos);
                float alpha = box.getAlpha();
                
                matrixStack.push();
                matrixStack.translate(relativePos.x, relativePos.y, relativePos.z);
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(box.rx));
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(box.ry));
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(box.rz));
                
                drawBox(immediate, matrixStack, box.size, (int)(alpha * 35));
                drawOutline(immediate, matrixStack, box.size, (int)(alpha * 200));
                drawBox(immediate, matrixStack, box.size * 0.35f, (int)(alpha * 255));
                matrixStack.pop();
            }
            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    private void drawBox(VertexConsumerProvider provider, MatrixStack matrixStack, float size, int alpha) {
        if (alpha <= 0) return;
        
        float half = size / 2f;
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        VertexConsumer buffer = provider.getBuffer(BOX_LAYER);
        
        float[][] vertices = {
            {-half, -half, -half, half, -half, -half, half, half, -half, -half, half, -half},
            {-half, -half, half, -half, half, half, half, half, half, half, -half, half},
            {-half, half, -half, half, half, -half, half, half, half, -half, half, half},
            {-half, -half, -half, -half, -half, half, half, -half, half, half, -half, -half},
            {-half, -half, -half, -half, -half, half, -half, half, half, -half, half, -half},
            {half, -half, -half, half, -half, half, half, half, half, half, half, -half}
        };
        
        for (float[] face : vertices) {
            for (int i = 0; i < 12; i += 3) {
                buffer.vertex(matrix, face[i], face[i+1], face[i+2]).color(255, 255, 255, alpha);
            }
        }
    }

    private void drawOutline(VertexConsumerProvider provider, MatrixStack matrixStack, float size, int alpha) {
        if (alpha <= 0) return;
        
        float half = size / 2f;
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        VertexConsumer buffer = provider.getBuffer(LINES_LAYER);
        
        float[][] edges = {
            {-half, -half, -half, half, -half, -half},
            {half, -half, -half, half, -half, half},
            {half, -half, half, -half, -half, half},
            {-half, -half, half, -half, -half, -half},
            {-half, half, -half, half, half, -half},
            {half, half, -half, half, half, half},
            {half, half, half, -half, half, half},
            {-half, half, half, -half, half, -half},
            {-half, -half, -half, -half, half, -half},
            {half, -half, -half, half, half, -half},
            {half, -half, half, half, half, half},
            {-half, -half, half, -half, half, half}
        };
        
        for (float[] edge : edges) {
            buffer.vertex(matrix, edge[0], edge[1], edge[2]).color(255, 255, 255, alpha);
            buffer.vertex(matrix, edge[3], edge[4], edge[5]).color(255, 255, 255, alpha);
        }
    }

    @Getter
    private static class Box {
        Vec3d pos, velocity;
        float rx, ry, rz, rsx, rsy, rsz, size;
        long spawnTime;

        Box(Vec3d position, float boxSize) {
            pos = position;
            size = boxSize;
            
            double angle = Math.random() * Math.PI * 2;
            double speed = 0.02 + Math.random() * 0.02;
            velocity = new Vec3d(Math.cos(angle) * speed, (Math.random() - 0.5) * 0.02, Math.sin(angle) * speed);
            
            rx = (float)(Math.random() * 360);
            ry = (float)(Math.random() * 360);
            rz = (float)(Math.random() * 360);
            
            rsx = (float)((Math.random() - 0.5) * 3);
            rsy = (float)((Math.random() - 0.5) * 3);
            rsz = (float)((Math.random() - 0.5) * 3);
            
            spawnTime = System.currentTimeMillis();
        }

        void tick() {
            velocity = velocity.multiply(0.98);
            pos = pos.add(velocity);
            rx += rsx;
            ry += rsy;
            rz += rsz;
        }

        boolean isDead() {
            return System.currentTimeMillis() - spawnTime > 10000;
        }

        float getAlpha() {
            long timeAlive = System.currentTimeMillis() - spawnTime;
            if (timeAlive < 100) {
                return timeAlive / 100f;
            }
            if (timeAlive > 7000) {
                return 1f - ((timeAlive - 7000) / 3000f);
            }
            return 1f;
        }
    }
}
