package elexis.mixins.render;

import elexis.modules.impl.misc.NameProtect;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatHud.class)
public abstract class JavelinChatMixin {
    @ModifyVariable(method = "addVisibleMessage", at = @At("HEAD"), argsOnly = true)
    private ChatHudLine elexis$protectChat(ChatHudLine line) {
        var safe = NameProtect.display(line.content());
        return safe == line.content() ? line : new ChatHudLine(line.creationTick(), safe, line.signature(), line.indicator());
    }
}
