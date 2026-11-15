package elexis.modules.impl.movement;

import elexis.Elexis;
import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.impl.combat.HitAura;
import elexis.modules.impl.movement.javelin.RocketEnvelope;
import elexis.modules.impl.movement.javelin.RocketSelection;
import elexis.modules.impl.movement.javelin.RocketUtil;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

@FunctionAdd(name = "ElytraBooster", alias = "Elytra Booster", category = Category.Movement,
        description = "Буст элитры из Javelin с расчётом импульса фейерверка")
public final class ElytraBooster extends Function {
    public final ModeSetting mode = new ModeSetting("Режим", "Grim", "Grim", "GroundedGrim", "ReallyWorld", "Bravo");
    private Object groundedLevel;
    private boolean groundedWasGliding;
    private int groundedGlideStart = Integer.MIN_VALUE;
    private int groundedPushTick = Integer.MIN_VALUE;
    private int groundedHardTick = Integer.MIN_VALUE;
    private int groundedTravelTick = Integer.MIN_VALUE;
    private int lastFireworkTick = Integer.MIN_VALUE;
    private int fireworkCount;
    private Vec3d lastServerVelocity;
    private float lastYaw, lastPitch, previousYaw, previousPitch;

    public ElytraBooster() { addSettings(mode); }

    @EventHandler
    public void onTick(EventUpdate.Fast event) {
        if (nullCheck()) { resetBoost(); return; }
        if (groundedLevel != mc.world) {
            resetBoost();
            groundedLevel = mc.world;
            previousYaw = lastYaw = mc.player.getYaw();
            previousPitch = lastPitch = mc.player.getPitch();
        }
        boolean gliding = mc.player.isGliding();
        if (gliding && !groundedWasGliding) groundedGlideStart = mc.player.age;
        if (!gliding) {
            lastFireworkTick = Integer.MIN_VALUE;
            fireworkCount = 0;
            lastServerVelocity = null;
            groundedGlideStart = Integer.MIN_VALUE;
        }
        groundedWasGliding = gliding;
        previousYaw = lastYaw;
        previousPitch = lastPitch;
        lastYaw = mc.player.getYaw();
        lastPitch = mc.player.getPitch();
    }

    @EventHandler
    public void onPacket(EventReceivePacket event) {
        if (mc.player != null && (event.getPacket() instanceof PlayerPositionLookS2CPacket
                || event.getPacket() instanceof EntityVelocityUpdateS2CPacket velocity && velocity.getEntityId() == mc.player.getId())) {
            mc.execute(this::resetBoost);
        }
    }

    public void recordFirework(Vec3d oldVelocity) {
        if (!isState() || nullCheck() || !mc.player.isGliding()) return;
        if (lastFireworkTick != mc.player.age) {
            lastServerVelocity = oldVelocity;
            lastFireworkTick = mc.player.age;
            fireworkCount = 1;
        } else fireworkCount++;
    }

