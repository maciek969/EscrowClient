package elexis.modules.impl.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import elexis.Elexis;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.math.Mathf;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.client.utils.render.main.oldrender.ClientRenderer;
import elexis.client.utils.render.main.newrender.text.FontRegistry;
import elexis.client.utils.render.world.WorldGeometryEmitter;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.MultiBooleanSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@FunctionAdd(name = "Predictions", alias = "Predictions", category = Category.Render, description = "Shows projectile landing prediction")
public class Predictions extends Function {
    private static final RenderPipeline LINES_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
                    .withLocation(Identifier.of("elexis", "predictions_lines"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.DEBUG_LINES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );
    public static final RenderLayer LINES_LAYER = RenderLayer.of(
            "elexis_predictions_lines",
            RenderSetup.builder(LINES_PIPELINE).expectedBufferSize(1024).build());

    private static final DecimalFormat TIME_FORMAT = new DecimalFormat("0.0");
    private static final ItemStack ENDER_PEARL_STACK = new ItemStack(Items.ENDER_PEARL);
    private static final ItemStack ARROW_STACK = new ItemStack(Items.ARROW);
    private static final ItemStack TRIDENT_STACK = new ItemStack(Items.TRIDENT);
    private static final ItemStack SNOWBALL_STACK = new ItemStack(Items.SNOWBALL);

    public final MultiBooleanSetting projectiles = new MultiBooleanSetting(
            "Projectiles",
            new BooleanSetting("Ender Pearl", true),
            new BooleanSetting("Arrow", false),
            new BooleanSetting("Trident", false),
            new BooleanSetting("Items", true),
            new BooleanSetting("Snowball", false)
    );

    private final List<Entity> projectileCache = new ArrayList<>();
    private final List<TrajectoryData> trajectoryCache = new ArrayList<>();

    public Predictions() {
        addSettings(projectiles);
    }

    @EventHandler
    public void onRender3D(EventRender.World event) {
        if (!isState() || mc.player == null || mc.world == null) {
            projectileCache.clear();
            trajectoryCache.clear();
            return;
        }
        collectProjectiles();
        renderTrajectories3D(event);
    }

    @EventHandler
    public void onRender2D(EventRender.Screen.Hud event) {
        if (!isState() || mc.player == null || mc.world == null) {
            return;
        }
        renderTrajectories2D(event);
    }

    private void collectProjectiles() {
        projectileCache.clear();
        trajectoryCache.clear();
        int itemCount = 0;
        List<Entity> entities = mc.world.getOtherEntities(
                mc.player,
                mc.player.getBoundingBox().expand(128.0),
                e -> e instanceof EnderPearlEntity || e instanceof ArrowEntity || e instanceof ItemEntity || e instanceof TridentEntity || e instanceof SnowballEntity
        );

        for (Entity entity : entities) {
            if (entity instanceof ItemEntity itemEntity) {
                if (!projectiles.get("Items")) {
                    continue;
                }
                ItemStack stack = itemEntity.getStack();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                boolean allowed = stack.isOf(Items.ENDER_PEARL) || stack.isOf(Items.ARROW) || stack.isOf(Items.TRIDENT) || stack.isOf(Items.SNOWBALL);
                if (!allowed || entity.isTouchingWater() || entity.isInLava() || entity.isOnGround()) {
                    continue;
                }
                if (++itemCount > 33) {
                    continue;
                }
            } else if (entity instanceof EnderPearlEntity && !projectiles.get("Ender Pearl")) {
                continue;
            } else if (entity instanceof ArrowEntity && !projectiles.get("Arrow")) {
                continue;
            } else if (entity instanceof TridentEntity && !projectiles.get("Trident")) {
                continue;
            } else if (entity instanceof SnowballEntity && !projectiles.get("Snowball")) {
                continue;
            }

            if (entity.lastX != entity.getX() || entity.lastY != entity.getY() || entity.lastZ != entity.getZ()) {
                projectileCache.add(entity);
            }
        }
    }

    private void renderTrajectories3D(EventRender.World event) {
        if (projectileCache.isEmpty()) {
            return;
        }

        MatrixStack matrices = event.getMatrixStack();
        BufferAllocator allocator = new BufferAllocator(262144);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        try {
            VertexConsumer buffer = immediate.getBuffer(LINES_LAYER);
            WorldGeometryEmitter emitter = new WorldGeometryEmitter(mc.gameRenderer.getCamera(), matrices.peek(), buffer);
            for (Entity entity : projectileCache) {
                renderLine(entity, emitter);
            }
            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    private void renderLine(Entity entity, WorldGeometryEmitter emitter) {
        Vec3d entityPosition = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        Vec3d entityMotion = entity.getVelocity();
        double posX = entityPosition.x;
        double posY = entityPosition.y;
        double posZ = entityPosition.z;
        double motionX = entityMotion.x;
        double motionY = entityMotion.y;
        double motionZ = entityMotion.z;
        double finalX = posX;
        double finalY = posY;
        double finalZ = posZ;
        int steps = 0;

        int base = ThemeColorUtils.getIconColor(1, 1);
        int shade = ThemeColorUtils.rgba(0, 0, 0, 255);

        boolean isEnderPearl = entity instanceof EnderPearlEntity;
        boolean isSnowball = entity instanceof SnowballEntity;
        boolean isItem = entity instanceof ItemEntity;
        boolean isTrident = entity instanceof TridentEntity;

        double gravityValue = isEnderPearl || isSnowball ? 0.03 : (isItem ? 0.04 : 0.05);
        double dragAir = 0.99;
        double dragWater = (!isEnderPearl && !isItem && !isSnowball) ? 0.6 : 0.8;
        boolean hasGravity = !entity.hasNoGravity();

        for (int i = 0; i <= 300; i++) {
            steps++;
            double lastX = posX;
            double lastY = posY;
            double lastZ = posZ;
            posX += motionX;
            posY += motionY;
            posZ += motionZ;

            boolean inWater = entity.isTouchingWater() || mc.world.getBlockState(BlockPos.ofFloored(lastX, lastY, lastZ)).isOf(Blocks.WATER);
            double scale = inWater && !isTrident ? dragWater : dragAir;
            motionX *= scale;
            motionY = motionY * scale - (hasGravity ? gravityValue : 0.0);
            motionZ *= scale;

            Vec3d lastPosition = new Vec3d(lastX, lastY, lastZ);
            Vec3d currentPosition = new Vec3d(posX, posY, posZ);
            HitResult hit = mc.world.raycast(new RaycastContext(lastPosition, currentPosition, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
            if (hit.getType() == HitResult.Type.BLOCK || posY <= 0.0) {
                finalX = lastX;
                finalY = lastY;
                finalZ = lastZ;
                break;
            }

            finalX = posX;
            finalY = posY;
            finalZ = posZ;

            float t = MathHelper.clamp(i / 60.0f, 0.0f, 1.0f);
            int rgb = lerpRgb(base, shade, t);
            int a = i < 4 ? (int) ((i / 4.0f) * 200.0f) : 200;
            int color = (a & 0xFF) << 24 | (rgb & 0xFFFFFF);
            emitter.emitLine(lastPosition, currentPosition, color);
        }
        trajectoryCache.add(new TrajectoryData(entity, new Vec3d(finalX, finalY, finalZ), steps));
    }

    private void renderTrajectories2D(EventRender.Screen.Hud event) {
        ClientRenderer oldRenderer = Elexis.getInstance().testRender;
        DrawContext drawContext = oldRenderer == null ? null : oldRenderer.getDrawContext();
        if (drawContext == null || trajectoryCache.isEmpty()) {
            return;
        }

        for (TrajectoryData data : trajectoryCache) {
            Vec3d screen = Mathf.worldSpaceToScreenSpace(data.finalPosition);
            if (screen == null || screen.z < 0.0 || screen.z > 1.0) {
                continue;
            }

            float x = (float) screen.x * 2.0f;
            float y = (float) screen.y * 2.0f + 6.0f;
            String timeText = TIME_FORMAT.format(data.steps * 0.05f) + " sec";
            ItemStack icon = getProjectileItemStack(data.entity);

            float fontSize = 22.0f;
            float textW = event.getRenderer().measureText(FontRegistry.SF_MEDIUM, timeText, fontSize).width;
            float paddingLeft = 6.0f;
            float paddingRight = 8.0f;
            float iconSize = 16.0f;
            float gap = 4.0f;
            float boxH = 20.0f;
            float boxW = paddingLeft + iconSize + gap + textW + paddingRight;
            float boxX = x - boxW / 2.0f;
            float boxY = y - boxH / 2.0f;

            event.getRenderer().rect(boxX, boxY, boxW, boxH, 6.0f, 0x7A111111);
            event.getRenderer().rectOutline(boxX, boxY, boxW, boxH, 6.0f, ThemeColorUtils.applyOpacity(ThemeColorUtils.getIconColor(10, data.entity.getId()), 90), 1.0f);

            if (icon != null && !icon.isEmpty()) {
                try {
                    drawContext.drawItem(icon, Math.round(boxX + paddingLeft), Math.round(boxY + (boxH - iconSize) * 0.5f), data.entity.getId());
                    drawContext.drawStackOverlay(mc.textRenderer, icon, Math.round(boxX + paddingLeft), Math.round(boxY + (boxH - iconSize) * 0.5f));
                } catch (Throwable ignored) {
                }
            }

            float textY = boxY + boxH * 0.5f + 4.5f;
            event.getRenderer().text(FontRegistry.SF_MEDIUM, boxX + paddingLeft + iconSize + gap, textY, fontSize, timeText, 0xFFFFFFFF);
        }
    }

    private ItemStack getProjectileItemStack(Entity entity) {
        if (entity instanceof EnderPearlEntity) return ENDER_PEARL_STACK;
        if (entity instanceof ArrowEntity) return ARROW_STACK;
        if (entity instanceof TridentEntity) return TRIDENT_STACK;
        if (entity instanceof SnowballEntity) return SNOWBALL_STACK;
        if (entity instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getStack();
            return stack == null ? ItemStack.EMPTY : stack;
        }
        return ItemStack.EMPTY;
    }

    private static int lerpRgb(int c1, int c2, float t) {
        t = MathHelper.clamp(t, 0.0F, 1.0F);
        int r1 = c1 >> 16 & 0xFF;
        int g1 = c1 >> 8 & 0xFF;
        int b1 = c1 & 0xFF;
        int r2 = c2 >> 16 & 0xFF;
        int g2 = c2 >> 8 & 0xFF;
        int b2 = c2 & 0xFF;
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    private record TrajectoryData(Entity entity, Vec3d finalPosition, int steps) {
    }
}

