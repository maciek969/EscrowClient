package elexis.modules.impl.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ColorSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import lombok.Getter;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@FunctionAdd(name = "Invoker", alias = "Invoker", category = Category.Render)
public class Invoker extends Function {
    private final ColorSetting sphere1Color = new ColorSetting("Exort color", new Color(255, 200, 100, 200).getRGB());
    private final ColorSetting sphere2Color = new ColorSetting("Wex color", new Color(255, 150, 255, 200).getRGB());
    private final ColorSetting sphere3Color = new ColorSetting("Quas color", new Color(150, 150, 255, 200).getRGB());

    private final SliderSetting sphereSize = new SliderSetting("Sphere Size", 0.3f, 0.1f, 0.8f, 0.05f);
    private final SliderSetting orbitRadius = new SliderSetting("Orbit Radius", 0.8f, 0.3f, 2.0f, 0.1f);
    private final SliderSetting height = new SliderSetting("Height", 0.7f, -0.5f, 1.5f, 0.1f);
    private final SliderSetting rotationSpeed = new SliderSetting("Rotation Speed", 1.0f, 0.1f, 5.0f, 0.1f);
    private final SliderSetting trailLength = new SliderSetting("Trail Length", 20, 5, 50, 1);

    private final BooleanSetting renderSelf = new BooleanSetting("Render Self", true);
    private final BooleanSetting renderOthers = new BooleanSetting("Render Others", false);
    private final BooleanSetting renderInFirstPerson = new BooleanSetting("Render in First Person", true);
    private final BooleanSetting enableTrails = new BooleanSetting("Enable Trails", true);

    private static final Identifier GLOW_TEXTURE = Identifier.of("elexis", "textures/particles/glow.png");
    