    public Vec3d modifyGlidingVelocity(Vec3d vanillaVelocity) {
        if (!isState() || nullCheck() || !mc.player.isGliding()) return vanillaVelocity;
        if (mc.player.age - lastFireworkTick != 1 || lastServerVelocity == null) return vanillaVelocity;
        var aura = Elexis.getFunctionManager().getHitAura();
        if (aura.isState() && HitAura.target != null && mc.player.distanceTo(HitAura.target) < 3
                && mc.player.getAttackCooldownProgress(0) >= 0.92f) return vanillaVelocity;
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        float priorYaw = yaw != lastYaw || pitch != lastPitch ? lastYaw : previousYaw;
        float priorPitch = yaw != lastYaw || pitch != lastPitch ? lastPitch : previousPitch;
        Vec3d look = mc.player.getRotationVector(pitch, yaw);
        Vec3d lastLook = mc.player.getRotationVector(mode.is("GroundedGrim") ? lastPitch : priorPitch,
                mode.is("GroundedGrim") ? lastYaw : priorYaw);
        Vec3d boosted = mode.is("GroundedGrim") ? groundedGrimBoost(look, lastLook, pitch, vanillaVelocity)
                : grimBoost(look, lastLook, pitch);
        if (mode.is("ReallyWorld")) boosted = limitReallyWorldBoost(boosted, vanillaVelocity);
        if (mode.is("Bravo")) boosted = limitBravoBoost(boosted, vanillaVelocity);
        if (boosted == null || !Double.isFinite(boosted.x) || !Double.isFinite(boosted.y) || !Double.isFinite(boosted.z)) return vanillaVelocity;
        if (!mode.is("GroundedGrim") && aura.isState() && HitAura.target != null && HitAura.target.isAlive()) {
            Vec3d delta = HitAura.target.getEyePos().subtract(mc.player.getEyePos().add(mc.player.getVelocity()));
            double targetYaw = Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
            if (Math.abs(MathHelper.wrapDegrees(targetYaw - yaw)) > 80 && boosted.lengthSquared() > 1.0e-12) {
                boosted = boosted.normalize().multiply(1.5);
            }
        }
        return boosted;
    }

    private static int protocolVersion() { return SharedConstants.getGameVersion().protocolVersion(); }

   public Vec3d grimBoost(Vec3d currentLook, Vec3d lastLook, float currentPitch) {
      double gravity = mc.player.getFinalGravity();
      boolean slowFalling = mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
      Vec3d predicted = this.predictElytra(this.lastServerVelocity, currentLook, currentPitch, gravity, slowFalling);
      boolean pointThree = this.viaVersionProtocol();
      double antiTickSkip = pointThree ? 0.0 : 0.05;
      Vec3d fireworkMin = new Vec3d(
         this.fireworkMin(currentLook.x, lastLook.x, antiTickSkip),
         this.fireworkMin(currentLook.y, lastLook.y, antiTickSkip),
         this.fireworkMin(currentLook.z, lastLook.z, antiTickSkip)
      );
      Vec3d fireworkMax = new Vec3d(
         this.fireworkMax(currentLook.x, lastLook.x, antiTickSkip),
         this.fireworkMax(currentLook.y, lastLook.y, antiTickSkip),
         this.fireworkMax(currentLook.z, lastLook.z, antiTickSkip)
      );
      Vec3d allowedMin = new Vec3d(
         predicted.x + Math.min(0.0, fireworkMin.x - this.lastServerVelocity.x),
         predicted.y + Math.min(0.0, fireworkMin.y - this.lastServerVelocity.y),
         predicted.z + Math.min(0.0, fireworkMin.z - this.lastServerVelocity.z)
      );
      Vec3d allowedMax = new Vec3d(
         predicted.x + Math.max(0.0, fireworkMax.x - this.lastServerVelocity.x),
         predicted.y + Math.max(0.0, fireworkMax.y - this.lastServerVelocity.y),
         predicted.z + Math.max(0.0, fireworkMax.z - this.lastServerVelocity.z)
      );
      Vec3d boosted = this.velocityAlongLook(currentLook, allowedMin, allowedMax, null);
      if (pointThree) {
         Vec3d zeroPrediction = this.predictElytra(Vec3d.ZERO, currentLook, currentPitch, gravity, slowFalling);
         Vec3d pointThreeMin = new Vec3d(
            zeroPrediction.x + fireworkMin.x - 0.0894, zeroPrediction.y + fireworkMin.y - 0.06, zeroPrediction.z + fireworkMin.z - 0.0894
         );
         Vec3d pointThreeMax = new Vec3d(
            zeroPrediction.x + fireworkMax.x + 0.0894, zeroPrediction.y + fireworkMax.y + 0.06, zeroPrediction.z + fireworkMax.z + 0.0894
         );
         Vec3d pointThreeBoosted = this.velocityAlongLook(currentLook, pointThreeMin, pointThreeMax, null);
         if (this.isBetterBoost(pointThreeBoosted, boosted, null)) {
            boosted = pointThreeBoosted;
         }
      }

      return boosted;
   }

