package elexis.modules.impl.render;

import elexis.client.events.impl.entity.EventAttack;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.client.utils.render.main.oldrender.system.ClientPipelines;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ColorSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@FunctionAdd(name = "HitShot", alias = "HitShot", category = Category.Render, description = "Ghost snapshots of player movement")
public class HitShot extends Function {
    private static final long SNAPSHOT_COOLDOWN_MS = 500L;
    private static final int MAX_SNAPSHOTS_PER_PLAYER = 10;

    private final ModeSetting trigger = new ModeSetting("Trigger", "Always", "Always", "Jumps Only");
    private final ModeSetting show = new ModeSetting("Show", "Only Me", "Only Me", "All");
    private final BooleanSetting firstPerson = new BooleanSetting("First Person", false);
    private final BooleanSetting fill = new BooleanSetting("Fill", true);
    private final ModeSetting colorMode = new ModeSetting("Color", "Theme", "Theme", "Custom");
    private final ColorSetting customColor = new ColorSetting("Custom Color", new Color(255, 100, 100, 200).getRGB())
            .setVisible(() -> colorMode.is("Custom"));

    private final Map<UUID, List<HitShotSnapshot>> snapshots = new ConcurrentHashMap<>();
    private ClientWorld trackedWorld;

    public HitShot() {
        addSettings(trigger, show, firstPerson, fill, colorMode, customColor);
    }

    @Override
    public void onEnable() {
        clearSnapshots();
        trackedWorld = mc.world;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        clearSnapshots();
        trackedWorld = null;
        super.onDisable();
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (mc.world == null || mc.player == null) {
            clearSnapshots();
            trackedWorld = null;
            return;
        }

        if (trackedWorld != mc.world) {
            clearSnapshots();
            trackedWorld = mc.world;
        }

        long now = System.currentTimeMillis();
        pruneSnapshots(now);
    }

    @EventHandler
    public void onAttack(EventAttack event) {
        if (nullCheck() || !(event.getTarget() instanceof AbstractClientPlayerEntity target) || target == mc.player) {
            return;
        }

        long now = System.currentTimeMillis();
        Vec3d snapshotPos = new Vec3d(target.getX(), target.getY(), target.getZ());
        addSnapshot(target, snapshotPos, now);
    }

