package elexis.modules.impl.combat;

import elexis.Elexis;
import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.impl.combat.javelin.pearl.*;
import java.util.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

@FunctionAdd(name = "TargetPearl", alias = "Target Pearl", category = Category.Combat,
        description = "Следует за перлом цели с расчётом приземления и собственной траектории из Javelin")
public final class TargetPearl extends Function {
    private static PearlRotation movementRotation;
    private static ClientWorld rotationLevel;
    private static Entity rotationPlayer;
    private final Map<Integer, Landing> landings = new HashMap<>();
    private final Map<Integer, Integer> handled = new HashMap<>();
    private ClientWorld trackedLevel;
    private LivingEntity lastTarget;
    private Vec3d sentPosition;
    private Vec3d sentMovement = Vec3d.ZERO;
    private boolean sentOnGround;
    private boolean movementReady;
    private boolean receivedMovement;
    private int lastAttemptTick = Integer.MIN_VALUE;
    private int remainingSimulationSteps;

    public static PearlRotation getMovementRotation() {
        return mc.world == rotationLevel && mc.player == rotationPlayer ? movementRotation : null;
    }

    public static void finishMovementRotation() {
        movementRotation = null;
        rotationLevel = null;
        rotationPlayer = null;
    }

    public static void observeSentPacket(ClientConnection connection, Packet<?> packet) {
        var manager = Elexis.getFunctionManager();
        if (manager == null) return;
        TargetPearl module = (TargetPearl) manager.getFunctionByName("TargetPearl");
        if (module == null || !module.isState()) return;
        ClientWorld level = mc.world;
        Runnable capture = () -> {
            if (!module.isState() || mc.world != level || mc.player == null
                    || mc.getNetworkHandler() == null || mc.getNetworkHandler().getConnection() != connection) return;
            if (module.trackedLevel != level) {
                module.clearState();
                module.trackedLevel = level;
            }
            if (packet instanceof PlayerMoveC2SPacket movement) module.rememberMovement(movement);
            else if (packet instanceof ClientTickEndC2SPacket) {
                if (!module.receivedMovement) module.sentMovement = Vec3d.ZERO;
                module.receivedMovement = false;
            }
        };
        if (mc.isOnThread()) capture.run();
        else mc.execute(capture);
    }

    @EventHandler
    public void onPacket(EventReceivePacket event) {
        if (event.getPacket() instanceof PlayerRespawnS2CPacket) mc.execute(this::clearState);
    }

    @EventHandler
    public void onTick(EventUpdate.Fast event) {
        if (trackedLevel != mc.world) {
            clearState();
            trackedLevel = mc.world;
        }
        if (nullCheck()) return;
        int tick = mc.player.age;
        handled.values().removeIf(expires -> expires < tick);
        landings.values().removeIf(landing -> landing.expires() < tick);
        updateAndThrow();
    }

    private void rememberMovement(PlayerMoveC2SPacket packet) {
        Vec3d previous = sentPosition == null ? mc.player.getEntityPos() : sentPosition;
        Vec3d next = new Vec3d(packet.getX(previous.x), packet.getY(previous.y), packet.getZ(previous.z));
        Vec3d delta = next.subtract(previous);
        movementReady = (sentPosition != null || mc.player.getMovement().lengthSquared() < 1.0e-6)
                && finite(delta) && delta.lengthSquared() < 100;
        sentMovement = movementReady ? delta : Vec3d.ZERO;
        receivedMovement = true;
        sentPosition = next;
        sentOnGround = packet.isOnGround();
    }

