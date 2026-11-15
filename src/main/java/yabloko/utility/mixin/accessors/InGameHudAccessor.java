package yabloko.utility.mixin.accessors;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({InGameHud.class})
public interface InGameHudAccessor {
   @Accessor("heldItemTooltipFade")
   int getHeldItemTooltipFade();

   @Accessor("overlayMessage")
   Text getOverlayMessage();

   @Accessor("overlayRemaining")
   int getOverlayRemaining();

   @Accessor("overlayTinted")
   boolean isOverlayTinted();

   @Accessor("currentStack")
   ItemStack getCurrentStack();

   @Invoker("renderStatusBars")
   void invokeRenderStatusBars(DrawContext var1);
}
