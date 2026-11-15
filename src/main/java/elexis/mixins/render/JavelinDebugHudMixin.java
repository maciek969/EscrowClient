package elexis.mixins.render;

import elexis.modules.impl.misc.NameProtect;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(DebugHud.class)
public abstract class JavelinDebugHudMixin {
    @ModifyVariable(method = "drawText", at = @At("HEAD"), argsOnly = true)
    private List<String> elexis$protectDebug(List<String> lines) {
        return NameProtect.debugLines(lines);
    }
}
