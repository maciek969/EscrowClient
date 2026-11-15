package elexis.mixins.render;

import elexis.modules.impl.render.javelin.crystalhands.CrystalEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class JavelinCrystalRemovalMixin {
    @Inject(method="setRemoved",at=@At("HEAD"))
    private void javelin$removed(Entity.RemovalReason reason, CallbackInfo ci) {
        if ((Object)this instanceof EndCrystalEntity crystal) CrystalEffects.onRemove(crystal, reason);
    }
}
