package elexis.mixins.accessors;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(LivingEntity.class)
public interface JavelinPotionSwirlsAccessor {
    @Accessor("POTION_SWIRLS")
    static TrackedData<List<ParticleEffect>> elexis$potionSwirls() { throw new AssertionError(); }
}