    @EventHandler
    public void onRender(EventRender.World event) {
        if (nullCheck() || snapshots.isEmpty()) {
            return;
        }

        BufferAllocator allocator = new BufferAllocator(262144);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        try {
            long now = System.currentTimeMillis();

            for (Map.Entry<UUID, List<HitShotSnapshot>> entry : snapshots.entrySet()) {
                boolean isLocal = entry.getKey().equals(mc.player.getUuid());
                if (isLocal && mc.options.getPerspective() == Perspective.FIRST_PERSON && !firstPerson.get()) {
                    continue;
                }

                for (HitShotSnapshot snapshot : entry.getValue()) {
                    if (snapshot.isExpired(now)) {
                        continue;
                    }

                    float alpha = snapshot.alpha(now);
                    if (alpha <= 0.01f) {
                        continue;
                    }

                    int lineColor = resolveColor(entry.getKey().hashCode(), alpha, 235);
                    int fillColor = resolveColor(entry.getKey().hashCode(), alpha, 70);
                    if (fill.get()) {
                        renderSnapshotFill(event.getMatrixStack(), immediate.getBuffer(ClientPipelines.QUAD), snapshot, fillColor, now);
                    }
                    renderSnapshotOutline(event.getMatrixStack(), immediate.getBuffer(Predictions.LINES_LAYER), snapshot, lineColor, now);
                }
            }

            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    private void pruneSnapshots(long now) {
        Iterator<Map.Entry<UUID, List<HitShotSnapshot>>> iterator = snapshots.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, List<HitShotSnapshot>> entry = iterator.next();
            entry.getValue().removeIf(snapshot -> snapshot.isExpired(now));
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

    private void clearSnapshots() {
        snapshots.clear();
    }

    private int resolveColor(int seed, float snapshotAlpha, int alpha) {
        int baseColor = colorMode.is("Custom")
                ? customColor.get()
                : ThemeColorUtils.getIconColor(10, seed);
        int scaledAlpha = Math.max(0, Math.min(255, Math.round(alpha * snapshotAlpha)));
        return ThemeColorUtils.applyOpacity(baseColor, scaledAlpha);
    }

    private void addSnapshot(AbstractClientPlayerEntity player, Vec3d snapshotPos, long now) {
        List<HitShotSnapshot> playerSnapshots = snapshots.computeIfAbsent(player.getUuid(), ignored -> new ArrayList<>());
        if (!playerSnapshots.isEmpty() && now - playerSnapshots.get(playerSnapshots.size() - 1).getCreatedAt() < SNAPSHOT_COOLDOWN_MS) {
            return;
        }

        playerSnapshots.add(HitShotSnapshot.capture(player, snapshotPos, now));
        while (playerSnapshots.size() > MAX_SNAPSHOTS_PER_PLAYER) {
            playerSnapshots.remove(0);
        }
    }

    private void renderSnapshotFill(MatrixStack matrices, VertexConsumer fillBuffer, HitShotSnapshot snapshot, int fillColor, long now) {
        Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();
        matrices.push();
        matrices.translate(
                snapshot.getPosition().x - camera.x,
                snapshot.getPosition().y - camera.y + snapshot.riseOffset(now),
                snapshot.getPosition().z - camera.z
        );
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - snapshot.getBodyYaw()));

        drawFilledSegment(matrices, fillBuffer, 0.0f, 0.90f, 0.0f, 0.50f, 0.75f, 0.25f, 0.0f, 0.0f, 0.0f, fillColor);
        drawFilledSegment(matrices, fillBuffer, 0.0f, 1.50f, 0.0f, 0.50f, 0.50f, 0.50f, snapshot.getPitch(), snapshot.getHeadYaw() - snapshot.getBodyYaw(), 0.0f, fillColor);
        drawFilledSegment(matrices, fillBuffer, -0.375f, 0.90f, 0.0f, 0.25f, 0.75f, 0.25f, 0.0f, 0.0f, -6.0f, fillColor);
        drawFilledSegment(matrices, fillBuffer, 0.375f, 0.90f, 0.0f, 0.25f, 0.75f, 0.25f, 0.0f, 0.0f, 6.0f, fillColor);
        drawFilledSegment(matrices, fillBuffer, -0.125f, 0.375f, 0.0f, 0.25f, 0.75f, 0.25f, 0.0f, 0.0f, 0.0f, fillColor);
        drawFilledSegment(matrices, fillBuffer, 0.125f, 0.375f, 0.0f, 0.25f, 0.75f, 0.25f, 0.0f, 0.0f, 0.0f, fillColor);
        matrices.pop();
    }

    private void renderSnapshotOutline(MatrixStack matrices, VertexConsumer lineBuffer, HitShotSnapshot snapshot, int lineColor, long now) {
        Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();
        matrices.push();
        matrices.translate(
                snapshot.getPosition().x - camera.x,
                snapshot.getPosition().y - camera.y + snapshot.riseOffset(now),
                snapshot.getPosition().z - camera.z
        );
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - snapshot.getBodyYaw()));

