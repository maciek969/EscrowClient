package elexis.modules.impl.combat.javelin;

import elexis.Elexis;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.client.IMinecraft;
import elexis.modules.api.Function;
import elexis.modules.impl.render.visual.VisualGeometry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** Shared native inventory, targeting and placement operations used by the Javelin modules. */
public final class JavelinCombat implements IMinecraft {
    private JavelinCombat() {}

    public record Placement(BlockPos support, Direction face, Vec3d point) {
        public BlockPos destination() { return support.offset(face); }
    }

    public record FadingBox(BlockPos position, int color, long createdAt) {}

    public static boolean ready() {
        return mc.player != null && mc.world != null && mc.interactionManager != null
                && mc.player.isAlive() && !mc.player.isSpectator();
    }

    public static boolean enabled(String name) {
        Function function = Elexis.getFunctionManager().getFunctions().stream()
                .filter(candidate -> candidate.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        return function != null && function.isState();
    }

    public static <T extends Function> T module(String name, Class<T> type) {
        return Elexis.getFunctionManager().getFunctions().stream().filter(type::isInstance)
                .map(type::cast).filter(module -> module.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public static boolean validTarget(PlayerEntity player, double range) {
        if (player == null) return false;
        return ready() && player != mc.player && player.isAlive() && !player.isRemoved()
                && !player.isSpectator() && player.getEntityWorld() == mc.world
                && player.squaredDistanceTo(mc.player) <= range * range
                && !Elexis.getInstance().getFriendStorage().isFriend(player.getGameProfile().name());
    }

    public static PlayerEntity target(PlayerEntity previous, double range, boolean keepCurrent) {
        if (keepCurrent && previous != null && validTarget(previous, range)) return previous;
        return mc.world.getPlayers().stream().filter(player -> validTarget(player, range))
                .min(Comparator.comparingDouble(JavelinCombat::angleDistance)).orElse(null);
    }

    private static double angleDistance(PlayerEntity entity) {
        float[] rotation = angles(entity.getEyePos());
        return Math.hypot(MathHelper.wrapDegrees(rotation[0] - mc.player.getYaw()), rotation[1] - mc.player.getPitch());
    }

    public static Vec3d predict(PlayerEntity player, int ticks) {
        Vec3d position = player.getEntityPos();
        Vec3d motion = player.getVelocity();
        Box box = player.getBoundingBox();
        for (int tick = 0; tick < ticks; tick++) {
            Vec3d next = position.add(motion);
            Box moved = box.offset(next.subtract(player.getEntityPos()));
            if (!mc.world.isSpaceEmpty(player, moved)) break;
            position = next;
            if (!player.isGliding()) motion = motion.multiply(0.91, 0.98, 0.91);
        }
        return position;
    }

    public static int find(Item item, int start, int end) {
        return find(stack -> stack.isOf(item), start, end);
    }

    public static int find(Predicate<ItemStack> predicate, int start, int end) {
        if (mc.player == null) return -1;
        for (int index = Math.max(0, start); index < Math.min(36, end); index++) {
            if (predicate.test(mc.player.getInventory().getStack(index))) return index;
        }
        return -1;
    }

    public static Slot inventorySlot(int index) {
        if (mc.player == null || mc.player.currentScreenHandler == null) return null;
        for (Slot slot : mc.player.currentScreenHandler.slots) {
            if (slot.inventory == mc.player.getInventory() && slot.getIndex() == index) return slot;
        }
        return null;
    }

    public static boolean swapInventory(int inventoryIndex, int hotbarButton) {
        if (!ready() || !mc.player.currentScreenHandler.getCursorStack().isEmpty()) return false;
        Slot slot = inventorySlot(inventoryIndex);
        if (slot == null || inventoryIndex < 0 || inventoryIndex >= 36) return false;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot.id, hotbarButton, SlotActionType.SWAP, mc.player);
        return true;
    }

    public static void select(int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot > 8) return;
        mc.player.getInventory().setSelectedSlot(hotbarSlot);
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
    }

    public static float[] angles(Vec3d point) {
        Vec3d delta = point.subtract(mc.player.getEyePos());
        return new float[]{(float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90f,
                (float)-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)))};
    }

    public static void sendRotation(Vec3d point) {
        float[] angles = angles(point);
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                angles[0], angles[1], mc.player.isOnGround(), mc.player.horizontalCollision));
    }

    public static void restoreRotation() {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision));
    }

    public static Vec3d facePoint(BlockPos block, Direction face) {
        Vec3d eye = mc.player.getEyePos();
        double x = MathHelper.clamp(eye.x, block.getX() + .001, block.getX() + .999);
        double y = MathHelper.clamp(eye.y, block.getY() + .001, block.getY() + .999);
        double z = MathHelper.clamp(eye.z, block.getZ() + .001, block.getZ() + .999);
        return switch (face) {
            case WEST -> new Vec3d(block.getX(), y, z);
            case EAST -> new Vec3d(block.getX() + 1, y, z);
            case DOWN -> new Vec3d(x, block.getY(), z);
            case UP -> new Vec3d(x, block.getY() + 1, z);
            case NORTH -> new Vec3d(x, y, block.getZ());
            case SOUTH -> new Vec3d(x, y, block.getZ() + 1);
        };
    }

    public static Placement visibleFace(BlockPos support, Direction requiredFace) {
        if (!ready() || mc.world.getBlockState(support).isAir()) return null;
        double reach = mc.player.getBlockInteractionRange() + .003;
        Placement best = null;
        double nearest = Double.MAX_VALUE;
        for (Direction face : Direction.values()) {
            if (requiredFace != null && face != requiredFace) continue;
            Vec3d point = facePoint(support, face);
            double distance = point.squaredDistanceTo(mc.player.getEyePos());
            if (distance > reach * reach || distance >= nearest) continue;
            Vec3d inside = point.subtract(Vec3d.of(face.getVector()).multiply(.0001));
            BlockHitResult ray = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), inside,
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
            if (ray.getType() != HitResult.Type.BLOCK || !ray.getBlockPos().equals(support) || ray.getSide() != face) continue;
            nearest = distance;
            best = new Placement(support.toImmutable(), face, point);
        }
        return best;
    }

    public static Placement supportFor(BlockPos destination) {
        if (!mc.world.getBlockState(destination).isReplaceable()) return null;
        Placement best = null;
        double distance = Double.MAX_VALUE;
        for (Direction face : Direction.values()) {
            BlockPos support = destination.offset(face.getOpposite());
            BlockState state = mc.world.getBlockState(support);
            if (!state.isSolidBlock(mc.world, support)) continue;
            Placement candidate = visibleFace(support, face);
            if (candidate == null) continue;
            double nextDistance = candidate.point().squaredDistanceTo(mc.player.getEyePos());
            if (nextDistance < distance) { best = candidate; distance = nextDistance; }
        }
        return best;
    }

    public static boolean blocked(BlockPos destination, boolean crystal) {
        Box box = crystal ? new Box(destination).expand(.5, 0, .5).stretch(0, 1, 0) : new Box(destination);
        return !mc.world.getOtherEntities(null, box, entity -> !entity.isSpectator() && entity.isAlive()
                && !(entity instanceof net.minecraft.entity.ItemEntity)).isEmpty();
    }

    public static boolean place(Placement placement, int slot) {
        if (!ready() || placement == null || slot < 0 || slot > 8) return false;
        int previous = mc.player.getInventory().getSelectedSlot();
        try {
            sendRotation(placement.point());
            if (previous != slot) select(slot);
            var result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(placement.point(), placement.face(), placement.support(), false));
            if (result.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);
            return result.isAccepted();
        } finally {
            if (mc.player.getInventory().getSelectedSlot() != previous) select(previous);
            restoreRotation();
        }
    }

    public static List<BlockPos> nearby(int radius) {
        BlockPos center = mc.player.getBlockPos();
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos position : BlockPos.iterate(center.add(-radius, -radius, -radius), center.add(radius, radius, radius))) {
            result.add(position.toImmutable());
        }
        return result;
    }

    public static void renderBoxes(EventRender.World event, List<FadingBox> boxes) {
        long now = System.nanoTime();
        boxes.removeIf(box -> now - box.createdAt() > 800_000_000L);
        if (boxes.isEmpty()) return;
        try (VisualGeometry.Frame frame = new VisualGeometry.Frame(event)) {
            for (FadingBox box : boxes) {
                float alpha = 1f - (now - box.createdAt()) / 800_000_000f;
                frame.box(new Box(box.position()).expand(.002), VisualGeometry.alpha(box.color(), alpha));
            }
        }
    }
}