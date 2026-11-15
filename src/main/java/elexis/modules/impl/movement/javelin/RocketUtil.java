package elexis.modules.impl.movement.javelin;

import java.util.function.UnaryOperator;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class RocketUtil {
   public static final double MARGIN = 1.0E-4;
   public static final double RESIDUAL_FLOOR = 0.003;

   public static Vec3d rocket(Vec3d velocity, Vec3d look) {
      return velocity.multiply(0.5).add(look.multiply(0.85));
   }

   public static Vec3d threshold(Vec3d velocity, boolean modern) {
      boolean smallHorizontal = velocity.horizontalLengthSquared() < 9.0E-6;
      return new Vec3d(
         modern ? (smallHorizontal ? 0.0 : velocity.x) : (Math.abs(velocity.x) < 0.003 ? 0.0 : velocity.x),
         Math.abs(velocity.y) < 0.003 ? 0.0 : velocity.y,
         modern ? (smallHorizontal ? 0.0 : velocity.z) : (Math.abs(velocity.z) < 0.003 ? 0.0 : velocity.z)
      );
   }

   public static Vec3d travel(Vec3d velocity, Vec3d look, float pitch, double gravityY, double gravity, boolean slowFalling, boolean legacyTrig) {
      return travel(velocity, look, pitch, gravityY, gravity, slowFalling, legacyTrig, legacyTrig);
   }

   public static Vec3d travel(
      Vec3d velocity, Vec3d look, float pitch, double gravityY, double gravity, boolean slowFalling, boolean legacyTrig, boolean legacySlowGravity
   ) {
      float radians = pitch * (float) (Math.PI / 180.0);
      double horizontalLook = look.horizontalLength();
      double speed = velocity.horizontalLength();
      double lift = legacyTrig ? MathHelper.cos(radians) : Math.cos(radians);
      lift = lift * lift * Math.min(1.0, look.length() / 0.4);
      if (legacyTrig) {
         lift = (float)lift;
      }

      if (gravityY <= 0.0 && slowFalling) {
         gravity = legacySlowGravity ? 0.01 : Math.min(gravity, 0.01);
      }

      Vec3d result = velocity.add(0.0, gravity * (-1.0 + lift * 0.75), 0.0);
      if (result.y < 0.0 && horizontalLook > 0.0) {
         double conversion = result.y * -0.1 * lift;
         result = result.add(look.x * conversion / horizontalLook, conversion, look.z * conversion / horizontalLook);
      }

      if (radians < 0.0F && horizontalLook > 0.0) {
         double conversion = speed * -MathHelper.sin(radians) * 0.04;
         result = result.add(-look.x * conversion / horizontalLook, conversion * 3.2, -look.z * conversion / horizontalLook);
      }

      if (horizontalLook > 0.0) {
         result = result.add((look.x / horizontalLook * speed - result.x) * 0.1, 0.0, (look.z / horizontalLook * speed - result.z) * 0.1);
      }

      return result.multiply(0.99F, 0.98F, 0.99F);
   }

   public static double residual(double look, double lastLook, int rockets, double cap) {
      return Math.min(cap, Math.max(0.003, 0.5 * Math.abs(look - lastLook) * 0.85 * Math.min(rockets, 4)));
   }

   public static RocketSelection select(RocketEnvelope input, Double targetSpeed, UnaryOperator<Vec3d> collide) {
      if (finite(input.start())
         && finite(input.look())
         && finite(input.lastLook())
         && !(input.look().lengthSquared() < 1.0E-12)
         && input.rockets() > 0
         && Double.isFinite(input.gravity())
         && Double.isFinite(input.residualCap())) {
         Vec3d direction = input.look().normalize();
         Vec3d tolerance = new Vec3d(
            Math.max(0.0, residual(input.look().x, input.lastLook().x, input.rockets(), input.residualCap()) + input.horizontalExtra() - 1.0E-4),
            Math.max(0.0, residual(input.look().y, input.lastLook().y, input.rockets(), input.residualCap()) + input.verticalExtra() - 1.0E-4),
            Math.max(0.0, residual(input.look().z, input.lastLook().z, input.rockets(), input.residualCap()) + input.horizontalExtra() - 1.0E-4)
         );
         RocketSelection best = null;
         boolean skip = input.start().lengthSquared() < input.movementThreshold() * input.movementThreshold();

         for (int startIndex = 0; startIndex < (skip ? 3 : 1); startIndex++) {
            Vec3d start = startIndex == 0 ? threshold(input.start(), input.modernClamp()) : (startIndex == 1 ? Vec3d.ZERO : new Vec3d(0.0, input.start().y, 0.0));
            Vec3d extra = startIndex == 0
               ? tolerance
               : tolerance.add(
                  0.99 * input.movementThreshold() * 2.0 + input.movementThreshold(),
                  input.movementThreshold() * 2.0,
                  0.99 * input.movementThreshold() * 2.0 + input.movementThreshold()
               );

            for (int lookIndex = 0; lookIndex < 2; lookIndex++) {
               Vec3d boosted = start;
               Vec3d rocketLook = lookIndex == 0 ? input.look() : input.lastLook();

               for (int count = 0; count <= input.rockets(); count++) {
                  if (count > 0) {
                     boosted = rocket(boosted, rocketLook);
                  }

                  Vec3d centre = travel(
                     boosted, input.look(), input.pitch(), input.start().y, input.gravity(), input.slowFalling(), input.legacyTrig(), input.legacySlowGravity()
                  );
                  Vec3d min = centre.subtract(extra);
                  Vec3d max = centre.add(extra);
                  if (input.hardEntity()) {
                     min = new Vec3d(Math.min(min.x, 0.0) - 0.05, Math.min(min.y, 0.0), Math.min(min.z, 0.0) - 0.05);
                     max = new Vec3d(Math.max(max.x, 0.0) + 0.05, Math.max(max.y, 0.0), Math.max(max.z, 0.0) + 0.05);
                  }

                  Vec3d ray = targetSpeed == null && direction.horizontalLengthSquared() > 0.01
                     ? alongHorizontalLook(direction, min, max)
                     : alongLook(direction, min, max, targetSpeed);
                  Vec3d velocity = ray != null ? ray : closestToLook(direction, min, max, targetSpeed);
                  Vec3d movement = collide.apply(velocity);
                  if (finite(movement)) {
                     Vec3d steeringMovement = steering(movement, direction);
                     Vec3d steeringDirection = steering(direction, direction);
                     boolean aligned = steeringMovement.lengthSquared() > 1.0E-12
                        && steeringMovement.dotProduct(steeringDirection) > 0.0
                        && steeringMovement.crossProduct(steeringDirection).lengthSquared() <= 1.0E-12;
                     RocketSelection candidate = new RocketSelection(velocity, movement, centre, min, max, aligned);
                     if (better(candidate, best, direction, targetSpeed)) {
                        best = candidate;
                     }

                     if (count > 0 && boosted.squaredDistanceTo(rocket(boosted, rocketLook)) < 1.0E-24) {
                        break;
                     }
                  }
               }

               if (input.look().squaredDistanceTo(input.lastLook()) < 1.0E-24) {
                  break;
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   public static Vec3d alongLook(Vec3d direction, Vec3d min, Vec3d max, Double targetSpeed) {
      double low = 0.0;
      double high = Double.POSITIVE_INFINITY;

      for (int axis = 0; axis < 3; axis++) {
         double component = axis == 0 ? direction.x : (axis == 1 ? direction.y : direction.z);
         double a = axis == 0 ? min.x : (axis == 1 ? min.y : min.z);
         double b = axis == 0 ? max.x : (axis == 1 ? max.y : max.z);
         if (Math.abs(component) < 1.0E-10) {
            if (a > 0.0 || b < 0.0) {
               return null;
            }
         } else {
            low = Math.max(low, Math.min(a / component, b / component));
            high = Math.min(high, Math.max(a / component, b / component));
         }
      }

      return Double.isFinite(high) && !(high <= 0.0) && !(high < low) ? direction.multiply(targetSpeed == null ? high : MathHelper.clamp(targetSpeed, low, high)) : null;
   }

   public static Vec3d alongHorizontalLook(Vec3d direction, Vec3d min, Vec3d max) {
      double horizontal = direction.horizontalLength();
      Vec3d heading = new Vec3d(direction.x / horizontal, 0.0, direction.z / horizontal);
      Vec3d ray = alongLook(heading, new Vec3d(min.x, Double.NEGATIVE_INFINITY, min.z), new Vec3d(max.x, Double.POSITIVE_INFINITY, max.z), null);
      if (ray == null) {
         return null;
      }

      double desiredY = ray.horizontalLength() * direction.y / horizontal;
      return new Vec3d(ray.x, MathHelper.clamp(desiredY, min.y, max.y), ray.z);
   }

   public static Vec3d closestToLook(Vec3d direction, Vec3d min, Vec3d max, Double targetSpeed) {
      if (targetSpeed != null) {
         return clamp(direction.multiply(targetSpeed), min, max);
      }

      if (!(direction.horizontalLengthSquared() > 0.01)) {
         double low = 0.0;
         double high = Math.sqrt(Math.max(min.x * min.x, max.x * max.x) + Math.max(min.y * min.y, max.y * max.y) + Math.max(min.z * min.z, max.z * max.z))
            + 1.0;

         for (int iteration = 0; iteration < 48; iteration++) {
            double t = (low + high) * 0.5;
            double x = direction.x * t;
            double y = direction.y * t;
            double z = direction.z * t;
            double derivative = (x - MathHelper.clamp(x, min.x, max.x)) * direction.x
               + (y - MathHelper.clamp(y, min.y, max.y)) * direction.y
               + (z - MathHelper.clamp(z, min.z, max.z)) * direction.z;
            if (derivative <= 1.0E-14) {
               low = t;
            } else {
               high = t;
            }
         }

         return clamp(direction.multiply(low), min, max);
      } else {
         Vec3d best = null;
         double bestAlignment = -2.0;
         double bestProgress = Double.NEGATIVE_INFINITY;
         double horizontal = direction.horizontalLength();

         for (int corner = 0; corner < 4; corner++) {
            double x = (corner & 1) == 0 ? min.x : max.x;
            double z = (corner & 2) == 0 ? min.z : max.z;
            double speed = Math.hypot(x, z);
            double progress = (x * direction.x + z * direction.z) / horizontal;
            double alignment = speed > 1.0E-10 ? progress / speed : -2.0;
            if (alignment > bestAlignment + 1.0E-12 || Math.abs(alignment - bestAlignment) <= 1.0E-12 && progress > bestProgress) {
               bestAlignment = alignment;
               bestProgress = progress;
               best = new Vec3d(x, MathHelper.clamp(progress * direction.y / horizontal, min.y, max.y), z);
            }
         }

         return best;
      }
   }

   public static boolean better(RocketSelection candidate, RocketSelection best, Vec3d direction, Double targetSpeed) {
      if (best == null) {
         return true;
      }

      if (targetSpeed != null) {
         return candidate.movement().squaredDistanceTo(direction.multiply(targetSpeed)) < best.movement().squaredDistanceTo(direction.multiply(targetSpeed));
      }

      if (candidate.aligned() != best.aligned()) {
         return candidate.aligned();
      }

      Vec3d movement = steering(candidate.movement(), direction);
      Vec3d bestMovement = steering(best.movement(), direction);
      Vec3d heading = steering(direction, direction).normalize();
      double speed = movement.length();
      double bestSpeed = bestMovement.length();
      double alignment = speed > 1.0E-10 ? movement.dotProduct(heading) / speed : -2.0;
      double bestAlignment = bestSpeed > 1.0E-10 ? bestMovement.dotProduct(heading) / bestSpeed : -2.0;
      return Math.abs(alignment - bestAlignment) > 1.0E-8 ? alignment > bestAlignment : movement.dotProduct(heading) > bestMovement.dotProduct(heading);
   }

   public static Vec3d steering(Vec3d movement, Vec3d direction) {
      return direction.horizontalLengthSquared() > 0.01 ? new Vec3d(movement.x, 0.0, movement.z) : movement;
   }

   public static Vec3d clamp(Vec3d v, Vec3d min, Vec3d max) {
      return new Vec3d(MathHelper.clamp(v.x, min.x, max.x), MathHelper.clamp(v.y, min.y, max.y), MathHelper.clamp(v.z, min.z, max.z));
   }

   public static boolean finite(Vec3d v) {
      return v != null && Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
   }
}
