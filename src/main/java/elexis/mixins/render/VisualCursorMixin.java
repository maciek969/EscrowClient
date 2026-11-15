package elexis.mixins.render;

import elexis.modules.impl.render.visual.OverlayVisuals;
import elexis.modules.impl.render.visual.SceneVisuals;
import net.minecraft.client.gui.cursor.Cursor;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Minecraft changes native cursor shapes during GUI rendering; keep the user's replacement. */
@Mixin(Cursor.class)
public class VisualCursorMixin {
    @Inject(method="applyTo",at=@At("HEAD"),cancellable=true)
    private void elexis$replaceCursor(Window window,CallbackInfo ci) {
        var module=SceneVisuals.active("Cursor",OverlayVisuals.Cursor.class);
        if(module!=null) {module.refreshNativeCursor();module.vanillaOverrides++;ci.cancel();}
    }
}
