package elexis.modules.impl.render;

import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.client.utils.render.world.WorldGeometryEmitter;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@FunctionAdd(
        name = "Box 3D",
        alias = "Box3D",
        category = Category.Render,
        description = "Draws 3D boxes around entities"
)
@Environment(EnvType.CLIENT)
public class Box3D extends Function {
    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting self = new BooleanSetting("Self", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", true);
    private final BooleanSetting animals = new BooleanSetting("Animals", true);

    public Box3D() {
        addSettings(players, self, mobs, animals);
    }

    @EventHandler
    public void onRender(EventRender.World event) {
        if (nullCheck()) {
            return;
        }

        MatrixStack matrices = event.getMatrixStack();
        BufferAllocator allocator = new BufferAllocator(262144);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);
        try {
            VertexConsumer buffer = immediate.getBuffer(Predictions.LINES_LAYER);
            WorldGeometryEmitter emitter = new WorldGeometryEmitter(mc.gameRenderer.getCamera(), matrices.peek(), buffer);

            for (Entity entity : mc.world.getEntities()) {
                if (shouldRender(entity)) {
                    renderEntityBox(entity, emitter, event.getTicks());
                }
            }

            immediate.draw();
        } finally {
            allocator.close();
        }
    }

    private boolean shouldRender(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof ArmorStandEntity || !entity.isAlive()) {
            return false;
        }
        if (entity == mc.player) {
            return self.get();
        }
        if (entity instanceof PlayerEntity) {
            return players.get();
        }
        if (entity instanceof Monster) {
            return mobs.get();
        }
        if (entity instanceof AnimalEntity) {
            return animals.get();
        }
        return false;
    }

    private void renderEntityBox(Entity entity, WorldGeometryEmitter emitter, float tickDelta) {
        Vec3d pos = new Vec3d(
                entity.lastX + (entity.getX() - entity.lastX) * tickDelta,
                entity.lastY + (entity.getY() - entity.lastY) * tickDelta,
                entity.lastZ + (entity.getZ() - entity.lastZ) * tickDelta
        );

        Box raw = entity.getBoundingBox();
        Box box = raw.offset(pos.x - entity.getX(), pos.y - entity.getY(), pos.z - entity.getZ()).expand(0.02D);
        int color = ThemeColorUtils.applyOpacity(ThemeColorUtils.getIconColor(8, entity.getId()), 230);
        drawBox(emitter, box, color);
    }

    private void drawBox(WorldGeometryEmitter emitter, Box box, int color) {
        Vec3d a = new Vec3d(box.minX, box.minY, box.minZ);
        Vec3d b = new Vec3d(box.maxX, box.minY, box.minZ);
        Vec3d c = new Vec3d(box.maxX, box.minY, box.maxZ);
        Vec3d d = new Vec3d(box.minX, box.minY, box.maxZ);
        Vec3d e = new Vec3d(box.minX, box.maxY, box.minZ);
        Vec3d f = new Vec3d(box.maxX, box.maxY, box.minZ);
        Vec3d g = new Vec3d(box.maxX, box.maxY, box.maxZ);
        Vec3d h = new Vec3d(box.minX, box.maxY, box.maxZ);

        emitter.emitLine(a, b, color); emitter.emitLine(b, c, color); emitter.emitLine(c, d, color); emitter.emitLine(d, a, color);
        emitter.emitLine(e, f, color); emitter.emitLine(f, g, color); emitter.emitLine(g, h, color); emitter.emitLine(h, e, color);
        emitter.emitLine(a, e, color); emitter.emitLine(b, f, color); emitter.emitLine(c, g, color); emitter.emitLine(d, h, color);
    }
}