   public boolean isBetterBoost(Vec3d candidate, Vec3d current, Double targetSpeed) {
      if (candidate == null) {
         return false;
      } else if (current == null) {
         return true;
      } else {
         return targetSpeed == null
            ? candidate.lengthSquared() > current.lengthSquared()
            : Math.abs(candidate.length() - targetSpeed) < Math.abs(current.length() - targetSpeed);
      }
   }

   public Vec3d limitReallyWorldBoost(Vec3d boosted, Vec3d vanillaVelocity) {
      if (boosted == null) {
         return null;
      }

      double horizontalLimit = Math.max(1.9949999999999999, vanillaVelocity.horizontalLength());
      double horizontalSpeed = boosted.horizontalLength();
      if (horizontalSpeed > horizontalLimit) {
         double scale = horizontalLimit / horizontalSpeed;
         boosted = new Vec3d(boosted.x * scale, boosted.y, boosted.z * scale);
      }

      double totalLimit = Math.max(3.0, vanillaVelocity.length());
      double totalSpeed = boosted.length();
      if (totalSpeed > totalLimit) {
         boosted = boosted.multiply(totalLimit / totalSpeed);
      }

      return boosted;
   }

   public Vec3d limitBravoBoost(Vec3d boosted, Vec3d vanillaVelocity) {
      if (boosted == null) {
         return null;
      }

      double horizontalLimit = Math.max(2.25, vanillaVelocity.horizontalLength());
      double horizontalSpeed = boosted.horizontalLength();
      if (horizontalSpeed > horizontalLimit) {
         double scale = horizontalLimit / horizontalSpeed;
         boosted = new Vec3d(boosted.x * scale, boosted.y, boosted.z * scale);
      }

      double totalLimit = Math.max(2.25, vanillaVelocity.length());
      double totalSpeed = boosted.length();
      if (totalSpeed > totalLimit) {
         boosted = boosted.multiply(totalLimit / totalSpeed);
      }

      return boosted;
   }

