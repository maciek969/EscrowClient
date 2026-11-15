package elexis.mixins.client;

import elexis.Elexis;
import elexis.client.events.impl.entity.EventAttack;
import elexis.client.events.impl.entity.EventBlockInteract;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void Elexis$attackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        var manager = Elexis.getFunctionManager();
        if (manager != null && manager.getTriggerBot() != null
                && manager.getTriggerBot().shouldBlockAttack(player, target)) {
            ci.cancel();
            return;
        }
        if (target != null) {
            Elexis.getEventBus().post(new EventAttack(target));
        }
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), require = 0)
    private void Elexis$interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (player == null || hitResult == null) {
            return;
        }
        BlockPos placePos = hitResult.getBlockPos().offset(hitResult.getSide());
        Elexis.getEventBus().post(new EventBlockInteract(hand, player.getStackInHand(hand).copy(), hitResult, placePos));
    }
}
