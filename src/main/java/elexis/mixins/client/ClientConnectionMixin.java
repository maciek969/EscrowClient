package elexis.mixins.client;

import elexis.Elexis;
import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.events.impl.client.EventSendPacket;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private static void Elexis$onPacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) {
            return;
        }
        EventReceivePacket event = new EventReceivePacket(packet);
        Elexis.getEventBus().post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void Elexis$onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null || packet == null) {
            return;
        }
        EventSendPacket event = new EventSendPacket(packet);
        Elexis.getEventBus().post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
