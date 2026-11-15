package elexis.mixins.accessors;

import net.minecraft.entity.projectile.ProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ProjectileEntity.class)
public interface JavelinProjectileAccessor {
    @Accessor("leftOwner")
    boolean elexis$leftOwner();
}
