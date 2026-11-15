package elexis.mixins.entity;

import elexis.modules.impl.render.NoRneder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class NoRenderEntityMixin {

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true, require = 0)
    private void noRender$invisible(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;

        if (NoRneder.is("Arrows in player") && self instanceof PersistentProjectileEntity arrow) {
            if (arrow.getOwner() != null
                    && MinecraftClient.getInstance().player != null
                    && arrow.getOwner().getId() != MinecraftClient.getInstance().player.getId()
                    && arrow.getVelocity().lengthSquared() < 0.001) {
                cir.setReturnValue(true);
                return;
            }
        }

        if (NoRneder.is("Armor stands") && self instanceof ArmorStandEntity) {
            cir.setReturnValue(true);
        }
    }
}
