package elexis.mixins.accessors;

import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChatHud.class)
public interface JavelinChatAccessor {
    @Invoker("refresh") void elexis$refresh();
}
