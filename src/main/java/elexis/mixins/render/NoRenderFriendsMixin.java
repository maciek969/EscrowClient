package elexis.mixins.render;

import elexis.Elexis;
import elexis.modules.impl.render.NoRneder;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class NoRenderFriendsMixin {

    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("TAIL"), require = 0
    )
    private void noRender$friends(PlayerLikeEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (!NoRneder.is("Друзья")) return;
        if (Elexis.getInstance() == null || Elexis.getInstance().getFriendStorage() == null) return;
        if (Elexis.getInstance().getFriendStorage().isFriend(player.getName().getString())) {
            state.invisible = true;
        }
    }
}