    private void updateAndThrow() {
        remainingSimulationSteps = 6000;
        var manager = Elexis.getFunctionManager();
        LivingEntity currentTarget = manager.getHitAura().isState() ? HitAura.target : manager.getAimBot().getTarget();
        if (currentTarget != null) lastTarget = currentTarget;
        LivingEntity target = lastTarget;
        if (target == null || !target.isAlive() || target.isRemoved() || target.getEntityWorld() != mc.world) {
            landings.clear();
            lastTarget = null;
            return;
        }
        UUID targetId = target.getUuid();
        landings.values().removeIf(landing -> !landing.owner().equals(targetId));
        List<Collider> colliders = new ArrayList<>();
        List<EnderPearlEntity> pearls = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (entity.isRemoved()) continue;
            if ((entity.canBeHitByProjectile() || entity.canHit()) && !entity.isSpectator()) {
                Vec3d movement = entity.getMovement();
                if (!finite(movement) || movement.lengthSquared() > 16) movement = Vec3d.ZERO;
                Box box = entity.getBoundingBox();
                colliders.add(new Collider(entity, box, movement, box.stretch(movement.multiply(5)).expand(0.3)));
            }
            if (entity instanceof EnderPearlEntity pearl && !handled.containsKey(pearl.getId())) {
                Entity owner = pearl.getOwner();
                if (owner != null && owner.getUuid().equals(targetId)) pearls.add(pearl);
            }
        }
        pearls.sort(Comparator.comparingInt(pearl -> pearl.age));
        int now = mc.player.age;
        for (int i = 0; i < Math.min(pearls.size(), 8); i++) {
            EnderPearlEntity pearl = pearls.get(i);
            Flight flight = simulate(pearl.getEntityPos(), pearl.getVelocity(), pearl.getOwner(), pearl,
                    pearl.age, pearl.hasNoGravity(), colliders, null);
            if (flight.impact() == null) landings.remove(pearl.getId());
            else landings.put(pearl.getId(), new Landing(pearl.getId(), targetId, flight.impact(),
                    now - pearl.age, now + flight.impact().ticks() + 20));
        }
        if (landings.size() > 8) {
            List<Landing> oldest = landings.values().stream().sorted(Comparator.comparingInt(Landing::spawnTick)).toList();
            for (int i = 0; i < oldest.size() - 8; i++) landings.remove(oldest.get(i).id());
        }
        if (!movementReady || sentPosition == null || mc.interactionManager == null || mc.player.isSpectator()
                || mc.player.hasVehicle() || mc.player.isUsingItem() || mc.currentScreen != null || lastAttemptTick == now) return;
        if (manager.getClickPearl().isPearlUseBusy()) return;
        boolean offhand = mc.player.getOffHandStack().isOf(Items.ENDER_PEARL);
        int selected = mc.player.getInventory().getSelectedSlot();
        int slot = mc.player.getMainHandStack().isOf(Items.ENDER_PEARL) ? selected : findPearl(0, 36);
        if (!offhand && slot < 0) return;
        ItemStack pearl = offhand ? mc.player.getOffHandStack() : mc.player.getInventory().getStack(slot);
        if (mc.player.getItemCooldownManager().isCoolingDown(pearl)) return;
        Vec3d start = sentPosition.add(0, mc.player.getEyeHeight(mc.player.getPose()) - 0.1f, 0);
        Vec3d inherited = new Vec3d(sentMovement.x, sentOnGround ? 0 : sentMovement.y, sentMovement.z);
        for (Landing landing : landings.values().stream().sorted(Comparator.comparingInt(Landing::spawnTick).reversed()).toList()) {
            if (landing.impact().position().squaredDistanceTo(sentPosition) < 9) continue;
            Branch shot = solve(start, inherited, landing.impact(), colliders);
            if (shot == null) continue;
            if (!offhand && slot >= 9 && (mc.player.currentScreenHandler != mc.player.playerScreenHandler
                    || !mc.player.playerScreenHandler.getCursorStack().isEmpty())) return;
            for (int craftingSlot = 1; !offhand && slot >= 9 && craftingSlot <= 4; craftingSlot++) {
                if (!mc.player.playerScreenHandler.getSlot(craftingSlot).getStack().isEmpty()) return;
            }
            lastAttemptTick = now;
            ItemStack previousHand = mc.player.getMainHandStack().copy();
            boolean inventorySwap = !offhand && slot >= 9;
            if (inventorySwap) mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, selected, SlotActionType.SWAP, mc.player);
            else if (!offhand && slot != selected) select(slot);
            try {
                if (throwPearl(shot.rotation(), offhand ? Hand.OFF_HAND : Hand.MAIN_HAND)) {
                    handled.put(landing.id(), now + 180);
                    landings.remove(landing.id());
                }
            } finally {
                if (inventorySwap && mc.player.currentScreenHandler == mc.player.playerScreenHandler
                        && ItemStack.areEqual(mc.player.getInventory().getStack(slot), previousHand)
                        && (mc.player.getMainHandStack().isEmpty() || mc.player.getMainHandStack().isOf(Items.ENDER_PEARL))) {
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, selected, SlotActionType.SWAP, mc.player);
                } else if (!offhand && !inventorySwap && selected != slot) select(selected);
            }
            return;
        }
    }

    private int findPearl(int start, int end) {
        for (int slot = start; slot < end; slot++) {
            if (mc.player.getInventory().getStack(slot).isOf(Items.ENDER_PEARL)) return slot;
        }
        return -1;
    }

    private void select(int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
        sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

   @Nullable
   public Branch solve(Vec3d start, Vec3d inherited, Impact target, List<Collider> colliders) {
      List<Vec3d> aims = new ArrayList<>();
      aims.add(target.position());
      if (target.face() != null) {
         Vec3d first = target.face().getOffsetY() == 0 ? new Vec3d(0.0, 1.0, 0.0) : new Vec3d(1.0, 0.0, 0.0);
         Vec3d second = target.face().getOffsetX() == 0 && target.face().getOffsetY() == 0 ? new Vec3d(1.0, 0.0, 0.0) : new Vec3d(0.0, 0.0, 1.0);

         for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
               if (x != 0 || y != 0) {
                  aims.add(target.position().add(first.multiply(x * 0.45)).add(second.multiply(y * 0.45)));
               }
            }
         }
      }

      for (boolean refine : new boolean[]{false, true}) {
         for (Vec3d aim : aims) {
            if (this.remainingSimulationSteps <= 0) {
               return null;
            }

            Branch candidate = this.solveAt(start, inherited, target, aim, colliders, refine);
            if (candidate != null) {
               return candidate;
            }
         }
      }

      return null;
   }

   @Nullable
   public Branch solveAt(Vec3d start, Vec3d inherited, Impact target, Vec3d aimPoint, List<Collider> colliders, boolean refine) {
      Vec3d aim = target.face() == null
         ? aimPoint
         : aimPoint.add(-target.face().getOffsetX() * 0.04, -target.face().getOffsetY() * 0.04, -target.face().getOffsetZ() * 0.04);
      List<PearlRotation> seeds = new ArrayList<>(SolveUtil.solve(start, inherited, aim));
      if (this.inWater(start) || this.inWater(target.position())) {
         seeds.addAll(SolveUtil.solve(start, inherited, aim, 0.8F));
      }

      for (PearlRotation rotation : seeds) {
         if (this.remainingSimulationSteps <= 0) {
            break;
         }

         Branch branch = this.evaluate(start, inherited, rotation, target, colliders);
         if (refine && !branch.valid() && (branch.water() || branch.error() < 16.0)) {
            for (float step : new float[]{4.0F, 1.0F, 0.25F, 0.0625F}) {
               if (branch.valid() || this.remainingSimulationSteps <= 0) {
                  break;
               }

               PearlRotation center = branch.rotation();

               for (int axis = 0; axis < 2; axis++) {
                  for (int sign : new int[]{-1, 1}) {
                     float yaw = center.getYaw() + (axis == 0 ? sign * step : 0.0F);
                     float pitch = MathHelper.clamp(center.getPitch() + (axis == 1 ? sign * step : 0.0F), -90.0F, 90.0F);
                     Branch candidate = this.evaluate(start, inherited, new PearlRotation(yaw, pitch), target, colliders);
                     if (better(candidate, branch)) {
                        branch = candidate;
                     }
                  }
               }
            }
         }

         if (branch.valid() && this.stable(start, inherited, branch.rotation(), target, colliders)) {
            return branch;
         }
      }

      return null;
   }

   public Branch evaluate(Vec3d start, Vec3d inherited, PearlRotation rotation, Impact target, List<Collider> colliders) {
      Vec3d velocity = rotation.toVector().normalize().multiply(1.5).add(inherited);
      Flight flight = this.simulate(start, velocity, mc.player, null, 0, false, colliders, target.position());
      Impact hit = flight.impact();
      double error = hit == null ? 4.0 + flight.closest() : hit.position().squaredDistanceTo(target.position());
      double side = hit != null && target.face() != null
         ? (hit.position().x - target.position().x) * target.face().getOffsetX()
            + (hit.position().y - target.position().y) * target.face().getOffsetY()
            + (hit.position().z - target.position().z) * target.face().getOffsetZ()
         : 0.0;
      boolean sameSide = hit != null && (target.face() == null || hit.face() == target.face() || hit.face() == null && side >= -0.04);
      boolean valid = sameSide && error <= 1.0 && Math.abs(hit.position().y - target.position().y) <= 0.75;
      return new Branch(rotation, error, hit == null ? 160 : hit.ticks(), valid, flight.water());
   }

   public boolean stable(Vec3d start, Vec3d inherited, PearlRotation rotation, Impact target, List<Collider> colliders) {
      Vec3d launch = rotation.toVector().normalize().multiply(1.5).add(inherited);
      double spread = 0.02584125;

      for (int x = -1; x <= 1; x++) {
         for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
               int axes = Math.abs(x) + Math.abs(y) + Math.abs(z);
               if (axes == 1 || axes == 3) {
                  Flight flight = this.simulate(start, launch.add(x * spread, y * spread, z * spread), mc.player, null, 0, false, colliders, null);
                  Impact hit = flight.impact();
                  if (hit == null
                     || target.face() != null && hit.face() == target.face().getOpposite()
                     || hit.position().squaredDistanceTo(target.position()) > 16.0) {
                     return false;
                  }
               }
            }
         }
      }

      return true;
   }

   public static boolean better(Branch candidate, @Nullable Branch best) {
      if (best == null) {
         return true;
      } else if (candidate.valid() != best.valid()) {
         return candidate.valid();
      } else {
         return candidate.valid() ? candidate.error() + candidate.ticks() * 0.002 < best.error() + best.ticks() * 0.002 : candidate.error() < best.error();
      }
   }

   public Flight simulate(
      Vec3d start, Vec3d velocity, Entity owner, @Nullable EnderPearlEntity source, int age, boolean noGravity, List<Collider> colliders, @Nullable Vec3d goal
   ) {
      if (finite(start) && finite(velocity)) {
         Vec3d position = start;
         double closest = goal == null ? Double.POSITIVE_INFINITY : start.squaredDistanceTo(goal);
         boolean water = false;
         boolean leftOwner = source != null && ((elexis.mixins.accessors.JavelinProjectileAccessor) source).elexis$leftOwner();

         for (int tick = 1; tick <= 160 && this.remainingSimulationSteps-- > 0; tick++) {
            BlockPos block = BlockPos.ofFloored(position);
            if (!mc.world.isChunkLoaded(block) || position.y < mc.world.getBottomY() - 16) {
               break;
            }

            BlockState state = mc.world.getBlockState(block);
            if (state.isOf(Blocks.BUBBLE_COLUMN) || state.isOf(Blocks.NETHER_PORTAL) || state.isOf(Blocks.END_PORTAL)) {
               break;
            }

            boolean submerged = this.inWater(position);
            water |= submerged;
            double drag = submerged ? 0.8F : 0.99F;
            velocity = velocity.add(0.0, noGravity ? 0.0 : -0.03, 0.0).multiply(drag);
            Vec3d next = position.add(velocity);
            BlockHitResult blockHit = mc.world.raycast(new RaycastContext(position, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            Impact nearest = blockHit.getType() == HitResult.Type.MISS ? null : new Impact(blockHit.getPos(), blockHit.getSide(), tick);
            double nearestDistance = nearest == null ? Double.POSITIVE_INFINITY : position.squaredDistanceTo(nearest.position());
            double margin = MathHelper.clamp((age + tick - 3) / 20.0, 0.0, 0.3F);
            Box segment = new Box(position, next).expand(margin + 0.01);

            for (Collider collider : colliders) {
               if (segment.intersects(collider.bounds())) {
                  Entity entity = collider.entity();
                  if (entity.canBeHitByProjectile() && entity != source && (leftOwner || !owner.isConnectedThroughVehicle(entity))) {
                     Vec3d movement = collider.movement();
                     Box box = collider.box().offset(movement.multiply(Math.min(tick - 1, 5))).expand(margin);
                     Vec3d relativeEnd = next.subtract(tick <= 5 ? movement : Vec3d.ZERO);
                     if (segment.intersects(box.stretch(tick <= 5 ? movement : Vec3d.ZERO))) {
                        Optional<Vec3d> clipped = box.raycast(position, relativeEnd);
                        if (box.contains(position) || !clipped.isEmpty()) {
                           double progress = box.contains(position) ? 0.0 : segmentProgress(position, relativeEnd, clipped.get());
                           Vec3d hit = position.add(velocity.multiply(progress));
                           double distance = position.squaredDistanceTo(hit);
                           if (distance < nearestDistance) {
                              nearestDistance = distance;
                              nearest = new Impact(hit, null, tick);
                           }
                        }
                     }
                  }
               }
            }

            Vec3d collisionEnd = nearest == null ? next : nearest.position();
            boolean unsupported = false;
            Box travelled = new Box(position, collisionEnd).expand(0.125, 0.0, 0.125).stretch(0.0, 0.25, 0.0);

            for (BlockPos crossed : BlockPos.iterate(travelled)) {
               if (crossesBlock(position, collisionEnd, crossed)) {
                  if (!mc.world.isChunkLoaded(crossed)) {
                     unsupported = true;
                     break;
                  }

                  BlockState crossedState = mc.world.getBlockState(crossed);
                  if (crossedState.isOf(Blocks.BUBBLE_COLUMN) || crossedState.isOf(Blocks.NETHER_PORTAL) || crossedState.isOf(Blocks.END_PORTAL)) {
                     unsupported = true;
                     break;
                  }
               }
            }

            if (unsupported) {
               break;
            }

            if (goal != null) {
               Vec3d end = nearest == null ? next : nearest.position();
               double progress = segmentProgress(position, end, goal);
               closest = Math.min(closest, goal.squaredDistanceTo(position.add(end.subtract(position).multiply(progress))));
            }

            if (nearest != null) {
               return new Flight(nearest, water, closest);
            }

            if (!leftOwner) {
               Box swept = new Box(next.x - 0.125, next.y, next.z - 0.125, next.x + 0.125, next.y + 0.25, next.z + 0.125)
                  .stretch(velocity)
                  .expand(1.0);
               leftOwner = true;

               for (Collider collider : colliders) {
                  if (owner.isConnectedThroughVehicle(collider.entity())
                     && collider.entity().canHit()
                     && collider.box().offset(collider.movement().multiply(Math.min(tick, 5))).intersects(swept)) {
                     leftOwner = false;
                     break;
                  }
               }
            }

            position = next;
         }

         return new Flight(null, water, closest);
      } else {
         return new Flight(null, false, Double.POSITIVE_INFINITY);
      }
   }

   public static boolean crossesBlock(Vec3d start, Vec3d end, BlockPos block) {
      Box box = new Box(block).expand(0.125, 0.0, 0.125).stretch(0.0, -0.25, 0.0);
      return box.contains(start) || box.raycast(start, end).isPresent();
   }

   public boolean inWater(Vec3d position) {
      Box box = new Box(position.x - 0.125, position.y, position.z - 0.125, position.x + 0.125, position.y + 0.25, position.z + 0.125).contract(0.001);

      for (BlockPos pos : BlockPos.iterate(box)) {
         FluidState fluid = mc.world.getFluidState(pos);
         if (fluid.isIn(FluidTags.WATER) && pos.getY() + fluid.getHeight(mc.world, pos) >= box.minY) {
            return true;
         }
      }

      return false;
   }

   public static double segmentProgress(Vec3d start, Vec3d end, Vec3d point) {
      Vec3d delta = end.subtract(start);
      return delta.lengthSquared() < 1.0E-12 ? 0.0 : MathHelper.clamp(point.subtract(start).dotProduct(delta) / delta.lengthSquared(), 0.0, 1.0);
   }

    private boolean throwPearl(PearlRotation rotation, Hand hand) {
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        try {
            mc.player.setYaw(rotation.yaw());
            mc.player.setPitch(rotation.pitch());
            movementRotation = rotation;
            rotationLevel = mc.world;
            rotationPlayer = mc.player;
            var result = mc.interactionManager.interactItem(mc.player, hand);
            if (result.isAccepted()) {
                mc.player.swingHand(hand);
                mc.getEntityRenderDispatcher().getHeldItemRenderer().resetEquipProgress(hand);
                return true;
            }
            return false;
        } finally {
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        }
    }

    private static boolean finite(Vec3d value) {
        return value != null && Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    private void clearState() {
        landings.clear();
        handled.clear();
        lastTarget = null;
        sentPosition = null;
        sentMovement = Vec3d.ZERO;
        movementReady = false;
        receivedMovement = false;
        lastAttemptTick = Integer.MIN_VALUE;
        finishMovementRotation();
    }

    @Override
    public void onDisable() {
        clearState();
        trackedLevel = null;
        super.onDisable();
    }
}
