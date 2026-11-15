package elexis.modules.impl.movement.javelin;

import net.minecraft.util.math.Vec3d;

public record RocketEnvelope(
   Vec3d start,
   Vec3d look,
   Vec3d lastLook,
   float pitch,
   double gravity,
   boolean slowFalling,
   int rockets,
   double residualCap,
   double horizontalExtra,
   double verticalExtra,
   double movementThreshold,
   boolean legacyTrig,
   boolean legacySlowGravity,
   boolean modernClamp,
   boolean hardEntity
) {
}
