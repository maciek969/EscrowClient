package elexis.mixins.accessors;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(InGameHud.class)
public interface InGameHudAccessor {
    @Invoker("renderStatusBars")
    void Elexis$renderStatusBars(DrawContext context);
}
