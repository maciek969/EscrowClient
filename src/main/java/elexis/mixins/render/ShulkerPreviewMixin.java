package elexis.mixins.render;

import elexis.Elexis;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class ShulkerPreviewMixin {
    @Shadow protected Slot focusedSlot;
    @Inject(method="drawMouseoverTooltip",at=@At("HEAD"),cancellable=true)
    private void Elexis$shulkerPreview(DrawContext context,int mouseX,int mouseY,CallbackInfo ci) {
        if(Elexis.getFunctionManager()==null || focusedSlot==null || !focusedSlot.hasStack()) return;
        var module=Elexis.getFunctionManager().getShulkerView();
        if(module==null || !module.isState()) return;
        ItemStack stack=focusedSlot.getStack();
        if(!(stack.getItem() instanceof BlockItem block) || !(block.getBlock() instanceof ShulkerBoxBlock)) return;
        var container=stack.get(DataComponentTypes.CONTAINER);
        if(container==null) return;
        var client=net.minecraft.client.MinecraftClient.getInstance();
        int x=Math.max(4,Math.min(mouseX+12,client.getWindow().getScaledWidth()-170));
        int y=Math.max(4,Math.min(mouseY-12,client.getWindow().getScaledHeight()-82));
        context.fill(x-2,y-2,x+166,y+78,0xF0111520);
        context.drawText(client.textRenderer,stack.getName(),x+4,y+3,0xFFE5DCFF,false);
        var contents=DefaultedList.ofSize(27,ItemStack.EMPTY);container.copyTo(contents);
        for(int i=0;i<27;i++) {
            int sx=x+3+i%9*18,sy=y+17+i/9*18;
            context.fill(sx,sy,sx+17,sy+17,0xFF252B39);
            context.drawItem(contents.get(i),sx,sy);
            context.drawStackOverlay(client.textRenderer,contents.get(i),sx,sy);
        }
        ci.cancel();
    }
}
