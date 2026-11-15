package elexis.mixins.render;

import elexis.Elexis;
import elexis.modules.impl.utils.BetterGameplay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.ScrollableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntryListWidget.class)
public class EntryListWidgetMixin {
    @Inject(
            method = "renderWidget",
            at = @At("HEAD")
    )
    public void manipulateScrollAmount(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!BetterGameplay.list.isSelected("Скролл") && !Elexis.getFunctionManager().getBetterGameplay().isState()) return;

        ((ScrollableWidget) (Object) this).Elexis$manipulateScrollAmount(delta);
    }
}