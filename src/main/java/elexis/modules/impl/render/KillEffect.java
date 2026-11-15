package elexis.modules.impl.render;

import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.events.impl.entity.EventAttack;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.client.utils.render.main.oldrender.system.ClientPipelines;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@FunctionAdd(name = "KillEffect", alias = "KillEffect", category = Category.Render, description = "Shows effect when a player dies")
@Environment(EnvType.CLIENT)
public class KillEffect extends Function {
    private static final Identifier KILL_SOUND_ID = Identifier.of("elexis", "kolokolnia_kill");
    private static final SoundEvent KILL_SOUND = SoundEvent.of(KILL_SOUND_ID);
    private static final long EFFECT_TIME_MS = 3600L;
    private static final long KILL_CONFIRM_MS = 1400L;
    private static final float SKIN_SIZE = 64.0F;

    private final Map<Integer, DeathMark> marks = new LinkedHashMap<>();
    private final Map<Integer, TrackedPlayer> trackedPlayers = new HashMap<>();
    private int lastAttackedId = -1;
    private Vec3d lastAttackedPos = Vec3d.ZERO;
    private Identifier lastAttackedSkin;
    private long lastAttackTime;
    private final Runnable restartEffectTask = this::restartEffect;

    @Override
    public void onEnable() {
        super.onEnable();
        restartEffectTask.run();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        restartEffectTask.run();
    }

    public void restartEffect() {
        marks.clear();
        trackedPlayers.clear();
        lastAttackedId = -1;
        lastAttackedPos = Vec3d.ZERO;
        lastAttackedSkin = null;
        lastAttackTime = 0L;
    }

    @EventHandler
    public void onAttack(EventAttack event) {
        if (event.getTarget() instanceof PlayerEntity player && player != mc.player) {
            lastAttackedId = player.getId();
            lastAttackedPos = position(player);
            lastAttackedSkin = skin(player);
            lastAttackTime = System.currentTimeMillis();
            trackedPlayers.put(player.getId(), new TrackedPlayer(position(player), player.getHealth(), lastAttackedSkin, lastAttackTime));
        }
    }

    @EventHandler
    public void onPacket(EventReceivePacket event) {
        if (mc.world == null || !(event.getPacket() instanceof EntityStatusS2CPacket packet)) {
            return;
        }
        Entity entity = packet.getEntity(mc.world);
        if (entity instanceof PlayerEntity player && player != mc.player && packet.getStatus() == 3 && wasKilledByMe(player.getId())) {
            addMark(player.getId(), position(player), skin(player));
        }
    }

    @EventHandler
    public void onUpdate(EventUpdate.Fast event) {
        if (nullCheck()) {
            marks.clear();
            trackedPlayers.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Set<Integer> present = new HashSet<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) {
                continue;
            }
            int id = player.getId();
            present.add(id);
            Vec3d pos = position(player);
            float health = player.getHealth();
            Identifier skin = skin(player);
            if ((player.deathTime > 0 || health <= 0.0F) && wasKilledByMe(id)) {
                addMark(id, pos, skin);
            }
            trackedPlayers.put(id, new TrackedPlayer(pos, health, skin, now));
        }

