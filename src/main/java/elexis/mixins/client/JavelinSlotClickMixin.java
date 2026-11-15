package elexis.mixins.client;

import elexis.Elexis;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class JavelinSlotClickMixin {
    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    private void elexis$slotClick(int syncId, int slot, int button, SlotActionType action, PlayerEntity player, CallbackInfo ci) {
        var manager = Elexis.getFunctionManager();
        if (manager == null) return;
        if (manager.getRwHelper().isState() && manager.getRwHelper().protectFriend(syncId, slot)) {
            ci.cancel();
            return;
        }
        if (manager.getItemScroller().isState() && manager.getItemScroller().dropMatching(syncId, slot, action)) ci.cancel();
    }
}
