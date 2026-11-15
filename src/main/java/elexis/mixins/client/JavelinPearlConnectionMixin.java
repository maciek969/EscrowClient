package elexis.mixins.client;

import elexis.modules.impl.combat.TargetPearl;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public abstract class JavelinPearlConnectionMixin {
    @Inject(method = "sendImmediately", at = @At("TAIL"))
    private void elexis$observeSentMovement(Packet<?> packet, ChannelFutureListener listener,
                                           boolean flush, CallbackInfo ci) {
        if (packet instanceof PlayerMoveC2SPacket || packet instanceof ClientTickEndC2SPacket) {
            TargetPearl.observeSentPacket((ClientConnection) (Object) this, packet);
        }
    }
}
