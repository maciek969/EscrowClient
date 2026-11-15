package elexis.mixins.client;

import elexis.Elexis;
import elexis.client.events.impl.client.EventWorldEmit;
import net.minecraft.client.world.WorldEventHandler;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldEventHandler.class)
public class WorldEventHandlerMixin {
    @Inject(method = "processWorldEvent", at = @At("HEAD"))
    private void Elexis$onWorldEvent(int eventId, BlockPos pos, int data, CallbackInfo ci) {
        Elexis.getEventBus().post(new EventWorldEmit(eventId, data, pos));
    }
}
