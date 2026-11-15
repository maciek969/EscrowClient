package elexis.modules.impl.movement.javelin;

import net.minecraft.util.math.Vec3d;

public record RocketSelection(Vec3d velocity, Vec3d movement, Vec3d centre, Vec3d min, Vec3d max, boolean aligned) {
}
