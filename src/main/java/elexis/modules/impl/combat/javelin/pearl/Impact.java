package elexis.modules.impl.combat.javelin.pearl;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public record Impact(Vec3d position, @Nullable Direction face, int ticks) {
}
