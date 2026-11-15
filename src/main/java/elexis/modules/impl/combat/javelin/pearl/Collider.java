package elexis.modules.impl.combat.javelin.pearl;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public record Collider(net.minecraft.entity.Entity entity, Box box, Vec3d movement, Box bounds) {
}
