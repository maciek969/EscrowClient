package elexis.modules.impl.render;

import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.events.impl.entity.EventAttack;
import elexis.client.events.impl.entity.EventMotion;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.math.Mathf;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.client.utils.render.main.oldrender.system.ClientPipelines;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.MultiBooleanSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@FunctionAdd(name = "Particles", alias = "Particles", category = Category.Render, description = "Spawns decorative particles for hits, throws and totems")
public class Particles extends Function {
    public static final MultiBooleanSetting events = new MultiBooleanSetting(
            "Spawn On",
            new BooleanSetting("Attack", true),
            new BooleanSetting("Throw", true),
            new BooleanSetting("World", false),
            new BooleanSetting("Totem", true)
    );
    public static final ModeSetting particleMode = new ModeSetting("Particle Type", "glow",
            "bucks1", "core1", "crest1", "cube1", "cubeblast1", "ded1", "firefly", "glow", "heart1",
            "light1", "show1", "snowbag1", "snowblast1", "snowbrich1", "snownew1", "sparkle", "star1");
    public static final SliderSetting size = new SliderSetting("Size", 0.5F, 0.0F, 1.0F, 0.1F, false);

    private final List<Particle> particles = new CopyOnWriteArrayList<>();

    public Particles() {
        addSettings(events, particleMode, size);
    }

    @EventHandler
    public void onAttack(EventAttack event) {
        if (!events.get("Attack") || event.getTarget() == null) {
            return;
        }
        Entity target = event.getTarget();
        for (int i = 0; i < 35; i++) {
            spawnParticle(
                    new Vec3d(target.getX(), target.getY() + Mathf.randomValue(0.0F, (float) target.getHeight()), target.getZ()),
                    new Vec3d(Mathf.randomValue(-6.0F, 6.0F), Mathf.randomValue(-6.0F, 6.0F), Mathf.randomValue(-6.0F, 6.0F)),
                    ThemeColorUtils.getIconColor(7, particles.size())
            );
        }
    }

    @EventHandler
    public void onPacket(EventReceivePacket event) {
        if (!events.get("Totem") || mc.world == null) {
            return;
        }
        if (!(event.getPacket() instanceof EntityStatusS2CPacket packet) || packet.getStatus() != 35) {
            return;
        }
        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }

