package elexis.modules.impl.render.javelin.target;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

public record Fragment(Sample sample, Vec3d velocity, long born, long life) {
}