        drawOutlinedSegment(matrices, lineBuffer, 0.0f, 0.90f, 0.0f, 0.50f, 0.75f, 0.25f, 0.0f, 0.0f, 0.0f, lineColor);
        drawOutlinedSegment(
                matrices,
                lineBuffer,
                0.0f,
                1.50f,
                0.0f,
                0.50f,
                0.50f,
                0.50f,
                snapshot.getPitch(),
                snapshot.getHeadYaw() - snapshot.getBodyYaw(),
                0.0f,
                lineColor
        );
        drawOutlinedSegment(matrices, lineBuffer, -0.375f, 0.90f, 0.0f, 0.25f, 0.75f, 0.25f, 0.0f, 0.0f, -6.0f, lineColor);
        drawOutlinedSegment(matrices, lineBuffer, 0.375f, 0.90f, 0.0f, 0.25f, 0.75f, 0.25f, 0.0f, 0.0f, 6.0f, lineColor);
        drawOutlinedSegment(matrices, lineBuffer, -0.125f, 0.375f, 0.0f, 0.25f, 0.75f, 0.25f, 0.0f, 0.0f, 0.0f, lineColor);
        drawOutlinedSegment(matrices, lineBuffer, 0.125f, 0.375f, 0.0f, 0.25f, 0.75f, 0.25f, 0.0f, 0.0f, 0.0f, lineColor);
        matrices.pop();
    }

    private void drawFilledSegment(
            MatrixStack matrices,
            VertexConsumer fillBuffer,
            float offsetX,
            float offsetY,
            float offsetZ,
            float width,
            float height,
            float depth,
            float pitch,
            float yaw,
            float roll,
            int fillColor
    ) {
        matrices.push();
        matrices.translate(offsetX, offsetY, offsetZ);
        if (yaw != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        }
        if (pitch != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        }
        if (roll != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        }

        Box localBox = new Box(
                -width / 2.0f,
                -height / 2.0f,
                -depth / 2.0f,
                width / 2.0f,
                height / 2.0f,
                depth / 2.0f
        );

        drawFilledPrism(fillBuffer, matrices.peek().getPositionMatrix(), width * 0.5f, height * 0.5f, depth * 0.5f, fillColor);
        matrices.pop();
    }

    private void drawOutlinedSegment(
            MatrixStack matrices,
            VertexConsumer lineBuffer,
            float offsetX,
            float offsetY,
            float offsetZ,
            float width,
            float height,
            float depth,
            float pitch,
            float yaw,
            float roll,
            int lineColor
    ) {
        matrices.push();
        matrices.translate(offsetX, offsetY, offsetZ);
        if (yaw != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        }
        if (pitch != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        }
        if (roll != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        }

        Box localBox = new Box(
                -width / 2.0f,
                -height / 2.0f,
                -depth / 2.0f,
                width / 2.0f,
                height / 2.0f,
                depth / 2.0f
        );

        drawOutlinedBox(lineBuffer, matrices.peek(), localBox, lineColor);
        matrices.pop();
    }

    private void drawFilledPrism(VertexConsumer buffer, Matrix4f matrix, float halfWidth, float halfHeight, float halfDepth, int color) {
        solidQuad(buffer, matrix, -halfWidth, -halfHeight, halfDepth, halfWidth, halfDepth, color);
        solidQuad(buffer, matrix, -halfWidth, -halfHeight, -halfDepth, halfWidth, -halfDepth, color);
        solidQuad(buffer, matrix, -halfWidth, -halfHeight, -halfDepth, -halfWidth, halfDepth, color);
        solidQuad(buffer, matrix, halfWidth, -halfHeight, -halfDepth, halfWidth, halfDepth, color);
        solidQuad(buffer, matrix, -halfWidth, halfHeight, -halfDepth, halfWidth, halfDepth, color);
        solidQuad(buffer, matrix, -halfWidth, -halfHeight, -halfDepth, halfWidth, -halfDepth, color);
    }

    private void drawOutlinedBox(VertexConsumer buffer, MatrixStack.Entry entry, Box box, int color) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        line(buffer, entry, minX, minY, minZ, maxX, minY, minZ, color);
        line(buffer, entry, maxX, minY, minZ, maxX, minY, maxZ, color);
        line(buffer, entry, maxX, minY, maxZ, minX, minY, maxZ, color);
        line(buffer, entry, minX, minY, maxZ, minX, minY, minZ, color);

        line(buffer, entry, minX, maxY, minZ, maxX, maxY, minZ, color);
        line(buffer, entry, maxX, maxY, minZ, maxX, maxY, maxZ, color);
        line(buffer, entry, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        line(buffer, entry, minX, maxY, maxZ, minX, maxY, minZ, color);

        line(buffer, entry, minX, minY, minZ, minX, maxY, minZ, color);
        line(buffer, entry, maxX, minY, minZ, maxX, maxY, minZ, color);
        line(buffer, entry, maxX, minY, maxZ, maxX, maxY, maxZ, color);
        line(buffer, entry, minX, minY, maxZ, minX, maxY, maxZ, color);
    }

    private void solidQuad(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float z2, int color) {
        buffer.vertex(matrix, x1, y1, z1).color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF);
        buffer.vertex(matrix, x2, y1, z1).color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF);
        buffer.vertex(matrix, x2, -y1, z2).color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF);
        buffer.vertex(matrix, x1, -y1, z2).color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF);
    }

    private void line(VertexConsumer buffer, MatrixStack.Entry entry,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      int color) {
        vertex(buffer, entry, x1, y1, z1, color);
        vertex(buffer, entry, x2, y2, z2, color);
    }

    private void vertex(VertexConsumer buffer, MatrixStack.Entry entry, float x, float y, float z, int color) {
        buffer.vertex(entry.getPositionMatrix(), x, y, z)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >> 24) & 0xFF);
    }
}