        int yellow = ThemeColorUtils.getColor(255, 215, 60, 255);
        Vec3d base = new Vec3d(player.getX(), player.getY() + player.getHeight() * 0.65, player.getZ());
        for (int i = 0; i < 55; i++) {
            spawnParticle(
                    base.add(Mathf.randomValue(-0.35F, 0.35F), Mathf.randomValue(-0.2F, 0.45F), Mathf.randomValue(-0.35F, 0.35F)),
                    new Vec3d(Mathf.randomValue(-6.0F, 6.0F), Mathf.randomValue(-6.0F, 6.0F), Mathf.randomValue(-6.0F, 6.0F)),
                    yellow
            );
        }
    }

    @EventHandler
    public void onMotion(EventMotion event) {
        if (mc.world == null) {
            return;
        }

        if (events.get("Throw")) {
            for (Entity entity : mc.world.getEntities()) {
                if ((entity instanceof EnderPearlEntity || entity instanceof ArrowEntity || entity instanceof TridentEntity)
                        && (!(entity instanceof TridentEntity trident) || !trident.isOnGround())) {
                    boolean moving = entity.lastX != entity.getX() || entity.lastY != entity.getY() || entity.lastZ != entity.getZ();
                    if (!moving) {
                        continue;
                    }
                    Vec3d pos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
                    for (int i = 0; i < 4; i++) {
                        spawnParticle(
                                new Vec3d(
                                        pos.x + Random.create().nextDouble() * 0.4 - 0.2,
                                        pos.y + Random.create().nextDouble() * 0.4 - 0.2,
                                        pos.z + Random.create().nextDouble() * 0.4 - 0.2),
                                new Vec3d(
                                        Random.create().nextDouble() * 2.0 - 1.0,
                                        Random.create().nextDouble() * 0.6 - 0.3,
                                        Random.create().nextDouble() * 2.0 - 1.0),
                                ThemeColorUtils.getIconColor(7, particles.size())
                        );
                    }
                }
            }
        }

        if (events.get("World") && mc.player != null) {
            for (int i = 0; i < 3; i++) {
                spawnParticle(
                        new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()).add(Mathf.randomValue(-8.0F, 8.0F), Mathf.randomValue(0.0F, 6.0F), Mathf.randomValue(-8.0F, 8.0F)),
                        new Vec3d(mc.player.getVelocity().x + Mathf.randomValue(-0.4F, 0.4F), Mathf.randomValue(-0.1F, 0.1F), mc.player.getVelocity().z + Mathf.randomValue(-0.4F, 0.4F)),
                        ThemeColorUtils.getIconColor(7, particles.size())
                );
            }
        }

        particles.removeIf(particle -> particle.age() > 2200L);
    }

    @EventHandler
    public void onRender3D(EventRender.World event) {
        if (particles.isEmpty()) {
            return;
        }
        BufferAllocator allocator = new BufferAllocator(262144);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        try {
            for (Particle particle : particles) {
                particle.update();
                float alpha = particle.alpha();
                if (alpha <= 0.0f) {
                    continue;
                }
                RenderLayer layer = ClientPipelines.PARTICLES.apply(particle.texture());
                MatrixStack matrix = event.getMatrixStack();
                matrix.push();
                Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
                matrix.translate(particle.position.x - cam.x, particle.position.y - cam.y, particle.position.z - cam.z);
                matrix.multiply(mc.getEntityRenderDispatcher().camera.getRotation());
                MatrixStack.Entry entry = matrix.peek();
                float s = particle.size;
                int color = ThemeColorUtils.applyOpacity(particle.color, (int) (alpha * 255.0f));
                drawTexturedQuad(immediate.getBuffer(layer), entry.getPositionMatrix(), entry.getNormalMatrix(), -s, -s, s * 2.0f, s * 2.0f, color);
                matrix.pop();
            }
            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    private void spawnParticle(Vec3d position, Vec3d velocity, int color) {
        float particleSize = 0.05F + size.get() * 0.2F;
        particles.add(new Particle(resolveTexture(), position.add(0.0, particleSize, 0.0), velocity.multiply(0.02), color, particleSize));
    }

    private Identifier resolveTexture() {
        return Identifier.of("elexis", "particle/" + particleMode.get().toLowerCase() + ".png");
    }

    private void drawTexturedQuad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix, float x, float y, float width, float height, int color) {
        Vector3f normal = new Vector3f(0.0F, 0.0F, 1.0F);
        normalMatrix.transform(normal);
        normal.normalize();
        float x2 = x + width;
        float y2 = y + height;
        put(buffer, matrix, normal, x, y, 0.0F, 1.0F, color);
        put(buffer, matrix, normal, x2, y, 1.0F, 1.0F, color);
        put(buffer, matrix, normal, x2, y2, 1.0F, 0.0F, color);
        put(buffer, matrix, normal, x, y2, 0.0F, 0.0F, color);
    }

    private void put(VertexConsumer buffer, Matrix4f matrix, Vector3f normal, float x, float y, float u, float v, int color) {
        buffer.vertex(matrix, x, y, 0.0F)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(15728880)
                .normal(normal.x, normal.y, normal.z);
    }

    private static final class Particle {
        private final Identifier texture;
        private Vec3d position;
        private Vec3d velocity;
        private final int color;
        private final float size;
        private final long createdAt = System.currentTimeMillis();

        private Particle(Identifier texture, Vec3d position, Vec3d velocity, int color, float size) {
            this.texture = texture;
            this.position = position;
            this.velocity = velocity;
            this.color = color;
            this.size = size;
        }

        private void update() {
            position = position.add(velocity);
            velocity = new Vec3d(velocity.x * 0.98, velocity.y * 0.98, velocity.z * 0.98);
        }

        private long age() {
            return System.currentTimeMillis() - createdAt;
        }

        private float alpha() {
            long age = age();
            float fadeIn = Math.min(1.0f, age / 250.0f);
            float fadeOut = age > 1200L ? 1.0f - Math.min(1.0f, (age - 1200L) / 1000.0f) : 1.0f;
            return Math.max(0.0f, Math.min(fadeIn, fadeOut));
        }

        private Identifier texture() {
            return texture;
        }
    }
}

