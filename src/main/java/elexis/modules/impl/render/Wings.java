package elexis.modules.impl.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@FunctionAdd(name = "Wings", alias = "Wings", category = Category.Render, description = "Draws stylized wings behind players")
public class Wings extends Function {
    private static final float DEFAULT_SPREAD = 8.0f;
    private static final int DEFAULT_ALPHA = 220;
    private static final int BUFFER_SIZE = 262144;

    private static final RenderPipeline TRIANGLES_ADDITIVE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
                    .withLocation(Identifier.of("elexis", "pipeline/world/wings_triangles_additive"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.TRIANGLES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build());

    private static final RenderPipeline TRIANGLES_TRANSLUCENT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
                    .withLocation(Identifier.of("elexis", "pipeline/world/wings_triangles_translucent"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.TRIANGLES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .build());

    private static final RenderPipeline LINES_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
                    .withLocation(Identifier.of("elexis", "pipeline/world/wings_lines"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.DEBUG_LINES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build());

    private static final RenderLayer WING_GLOW_LAYER = RenderLayer.of(
            "elexis/world/wings_glow",
            RenderSetup.builder(TRIANGLES_ADDITIVE_PIPELINE).expectedBufferSize(BUFFER_SIZE).build());
    private static final RenderLayer WING_FILL_LAYER = RenderLayer.of(
            "elexis/world/wings_fill",
            RenderSetup.builder(TRIANGLES_TRANSLUCENT_PIPELINE).expectedBufferSize(BUFFER_SIZE).build());
    private static final RenderLayer WING_LINES_LAYER = RenderLayer.of(
            "elexis/world/wings_lines",
            RenderSetup.builder(LINES_PIPELINE).expectedBufferSize(BUFFER_SIZE).build());

    private static final WingPoint[] SHAPE = {
            new WingPoint(0.08f, 0.10f, 0.88f), new WingPoint(0.28f, 0.34f, 0.78f),
            new WingPoint(0.56f, 0.82f, 0.62f), new WingPoint(0.86f, 0.30f, 0.52f),
            new WingPoint(1.14f, 0.46f, 0.40f), new WingPoint(1.24f, 0.04f, 0.30f),
            new WingPoint(1.02f, -0.18f, 0.28f), new WingPoint(1.18f, -0.64f, 0.22f),
            new WingPoint(0.86f, -0.46f, 0.20f), new WingPoint(0.80f, -0.98f, 0.14f),
            new WingPoint(0.54f, -0.74f, 0.16f), new WingPoint(0.30f, -1.16f, 0.12f),
            new WingPoint(0.10f, -0.54f, 0.18f)
    };

    private final BooleanSetting self = new BooleanSetting("Self", true);
    private final BooleanSetting players = new BooleanSetting("Players", false);
    private final SliderSetting size = new SliderSetting("Size", 1.0f, 0.75f, 1.35f, 0.05f, false);

    private float selfBodyYaw;
    private boolean selfBodyYawInitialized;
    private int cachedBaseColor = -1;
    private int cachedGlowColor = -1;
    private int cachedCoreColor = -1;
    private long lastColorUpdate = 0L;

    public Wings() {
        addSettings(self, players, size);
    }

    @EventHandler
    public void onRender3D(EventRender.World event) {
        if (mc.player == null || mc.world == null || mc.gameRenderer == null) {
            return;
        }

        MatrixStack stack = event.getMatrixStack();
        float tickDelta = event.getTicks();
        Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();
        BufferAllocator allocator = new BufferAllocator(BUFFER_SIZE);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);

        try {
            if (self.get() && !mc.options.getPerspective().isFirstPerson() && mc.player.isAlive() && !hasElytra(mc.player)) {
                renderWings(stack, mc.player, tickDelta, camera, immediate);
            }
            if (players.get()) {
                for (Entity entity : mc.world.getEntities()) {
                    if (!(entity instanceof PlayerEntity player) || player == mc.player || !player.isAlive() || hasElytra(player)) {
                        continue;
                    }
                    renderWings(stack, player, tickDelta, camera, immediate);
                }
            }
            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    @Override
    public void onDisable() {
        selfBodyYawInitialized = false;
        super.onDisable();
    }

    private boolean hasElytra(PlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }

    private void renderWings(MatrixStack stack, PlayerEntity player, float tickDelta, Vec3d camera,
                             VertexConsumerProvider.Immediate immediate) {
        double x = player.lastRenderX + (player.getX() - player.lastRenderX) * tickDelta - camera.x;
        double y = player.lastRenderY + (player.getY() - player.lastRenderY) * tickDelta - camera.y;
        double z = player.lastRenderZ + (player.getZ() - player.lastRenderZ) * tickDelta - camera.z;

        float bodyYaw = resolveBodyYaw(player, tickDelta);
        float move = MathHelper.clamp((float) player.getVelocity().horizontalLength() * 5f, 0f, 1f);
        WingPose pose = resolvePose(player, tickDelta);
        if (pose == null) {
            return;
        }

        float flap = (float) Math.sin((player.age + tickDelta) * pose.flapSpeed) * pose.flapAmplitude;
        float open = (DEFAULT_SPREAD + flap + move * pose.motionSpreadBoost) * pose.openMultiplier;
        float wingScale = size.get() * pose.scaleMultiplier;

        int baseColor = resolveBaseColor();
        int glowColor = resolveGlowColor(baseColor);
        int coreColor = resolveCoreColor(baseColor);
        int outlineColor = baseColor;

        stack.push();
        stack.translate(x, y, z);
        stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180f - bodyYaw));
        if (pose.preTranslateY != 0f || pose.preTranslateZ != 0f) {
            stack.translate(0f, pose.preTranslateY, pose.preTranslateZ);
        }
        if (pose.pitchRotation != 0f) {
            stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pose.pitchRotation));
        }
        if (pose.rollRotation != 0f) {
            stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(pose.rollRotation));
        }
        stack.translate(0f, pose.anchorY, pose.anchorZ);
        stack.scale(wingScale, wingScale, wingScale);
        renderWingSide(stack, -1f, open, baseColor, glowColor, coreColor, outlineColor, pose, immediate);
        renderWingSide(stack, 1f, open, baseColor, glowColor, coreColor, outlineColor, pose, immediate);
        stack.pop();
    }

    private void renderWingSide(MatrixStack stack, float side, float open, int baseColor, int glowColor,
                                int coreColor, int outlineColor, WingPose pose, VertexConsumerProvider.Immediate immediate) {
        stack.push();
        stack.translate(side * pose.sideOffset, pose.sideYOffset, pose.sideZOffset);
        stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(side * open));
        stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(side * pose.sideRoll));
        stack.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pose.sidePitch));
        Matrix4f matrix = stack.peek().getPositionMatrix();

        VertexConsumer glowBuf = immediate.getBuffer(WING_GLOW_LAYER);
        drawWingLayerToBuffer(glowBuf, matrix, side, 1.22f, setAlpha(glowColor, (int) (DEFAULT_ALPHA * 0.22f)), setAlpha(glowColor, 0));
        drawWingLayerToBuffer(glowBuf, matrix, side, 0.84f, setAlpha(coreColor, (int) (DEFAULT_ALPHA * 0.26f)), setAlpha(coreColor, 0));

        VertexConsumer fillBuf = immediate.getBuffer(WING_FILL_LAYER);
        drawWingLayerToBuffer(fillBuf, matrix, side, 1.0f, setAlpha(baseColor, DEFAULT_ALPHA), setAlpha(baseColor, 10));

        VertexConsumer linesBuf = immediate.getBuffer(WING_LINES_LAYER);
        drawWingOutlineToBuffer(linesBuf, matrix, side, 1.0f, setAlpha(outlineColor, (int) (DEFAULT_ALPHA * 0.62f)));
        drawWingRibsToBuffer(linesBuf, matrix, side, 0.96f, setAlpha(glowColor, (int) (DEFAULT_ALPHA * 0.20f)));
        stack.pop();
    }

    private void drawWingLayerToBuffer(VertexConsumer buffer, Matrix4f matrix, float side, float scale, int rootColor, int edgeColor) {
        for (int i = 0; i < SHAPE.length; i++) {
            WingPoint cur = SHAPE[i];
            WingPoint next = SHAPE[(i + 1) % SHAPE.length];
            vertex(buffer, matrix, 0f, 0f, 0f, rootColor);
            vertex(buffer, matrix, side * cur.x * scale, cur.y * scale, 0f, applyPointAlpha(edgeColor, cur.alphaMul));
            vertex(buffer, matrix, side * next.x * scale, next.y * scale, 0f, applyPointAlpha(edgeColor, next.alphaMul));
        }
    }

    private void drawWingOutlineToBuffer(VertexConsumer buffer, Matrix4f matrix, float side, float scale, int color) {
        for (int i = 0; i < SHAPE.length; i++) {
            WingPoint cur = SHAPE[i];
            WingPoint next = SHAPE[(i + 1) % SHAPE.length];
            vertex(buffer, matrix, side * cur.x * scale, cur.y * scale, 0f, color);
            vertex(buffer, matrix, side * next.x * scale, next.y * scale, 0f, color);
        }
        WingPoint last = SHAPE[SHAPE.length - 1];
        WingPoint first = SHAPE[0];
        vertex(buffer, matrix, side * last.x * scale, last.y * scale, 0f, color);
        vertex(buffer, matrix, side * first.x * scale, first.y * scale, 0f, color);
    }

    private void drawWingRibsToBuffer(VertexConsumer buffer, Matrix4f matrix, float side, float scale, int color) {
        int[] ribIndices = {2, 4, 7, 9, 11};
        for (int idx : ribIndices) {
            WingPoint point = SHAPE[idx];
            int rootColor = setAlpha(color, Math.max(8, (int) (alpha(color) * 0.75f)));
            int tipColor = applyPointAlpha(color, point.alphaMul);
            vertex(buffer, matrix, 0f, 0f, 0f, rootColor);
            vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0f, tipColor);
        }
    }

    private int resolveBaseColor() {
        long now = System.currentTimeMillis();
        if (cachedBaseColor == -1 || now - lastColorUpdate > 50L) {
            cachedBaseColor = ThemeColorUtils.getIconColor(10, 0);
            cachedGlowColor = ThemeColorUtils.interpolateColor(cachedBaseColor, makeColor(255, 255, 255, 255), 0.28);
            cachedCoreColor = ThemeColorUtils.interpolateColor(cachedBaseColor, makeColor(255, 255, 255, 255), 0.55);
            lastColorUpdate = now;
        }
        return cachedBaseColor;
    }

    private int resolveGlowColor(int base) {
        return cachedGlowColor;
    }

    private int resolveCoreColor(int base) {
        return cachedCoreColor;
    }

    private float resolveBodyYaw(PlayerEntity player, float tickDelta) {
        float target = MathHelper.lerpAngleDegrees(tickDelta, player.lastBodyYaw, player.bodyYaw);
        if (player != mc.player) {
            return target;
        }
        if (!selfBodyYawInitialized || player.age < 2) {
            selfBodyYaw = target;
            selfBodyYawInitialized = true;
            return selfBodyYaw;
        }
        selfBodyYaw = approachDegrees(selfBodyYaw, target, 14f);
        return selfBodyYaw;
    }

    private WingPose resolvePose(PlayerEntity player, float tickDelta) {
        float pitch = player.getPitch(tickDelta);
        if (player.isGliding()) {
            float speed = (float) player.getVelocity().length();
            float flightProgress = MathHelper.clamp(speed * speed * 4f, 0f, 1f);
            float pitchRotation = flightProgress * (-90f - pitch);
            return new WingPose(0.34f, 0.46f, 0f, 0f, pitchRotation, 0f, 0.76f, 0.92f, 0.10f, 0.58f, 0.05f, 0.06f, -5f, -2f, 0.13f);
        }
        if (player.isTouchingWater()) {
            return null;
        }
        if (player.isSneaking()) {
            return new WingPose(0f, 0f, 0.96f, 0.10f, 18f, 0f, 1f, 1f, 0.18f, 4.5f, 0.06f, 0.02f, -11f, -4f, 0.12f);
        }
        return new WingPose(0f, 0f, 1.38f, 0.10f, 0f, 0f, 1f, 1f, 0.18f, 4.5f, 0.06f, 0.02f, -11f, -4f, 0.12f);
    }

    private int applyPointAlpha(int color, float multiplier) {
        return setAlpha(color, Math.max(0, Math.min(255, (int) (alpha(color) * multiplier))));
    }

    private static float approachDegrees(float current, float target, float maxDelta) {
        float delta = MathHelper.wrapDegrees(target - current);
        delta = MathHelper.clamp(delta, -maxDelta, maxDelta);
        return current + delta;
    }

    private static int setAlpha(int color, int a) {
        return (MathHelper.clamp(a, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private static int alpha(int color) { return (color >> 24) & 0xFF; }
    private static int red(int color) { return (color >> 16) & 0xFF; }
    private static int green(int color) { return (color >> 8) & 0xFF; }
    private static int blue(int color) { return color & 0xFF; }
    private static int makeColor(int r, int g, int b, int a) { return (a << 24) | (r << 16) | (g << 8) | b; }

    private void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, int color) {
        buffer.vertex(matrix, x, y, z).color(red(color) / 255f, green(color) / 255f, blue(color) / 255f, alpha(color) / 255f);
    }

    private record WingPoint(float x, float y, float alphaMul) {}

    private record WingPose(float preTranslateY, float preTranslateZ, float anchorY, float anchorZ,
                            float pitchRotation, float rollRotation, float openMultiplier, float scaleMultiplier,
                            float motionSpreadBoost, float flapAmplitude, float sideOffset, float sideYOffset,
                            float sideZOffset, float sideRoll, float sidePitch, float flapSpeed) {
        private WingPose(float preTranslateY, float preTranslateZ, float anchorY, float anchorZ,
                         float pitchRotation, float rollRotation, float openMultiplier, float scaleMultiplier,
                         float motionSpreadBoost, float flapAmplitude, float sideOffset, float sideZOffset,
                         float sideRoll, float sidePitch, float flapSpeed) {
            this(preTranslateY, preTranslateZ, anchorY, anchorZ, pitchRotation, rollRotation, openMultiplier, scaleMultiplier,
                    motionSpreadBoost, flapAmplitude, sideOffset, 0f, sideZOffset, sideRoll, sidePitch, flapSpeed);
        }
    }
}
