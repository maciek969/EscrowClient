package elexis.mixins.client;

import elexis.Elexis;
import elexis.client.events.impl.client.EventChat;
import elexis.commands.commands.CommandDispatcher;
import elexis.modules.impl.utils.AutoAccept;
import elexis.modules.impl.utils.UnHook;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onExplosion", at = @At("TAIL"))
    private void Elexis$explosionShader(net.minecraft.network.packet.s2c.play.ExplosionS2CPacket packet, CallbackInfo ci) {
        var module=elexis.modules.impl.render.visual.SceneVisuals.active("TNTEffect",elexis.modules.impl.render.visual.ImpactVisuals.TNTEffect.class);
        if(module!=null) module.explosion(packet.center());
    }
    @Inject(method = "onPlayerList", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"), cancellable = true)
    public void antiIngoringPlayerInfoSelfcode(PlayerListS2CPacket packet, CallbackInfo ci) {
        ci.cancel(); // хах селфкод
    }

    @Inject(at = @At("HEAD"), method = "sendChatMessage(Ljava/lang/String;)V", cancellable = true)
    private void sendChatMessage(String string, CallbackInfo ci) {
        EventChat eventChat = new EventChat(string);
        Elexis.getEventBus().post(eventChat);
        if (eventChat.isCancelled()) {
            ci.cancel();
            return;
        }

        if (UnHook.unhooked && Objects.equals(string, UnHook.getCode())) {
            ci.cancel();
            UnHook.unhooked = false;
            Elexis.getFunctionManager().getUnHook().setState(false);
        }
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (message.startsWith(CommandDispatcher.prefix) && !UnHook.unhooked) {
            ci.cancel();
        }
    }

    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (Elexis.getFunctionManager() == null || Elexis.getFunctionManager().getAutoAccept() == null || packet == null || packet.content() == null) {
            return;
        }

        AutoAccept autoAccept = Elexis.getFunctionManager().getAutoAccept();
        autoAccept.handleServerMessage(packet.content().getString());
    }
}
