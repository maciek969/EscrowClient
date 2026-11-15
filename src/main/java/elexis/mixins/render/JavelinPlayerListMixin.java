package elexis.mixins.render;

import elexis.modules.impl.misc.NameProtect;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

import static elexis.client.utils.client.IMinecraft.mc;

@Mixin(PlayerListHud.class)
public abstract class JavelinPlayerListMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void elexis$protectTab(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        var text = cir.getReturnValue();
        var protectedText = NameProtect.display(text);
        if (text != protectedText) cir.setReturnValue(protectedText);
    }

    @Inject(method = "collectPlayerEntries", at = @At("RETURN"), cancellable = true)
    private void elexis$tabOrder(CallbackInfoReturnable<List<PlayerListEntry>> cir) {
        var protection = NameProtect.active();
        if (protection == null || protection.tabPosition.is("Off")) return;
        var entries = new ArrayList<>(cir.getReturnValue());
        var own = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        if (own == null || !entries.remove(own)) return;
        if (protection.tabPosition.is("Top")) entries.addFirst(own);
        else entries.addLast(own);
        cir.setReturnValue(entries);
    }
}
