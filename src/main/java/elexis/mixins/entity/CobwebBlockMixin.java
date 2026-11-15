package elexis.mixins.entity;

import elexis.Elexis;
import net.minecraft.block.BlockState;
import net.minecraft.block.CobwebBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CobwebBlock.class)
public abstract class CobwebBlockMixin {
    @Redirect(method = "onEntityCollision", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;slowMovement(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Vec3d;)V"))
    private void elexis$applyWebSlowdown(Entity entity, BlockState state, Vec3d slowdown) {
        if (entity == MinecraftClient.getInstance().player && Elexis.getFunctionManager() != null) {
            var noWeb = Elexis.getFunctionManager().getNoWeb();
            if (noWeb != null && noWeb.isState()) {
                if (noWeb.isGrimTest()) {
                    // Keep the vanilla vertical slowdown and any stronger Weaving bonus.
                    entity.slowMovement(state, new Vec3d(
                            Math.max(slowdown.x, 0.40D), slowdown.y, Math.max(slowdown.z, 0.40D)));
                }
                return;
            }
        }
        entity.slowMovement(state, slowdown);
    }
}