        Iterator<Map.Entry<Integer, TrackedPlayer>> iterator = trackedPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, TrackedPlayer> entry = iterator.next();
            int id = entry.getKey();
            TrackedPlayer tracked = entry.getValue();
            if (present.contains(id)) {
                continue;
            }
            if (wasKilledByMe(id)) {
                addMark(id, lastAttackedPos, lastAttackedSkin != null ? lastAttackedSkin : tracked.skin);
            }
            iterator.remove();
        }

        marks.values().removeIf(mark -> now - mark.created > EFFECT_TIME_MS);
    }

    @EventHandler
    public void onRender(EventRender.World event) {
        if (nullCheck() || marks.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();
        BufferAllocator allocator = new BufferAllocator(65536);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        try {
            MatrixStack matrices = event.getMatrixStack();
            Iterator<DeathMark> iterator = marks.values().iterator();
            while (iterator.hasNext()) {
                DeathMark mark = iterator.next();
                float age = (now - mark.created) / (float) EFFECT_TIME_MS;
                if (age >= 1.0F) {
                    iterator.remove();
                    continue;
                }
                VertexConsumer buffer = immediate.getBuffer(ClientPipelines.TARGET_ESP.apply(mark.skin));
                drawFlyingSkin(buffer, matrices, mark, camera, age);
            }
            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    private boolean wasKilledByMe(int id) {
        return id == lastAttackedId && System.currentTimeMillis() - lastAttackTime < KILL_CONFIRM_MS;
    }

    private void addMark(int id, Vec3d pos, Identifier skin) {
        if (marks.containsKey(id)) {
            return;
        }
        marks.put(id, new DeathMark(pos, skin != null ? skin : fallbackSkin(), System.currentTimeMillis()));
        if (mc.player != null && mc.player.squaredDistanceTo(pos.x, pos.y, pos.z) < 900.0D) {
            mc.player.playSound(KILL_SOUND, 1.0F, 1.0F);
        }
    }

    private Vec3d position(PlayerEntity player) {
        return new Vec3d(player.getX(), player.getY(), player.getZ());
    }

    private Identifier skin(PlayerEntity player) {
        Identifier id = null;
        try {
            if (mc.getNetworkHandler() != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
                if (entry != null) {
                    id = entry.getSkinTextures().body().texturePath();
                }
            }
            if (id == null && player instanceof AbstractClientPlayerEntity clientPlayer) {
                id = clientPlayer.getSkin().body().texturePath();
            }
        } catch (RuntimeException ignored) {
        }
        return id != null ? id : fallbackSkin();
    }

    private Identifier fallbackSkin() {
        Identifier id = null;
        try {
            if (mc.player != null) {
                if (mc.player instanceof AbstractClientPlayerEntity clientPlayer) {
                    id = clientPlayer.getSkin().body().texturePath();
                }
                if (id == null && mc.getNetworkHandler() != null) {
                    PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                    if (entry != null) {
                        id = entry.getSkinTextures().body().texturePath();
                    }
                }
            }
        } catch (RuntimeException ignored) {
        }
        return id != null ? id : Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
    }

    private void drawFlyingSkin(VertexConsumer buffer, MatrixStack matrices, DeathMark mark, Vec3d camera, float age) {
        float alpha = Math.max(0.0F, 1.0F - age);
        int color = ThemeColorUtils.applyOpacity(0xFFFFFFFF, (int) (255.0F * alpha));
        float drift = (float) Math.sin(age * Math.PI * 2.0D) * 0.14F;
        float scale = 0.9F + age * 0.22F;

        matrices.push();
        matrices.translate(mark.pos.x - camera.x + drift, mark.pos.y - camera.y + 0.25F + age * 2.45F, mark.pos.z - camera.z - drift * 0.4F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
        matrices.scale(scale, scale, scale);

        drawSkinPart(buffer, matrices.peek(), -0.22F, 0.62F, 0.44F, 0.68F, 20, 20, 8, 12, color);
        drawSkinPart(buffer, matrices.peek(), -0.58F, 0.62F, 0.22F, 0.66F, 44, 20, 4, 12, color);
        drawSkinPart(buffer, matrices.peek(), 0.36F, 0.62F, 0.22F, 0.66F, 36, 52, 4, 12, color);
        drawSkinPart(buffer, matrices.peek(), -0.23F, 0.02F, 0.21F, 0.62F, 4, 20, 4, 12, color);
        drawSkinPart(buffer, matrices.peek(), 0.02F, 0.02F, 0.21F, 0.62F, 20, 52, 4, 12, color);

        matrices.push();
        matrices.translate(0.0F, 1.43F + age * 0.18F, -0.08F - age * 0.14F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-42.0F));
        drawSkinPart(buffer, matrices.peek(), -0.28F, -0.28F, 0.56F, 0.56F, 8, 8, 8, 8, color);
        drawSkinPart(buffer, matrices.peek(), -0.30F, -0.30F, 0.60F, 0.60F, 40, 8, 8, 8, color);
        matrices.pop();

        matrices.pop();
    }

    private void drawSkinPart(VertexConsumer buffer, MatrixStack.Entry entry, float x, float y, float width, float height, int u, int v, int uw, int vh, int color) {
        Matrix4f matrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();
        Vector3f normal = new Vector3f(0.0F, 0.0F, 1.0F);
        normalMatrix.transform(normal);
        normal.normalize();

        float u0 = u / SKIN_SIZE;
        float v0 = v / SKIN_SIZE;
        float u1 = (u + uw) / SKIN_SIZE;
        float v1 = (v + vh) / SKIN_SIZE;

        put(buffer, matrix, normal, x, y, 0.0F, u0, v1, color);
        put(buffer, matrix, normal, x + width, y, 0.0F, u1, v1, color);
        put(buffer, matrix, normal, x + width, y + height, 0.0F, u1, v0, color);
        put(buffer, matrix, normal, x, y + height, 0.0F, u0, v0, color);
    }

    private void put(VertexConsumer buffer, Matrix4f matrix, Vector3f normal, float x, float y, float z, float u, float v, int color) {
        buffer.vertex(matrix, x, y, z)
                .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >> 24) & 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(15728880)
                .normal(normal.x, normal.y, normal.z);
    }

    private record DeathMark(Vec3d pos, Identifier skin, long created) {}
    private record TrackedPlayer(Vec3d pos, float health, Identifier skin, long updated) {}
}
