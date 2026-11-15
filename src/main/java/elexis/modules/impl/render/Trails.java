package elexis.modules.impl.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import elexis.client.events.impl.entity.EventMotion;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.client.utils.render.main.oldrender.system.ClientPipelines;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@FunctionAdd(name = "Trails", alias = "Trails", category = Category.Render, description = "Renders a moving trail behind the player")
public class Trails extends Function {
    public static final SliderSetting delay = new SliderSetting("Delay", 1500.0F, 500.0F, 5000.0F, 100.0F, false);
    public static final SliderSetting size = new SliderSetting("Size", 1.0F, 0.1F, 3.0F, 0.1F, false);
    public static final BooleanSetting firstPerson = new BooleanSetting("First Person", false);

    private final List<Particle3D> particles = new ArrayList<>();
    private final Identifier bloom = Identifier.of("elexis", "textures/world/firefly.png");
    private long lastParticleSpawnTime = 0L;
    private long lastParticleUpdateTimeNanos = 0L;
    private double lastPlayerX;
    private double lastPlayerY;
    private double lastPlayerZ;

    public Trails() {
        addSettings(delay, size, firstPerson);
    }

    @Override
    public void onEnable() {
        particles.clear();
        lastParticleSpawnTime = System.currentTimeMillis();
        lastParticleUpdateTimeNanos = System.nanoTime();
        super.onEnable();
    }

    @EventHandler
    public void onMotion(EventMotion event) {
        if (mc.player == null) {
            return;
        }

        double currentX = mc.player.getX();
        double currentY = mc.player.getY();
        double currentZ = mc.player.getZ();
        double dx = Math.abs(currentX - lastPlayerX);
        double dy = Math.abs(currentY - lastPlayerY);
        double dz = Math.abs(currentZ - lastPlayerZ);
        lastPlayerX = currentX;
        lastPlayerY = currentY;
        lastPlayerZ = currentZ;

        long currentTime = System.currentTimeMillis();
        if ((dx > 5.0E-5F || dy > 5.0E-5F || dz > 5.0E-5F) && currentTime - lastParticleSpawnTime >= 20L) {
            double distance = -(mc.player.getWidth() / 2.0F);
            double yawRad = Math.toRadians(mc.player.renderYaw);
            double xOffset = -Math.sin(yawRad) * distance;
            double zOffset = Math.cos(yawRad) * distance;
            particles.add(new Particle3D(
                    new Vec3d(currentX + xOffset, currentY + mc.player.getHeight() * 0.4F, currentZ + zOffset),
                    new Vec3d(mc.player.getVelocity().x, 0.0, mc.player.getVelocity().z).multiply(1.5 + Math.random())
            ));
            lastParticleSpawnTime = currentTime;
        }

        Iterator<Particle3D> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle3D particle = iterator.next();
            if (particle.age() > delay.get() || particle.position.distanceTo(new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ())) >= 100.0) {
                iterator.remove();
            }
        }
    }

    @EventHandler
    public void onRender3D(EventRender.World event) {
        if (particles.isEmpty()) {
            return;
        }
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON && !firstPerson.get()) {
            return;
        }

        long currentTimeNanos = System.nanoTime();
        if (lastParticleUpdateTimeNanos == 0L) {
            lastParticleUpdateTimeNanos = currentTimeNanos;
        }
        long deltaTimeNanos = currentTimeNanos - lastParticleUpdateTimeNanos;
        int updateCount = (int) (deltaTimeNanos / 4_166_666L);
        if (updateCount > 0) {
            updateCount = Math.min(updateCount, 50);
            for (int i = 0; i < updateCount; i++) {
                updateParticles(0.004166667F);
            }
            lastParticleUpdateTimeNanos = currentTimeNanos - deltaTimeNanos % 4_166_666L;
        }

        BufferAllocator allocator = new BufferAllocator(262144);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        try {
            RenderLayer renderLayer = ClientPipelines.PARTICLES.apply(bloom);
            float quadSize = size.get();
            for (int i = 0; i < particles.size(); i++) {
                Particle3D particle = particles.get(i);
                float alpha = particle.alpha(delay.get().longValue());
                if (alpha <= 0.0f) {
                    continue;
                }
                int[] colors = ThemeColorUtils.getClientColor(10, (int) (alpha * 180.0f));
                MatrixStack matrix = event.getMatrixStack();
                matrix.push();
                Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
                matrix.translate(particle.position.x - cam.x, particle.position.y - cam.y + quadSize / 2.0f, particle.position.z - cam.z);
                matrix.multiply(mc.getEntityRenderDispatcher().camera.getRotation());
                MatrixStack.Entry entry = matrix.peek();
                drawTexturedQuad(immediate.getBuffer(renderLayer), entry.getPositionMatrix(), entry.getNormalMatrix(),
                        -quadSize / 2.0f, -quadSize, quadSize, quadSize, colors[0], colors[1], colors[2], colors[3]);
                matrix.pop();
            }
            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    private void updateParticles(float deltaTimeSeconds) {
        for (Particle3D particle : particles) {
            particle.update(deltaTimeSeconds);
        }
        float smooth = 0.05F;
        for (int i = 1; i < particles.size(); i++) {
            Particle3D prevParticle = particles.get(i - 1);
            Particle3D currParticle = particles.get(i);
            prevParticle.position = new Vec3d(
                    prevParticle.position.x + (currParticle.position.x - prevParticle.position.x) * smooth,
                    prevParticle.position.y + (currParticle.position.y - prevParticle.position.y) * smooth,
                    prevParticle.position.z + (currParticle.position.z - prevParticle.position.z) * smooth
            );
        }
    }

    private void drawTexturedQuad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix, float x, float y, float width, float height,
                                  int color1, int color2, int color3, int color4) {
        Vector3f normal = new Vector3f(0.0F, 0.0F, 1.0F);
        normalMatrix.transform(normal);
        normal.normalize();
        float x2 = x + width;
        float y2 = y + height;
        put(buffer, matrix, normal, x, y, 0.0F, 1.0F, color1);
        put(buffer, matrix, normal, x2, y, 1.0F, 1.0F, color2);
        put(buffer, matrix, normal, x2, y2, 1.0F, 0.0F, color3);
        put(buffer, matrix, normal, x, y2, 0.0F, 0.0F, color4);
    }

    private void put(VertexConsumer buffer, Matrix4f matrix, Vector3f normal, float x, float y, float u, float v, int color) {
        buffer.vertex(matrix, x, y, 0.0F)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(15728880)
                .normal(normal.x, normal.y, normal.z);
    }

    private static final class Particle3D {
        private Vec3d position;
        private final Vec3d velocity;
        private final long createdAt = System.currentTimeMillis();

        private Particle3D(Vec3d position, Vec3d velocity) {
            this.position = position;
            this.velocity = velocity.multiply(0.01);
        }

        private void update(float deltaTimeSeconds) {
            position = position.add(velocity.x * 20.0 * deltaTimeSeconds, velocity.y * 20.0 * deltaTimeSeconds, velocity.z * 20.0 * deltaTimeSeconds);
        }

        private long age() {
            return System.currentTimeMillis() - createdAt;
        }

        private float alpha(long lifeMs) {
            long age = age();
            float fadeIn = Math.min(1.0f, age / 400.0f);
            float fadeOut = age >= Math.max(0L, lifeMs - 400L) ? 1.0f - Math.min(1.0f, (age - Math.max(0L, lifeMs - 400L)) / 400.0f) : 1.0f;
            return Math.max(0.0f, Math.min(fadeIn, fadeOut));
        }
    }
}
