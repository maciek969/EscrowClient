package elexis.mixins.entity;

import elexis.Elexis;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static elexis.modules.impl.player.PlayerUtilsFunction.NoPush;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "pushAwayFrom(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    public void cancelPushAway(Entity other, CallbackInfo ci) {
        if (NoPush.get() && Elexis.getFunctionManager().getPlayerUtilsFunction().isState()) ci.cancel();
    }
}