   public Vec3d groundedGrimBoost(Vec3d currentLook, Vec3d lastLook, float currentPitch, Vec3d vanillaVelocity) {
      int tick = mc.player.age;
      int protocol = protocolVersion();
      if (this.lastServerVelocity != null
         && this.fireworkCount > 0
         && protocol > 0
         && this.groundedTravelTick != tick
         && !mc.player.isTouchingWater()
         && !mc.player.isInLava()
         && !mc.player.isOnGround()
         && !mc.player.isClimbing()
         && !mc.player.hasVehicle()
         && !mc.player.isSpectator()
         && !mc.player.getAbilities().flying
         && !this.inStuckBlock()) {
         this.groundedTravelTick = tick;
         double gravity = mc.player.getFinalGravity();
         boolean slowFalling = mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING);
         Vec3d actualInput = mc.player.getVelocity();
         Vec3d localBaseline = RocketUtil.travel(actualInput, currentLook, currentPitch, actualInput.y, gravity, slowFalling, false);
         if (localBaseline.squaredDistanceTo(vanillaVelocity) > 1.0E-8) {
            return vanillaVelocity;
         }

         Box box = mc.player.getBoundingBox();
         Predicate<Entity> pushable = entity -> entity.isPushable() && entity != mc.player;

         for (Entity entity : mc.world.getOtherEntities(mc.player, box.expand(1.0))) {
            if (entity instanceof AbstractBoatEntity || entity instanceof ShulkerEntity || entity instanceof HappyGhastEntity) {
               this.groundedHardTick = tick;
            }

            if (pushable.test(entity) && entity.getBoundingBox().intersects(box.expand(0.5))) {
               this.groundedPushTick = tick;
            }
         }

         boolean hardRecent = recent(this.groundedHardTick, tick, 3);
         double horizontalExtra = recent(this.groundedPushTick, tick, 3) ? 0.05 : 0.0;
         double verticalExtra = 0.0;
         if (recent(this.groundedHardTick, tick, 2)) {
            horizontalExtra += 0.03;
            verticalExtra += 0.03;
         }

         if (recent(this.groundedGlideStart, tick, 4)) {
            double decay = 1.0 - (tick - this.groundedGlideStart) / 5.0;
            horizontalExtra += 0.1 * decay;
            verticalExtra += 0.2 * decay;
         }

         double reach = Math.max(actualInput.length(), this.lastServerVelocity.length()) + 3.0;
         Box search = box.expand(reach);
         List<VoxelShape> entityShapes = mc.world.getEntityCollisions(mc.player, search);
         UnaryOperator<Vec3d> collisions = mc.world.isSpaceEmpty(mc.player, search)
            ? UnaryOperator.identity()
            : velocity -> Entity.adjustMovementForCollisions(mc.player, velocity, box, mc.world, entityShapes);
         RocketEnvelope input = new RocketEnvelope(
            this.lastServerVelocity,
            currentLook,
            lastLook,
            currentPitch,
            gravity,
            slowFalling,
            this.fireworkCount,
            0.02,
            horizontalExtra,
            verticalExtra,
            protocol < 758 ? 0.03 : 2.0E-4,
            protocol < 758,
            protocol < 766,
            protocol >= 770,
            hardRecent
         );
         RocketSelection selection = RocketUtil.select(input, null, collisions);
         return selection == null ? vanillaVelocity : selection.velocity();
      } else {
         return vanillaVelocity;
      }
   }

   public static boolean recent(int previous, int tick, int window) {
      return previous != Integer.MIN_VALUE && tick >= previous && tick - previous <= window;
   }

   public boolean inStuckBlock() {
      Box box = mc.player.getBoundingBox().contract(1.0E-5);
      BlockPos.Mutable pos = new BlockPos.Mutable();

      for (int x = MathHelper.floor(box.minX); x <= MathHelper.floor(box.maxX); x++) {
         for (int y = MathHelper.floor(box.minY); y <= MathHelper.floor(box.maxY); y++) {
            for (int z = MathHelper.floor(box.minZ); z <= MathHelper.floor(box.maxZ); z++) {
               BlockState state = mc.world.getBlockState(pos.set(x, y, z));
               if (state.isOf(Blocks.COBWEB) || state.isOf(Blocks.SWEET_BERRY_BUSH) || state.isOf(Blocks.POWDER_SNOW)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public Vec3d predictElytra(Vec3d velocity, Vec3d look, float pitch, double gravity, boolean slowFalling) {
      return this.predictElytra(velocity, look, pitch, velocity.y, gravity, slowFalling);
   }

   public Vec3d predictElytra(Vec3d velocity, Vec3d look, float pitch, double gravityVelocityY, double gravity, boolean slowFalling) {
      float pitchRadians = pitch * (float) (Math.PI / 180.0);
      double lookHorizontal = Math.sqrt(look.x * look.x + look.z * look.z);
      double horizontalSpeed = velocity.horizontalLength();
      double lift = Math.cos(pitchRadians);
      lift = lift * lift * Math.min(1.0, look.length() / 0.4);
      if (gravityVelocityY <= 0.0 && slowFalling) {
         gravity = Math.min(gravity, 0.01);
      }

      Vec3d result = velocity.add(0.0, gravity * (-1.0 + lift * 0.75), 0.0);
      if (result.y < 0.0 && lookHorizontal > 0.0) {
         double convert = result.y * -0.1 * lift;
         result = result.add(look.x * convert / lookHorizontal, convert, look.z * convert / lookHorizontal);
      }

      if (pitchRadians < 0.0F && lookHorizontal > 0.0) {
         double convert = horizontalSpeed * -MathHelper.sin(pitchRadians) * 0.04;
         result = result.add(-look.x * convert / lookHorizontal, convert * 3.2, -look.z * convert / lookHorizontal);
      }

      if (lookHorizontal > 0.0) {
         result = result.add((look.x / lookHorizontal * horizontalSpeed - result.x) * 0.1, 0.0, (look.z / lookHorizontal * horizontalSpeed - result.z) * 0.1);
      }

      return result.multiply(0.99F, 0.98F, 0.99F);
   }

   public double fireworkMin(double currentLook, double lastLook, double antiTickSkip) {
      double value = (Math.min(-antiTickSkip, currentLook) + Math.min(-antiTickSkip, lastLook)) * 1.7;
      return Math.max(-1.7, value);
   }

   public double fireworkMax(double currentLook, double lastLook, double antiTickSkip) {
      double value = (Math.max(antiTickSkip, currentLook) + Math.max(antiTickSkip, lastLook)) * 1.7;
      return Math.min(1.7, value);
   }

   public boolean viaVersionProtocol() {
      int protocol = protocolVersion();
      return protocol > 0 && protocol < 758;
   }

   public Vec3d velocityAlongLook(Vec3d look, Vec3d allowedMin, Vec3d allowedMax, Double targetSpeed) {
      double minimumScale = 0.0;
      double maximumScale = Double.POSITIVE_INFINITY;
      double minimum = allowedMin.x + 0.005;
      double maximum = allowedMax.x - 0.005;
      double component = look.x;
      if (minimum > maximum) {
         return null;
      }

      if (Math.abs(component) < 1.0E-4) {
         if (minimum > 0.0 || maximum < 0.0) {
            return null;
         }
      } else {
         double first = minimum / component;
         double second = maximum / component;
         minimumScale = Math.max(minimumScale, Math.min(first, second));
         maximumScale = Math.min(maximumScale, Math.max(first, second));
      }

      minimum = allowedMin.y + 0.005;
      maximum = allowedMax.y - 0.005;
      component = look.y;
      if (minimum > maximum) {
         return null;
      }

      if (Math.abs(component) < 1.0E-4) {
         if (minimum > 0.0 || maximum < 0.0) {
            return null;
         }
      } else {
         double first = minimum / component;
         double second = maximum / component;
         minimumScale = Math.max(minimumScale, Math.min(first, second));
         maximumScale = Math.min(maximumScale, Math.max(first, second));
      }

      minimum = allowedMin.z + 0.005;
      maximum = allowedMax.z - 0.005;
      component = look.z;
      if (minimum > maximum) {
         return null;
      }

      if (Math.abs(component) < 1.0E-4) {
         if (minimum > 0.0 || maximum < 0.0) {
            return null;
         }
      } else {
         double first = minimum / component;
         double second = maximum / component;
         minimumScale = Math.max(minimumScale, Math.min(first, second));
         maximumScale = Math.min(maximumScale, Math.max(first, second));
      }

      if (Double.isFinite(maximumScale) && !(maximumScale <= 0.0) && !(maximumScale < minimumScale)) {
         double selectedScale = targetSpeed == null ? maximumScale : MathHelper.clamp(targetSpeed, minimumScale, maximumScale);
         return look.multiply(selectedScale);
      } else {
         return null;
      }
   }

   public void resetBoost() {
      this.lastFireworkTick = Integer.MIN_VALUE;
      this.fireworkCount = 0;
      this.lastServerVelocity = null;
      this.groundedLevel = null;
      this.groundedWasGliding = false;
      this.groundedGlideStart = Integer.MIN_VALUE;
      this.groundedPushTick = Integer.MIN_VALUE;
      this.groundedHardTick = Integer.MIN_VALUE;
      this.groundedTravelTick = Integer.MIN_VALUE;
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.resetBoost();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.resetBoost();
   }
}