    private static final RenderPipeline GLOW_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_TEX_COLOR_SNIPPET})
                    .withLocation(Identifier.of("elexis", "pipeline/invoker/glow"))
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build());
    
    private static final RenderPipeline TRAIL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
                    .withLocation(Identifier.of("elexis", "pipeline/invoker/trail"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.DEBUG_LINES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .build());
    
    private static final RenderLayer GLOW_LAYER = RenderLayer.of(
            "elexis/invoker/glow",
            RenderSetup.builder(GLOW_PIPELINE).expectedBufferSize(262144).build());
    private static final RenderLayer TRAIL_LAYER = RenderLayer.of(
            "elexis/invoker/trail",
            RenderSetup.builder(TRAIL_PIPELINE).expectedBufferSize(262144).build());

    private final List<SphereTrail>[] sphereTrails = new List[3];
    private float rotationOffset = 0f;

    public Invoker() {
        addSettings(sphere1Color, sphere2Color, sphere3Color, sphereSize, orbitRadius, height, rotationSpeed, trailLength,
                renderSelf, renderOthers, renderInFirstPerson, enableTrails);
        
        for (int i = 0; i < 3; i++) {
            sphereTrails[i] = new ArrayList<>();
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        for (int i = 0; i < 3; i++) {
            sphereTrails[i].clear();
        }
        rotationOffset = 0f;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        for (int i = 0; i < 3; i++) {
            sphereTrails[i].clear();
        }
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (mc.player == null || mc.world == null) return;
        
        rotationOffset += rotationSpeed.get();
        
        if (!enableTrails.get()) return;
        
        // Update trails for each sphere
        updateTrails(mc.player);
    }

    private void updateTrails(PlayerEntity player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        float bodyYaw = player.bodyYaw;
        
        float sphereY = player.getHeight() * 0.5f + height.get();
        float radius = orbitRadius.get();
        
        float[] angleOffsets = {180f, -90f, 360f};
        
        for (int i = 0; i < 3; i++) {
            float angle = angleOffsets[i] + rotationOffset;
            double angleRad = Math.toRadians(bodyYaw + angle);
            
            double sphereX = x + Math.cos(angleRad) * radius;
            double sphereY_pos = y + sphereY;
            double sphereZ = z + Math.sin(angleRad) * radius;
            
            Vec3d pos = new Vec3d(sphereX, sphereY_pos, sphereZ);
            
            sphereTrails[i].add(new SphereTrail(pos, System.currentTimeMillis()));
            
            // Remove old trails
            int maxTrails = trailLength.get().intValue();
            while (sphereTrails[i].size() > maxTrails) {
                sphereTrails[i].remove(0);
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender.World event) {
        if (mc.player == null || mc.world == null) return;
        
        MatrixStack matrixStack = event.getMatrixStack();
        BufferAllocator allocator = new BufferAllocator(262144);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        
        try {
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof PlayerEntity p)) continue;

                if (p == mc.player && !renderSelf.get()) continue;
                if (p != mc.player && !renderOthers.get()) continue;
                if (p.isInvisible()) continue;

                renderSpheres(matrixStack, immediate, p, event.getTicks());
            }
            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    private void renderSpheres(MatrixStack ms, VertexConsumerProvider provider, PlayerEntity player, float partialTicks) {
        if (player == mc.player && mc.options.getPerspective().isFirstPerson() && !renderInFirstPerson.get()) {
            return;
        }

        double x = MathHelper.lerp(partialTicks, player.lastRenderX, player.getX());
        double y = MathHelper.lerp(partialTicks, player.lastRenderY, player.getY());
        double z = MathHelper.lerp(partialTicks, player.lastRenderZ, player.getZ());

        float bodyYaw = MathHelper.lerpAngleDegrees(partialTicks, player.lastBodyYaw, player.bodyYaw);

        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        float sphereY = player.getHeight() * 0.5f + height.get();
        float radius = orbitRadius.get();
        float size = sphereSize.get();

        Color[] colors = {new Color(sphere1Color.get()), new Color(sphere2Color.get()), new Color(sphere3Color.get())};
        float[] angleOffsets = {180f, -90f, 360f};

        for (int i = 0; i < 3; i++) {
            float angle = angleOffsets[i] + rotationOffset;

            double angleRad = Math.toRadians(bodyYaw + angle);

            double sphereX = x + Math.cos(angleRad) * radius;
            double sphereY_pos = y + sphereY;
            double sphereZ = z + Math.sin(angleRad) * radius;

            // Render trails
            if (enableTrails.get() && player == mc.player) {
                renderTrails(ms, provider, sphereTrails[i], cam, colors[i]);
            }

            renderSphere(ms, provider, sphereX, sphereY_pos, sphereZ, cam, size, colors[i]);
        }
    }

    private void renderTrails(MatrixStack ms, VertexConsumerProvider provider, List<SphereTrail> trails, Vec3d cam, Color color) {
        if (trails.size() < 2) return;
        
        Matrix4f matrix = ms.peek().getPositionMatrix();
        VertexConsumer buffer = provider.getBuffer(TRAIL_LAYER);
        
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        
        long currentTime = System.currentTimeMillis();
        
        for (int i = 0; i < trails.size() - 1; i++) {
            SphereTrail current = trails.get(i);
            SphereTrail next = trails.get(i + 1);
            
            float alpha = calculateTrailAlpha(current.time, currentTime, trails.size(), i);
            
            Vec3d currentPos = current.pos.subtract(cam);
            Vec3d nextPos = next.pos.subtract(cam);
            
            buffer.vertex(matrix, (float)currentPos.x, (float)currentPos.y, (float)currentPos.z).color(r, g, b, alpha);
            buffer.vertex(matrix, (float)nextPos.x, (float)nextPos.y, (float)nextPos.z).color(r, g, b, alpha);
        }
    }

    private float calculateTrailAlpha(long trailTime, long currentTime, int totalTrails, int index) {
        long age = currentTime - trailTime;
        float maxAge = 1000f; // 1 second
        float fade = 1.0f - Math.min(age / maxAge, 1.0f);
        
        // Also fade based on position in trail (older = more transparent)
        float positionFade = (float)index / totalTrails;
        
        return fade * positionFade * 0.5f;
    }

    private void renderSphere(MatrixStack ms, VertexConsumerProvider provider, double x, double y, double z, Vec3d cam, float size, Color color) {
        ms.push();

        double renderX = x - cam.x;
        double renderY = y - cam.y;
        double renderZ = z - cam.z;

        ms.translate(renderX, renderY, renderZ);

        Camera camera = mc.gameRenderer.getCamera();
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        // Add rotation animation
        float time = System.currentTimeMillis() / 1000f;
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(time * 30f));

        Matrix4f matrix = ms.peek().getPositionMatrix();
        VertexConsumer buffer = provider.getBuffer(GLOW_LAYER);

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        buffer.vertex(matrix, -size, size, 0).texture(0f, 1f).color(r, g, b, a);
        buffer.vertex(matrix, size, size, 0).texture(1f, 1f).color(r, g, b, a);
        buffer.vertex(matrix, size, -size, 0).texture(1f, 0f).color(r, g, b, a);
        buffer.vertex(matrix, -size, -size, 0).texture(0f, 0f).color(r, g, b, a);

        ms.pop();
    }

    @Getter
    private static class SphereTrail {
        private final Vec3d pos;
        private final long time;

        SphereTrail(Vec3d pos, long time) {
            this.pos = pos;
            this.time = time;
        }
    }
}
