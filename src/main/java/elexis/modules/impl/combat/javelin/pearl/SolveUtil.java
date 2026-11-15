package elexis.modules.impl.combat.javelin.pearl;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.Vec3d;

public final class SolveUtil {
   public static final double SPEED = 1.5;
   public static final double GRAVITY = 0.03;
   public static final double AIR_DRAG = 0.99F;
   public static final int MAX_TICKS = 160;

   public static List<PearlRotation> solve(Vec3d start, Vec3d inherited, Vec3d target) {
      return solve(start, inherited, target, 0.99F);
   }

   public static List<PearlRotation> solve(Vec3d start, Vec3d inherited, Vec3d target, double drag) {
      List<PearlRotation> result = new ArrayList<>(2);
      Vec3d displacement = target.subtract(start);
      double scale = 0.0;
      double fall = 0.0;
      double velocityScale = 1.0;
      double fallVelocity = 0.0;

      for (int tick = 0; tick < 160; tick++) {
         velocityScale *= drag;
         fallVelocity = (fallVelocity + 0.03) * drag;
         Vec3d base = displacement.add(0.0, fall, 0.0).subtract(inherited.multiply(scale));
         Vec3d slope = new Vec3d(0.0, fallVelocity, 0.0).subtract(inherited.multiply(velocityScale));
         double a = slope.lengthSquared() - 2.25 * velocityScale * velocityScale;
         double b = 2.0 * (base.dotProduct(slope) - 2.25 * scale * velocityScale);
         double c = base.lengthSquared() - 2.25 * scale * scale;
         if (Math.abs(a) < 1.0E-12) {
            if (Math.abs(b) > 1.0E-12) {
               addSolution(result, -c / b, base, slope, scale, velocityScale);
            }
         } else {
            double discriminant = b * b - 4.0 * a * c;
            double tolerance = 1.0E-12 * (b * b + Math.abs(4.0 * a * c) + 1.0);
            if (discriminant >= -tolerance) {
               double root = Math.sqrt(Math.max(0.0, discriminant));
               double q = -0.5 * (b + Math.copySign(root, b));
               double first = q == 0.0 ? -b / (2.0 * a) : q / a;
               double second = q == 0.0 ? first : c / q;
               addSolution(result, Math.min(first, second), base, slope, scale, velocityScale);
               if (Math.abs(first - second) > 1.0E-8) {
                  addSolution(result, Math.max(first, second), base, slope, scale, velocityScale);
               }
            }
         }

         scale += velocityScale;
         fall += fallVelocity;
      }

      return result;
   }

   public static void addSolution(List<PearlRotation> result, double fraction, Vec3d base, Vec3d slope, double scale, double velocityScale) {
      if (Double.isFinite(fraction) && !(fraction <= 1.0E-8) && !(fraction > 1.00000001)) {
         fraction = Math.min(fraction, 1.0);
         double totalScale = scale + fraction * velocityScale;
         Vec3d direction = base.add(slope.multiply(fraction)).multiply(1.0 / (1.5 * totalScale));
         float yaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
         float pitch = (float)(-Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z))));
         if (Float.isFinite(yaw) && Float.isFinite(pitch)) {
            result.add(new PearlRotation(yaw, pitch));
         }
      }
   }
}
