package elexis.modules.impl.render.hud;

import elexis.client.utils.client.IMinecraft;
import elexis.client.utils.render.main.newrender.core.Renderer2D;
import elexis.modules.impl.render.Interface;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/** Reuses the vanilla item atlas for the late HUD pass and respects resource packs. */
public final class HudItemIcons implements IMinecraft {
    private static final int EXTRACT_Y = -1024;
    private static final Icon[] ICONS = new Icon[40];
    private static final Map<ItemStack,Integer> INDICES = new IdentityHashMap<>();
    private record Icon(int texture,float u0,float v0,float u1,float v1) {}
    private HudItemIcons() {}

    public static void prepare(DrawContext context) {
        Arrays.fill(ICONS,null);
        INDICES.clear();
        Interface hud=Interface.active();
        if (hud==null || mc.options.hudHidden || mc.inGameHud.getDebugHud().shouldShowDebugHud()) return;
        hud.collectItems(stack -> {
            if (stack.isEmpty() || INDICES.containsKey(stack) || INDICES.size() >= ICONS.length) return;
            int index=INDICES.size();
            INDICES.put(stack,index);
            context.drawItem(stack,-1024-index*20,EXTRACT_Y,5700+index);
        });
    }

    public static void capture(int x,int y,int texture,float u,float v,int pixels,int atlasSize) {
        if (y!=EXTRACT_Y || atlasSize<=0) return;
        int index=(-1024-x)/20;
        if (index<0 || index>=INDICES.size() || x!=-1024-index*20) return;
        float span=(float)pixels/atlasSize;
        ICONS[index]=new Icon(texture,u,v,u+span,v-span);
    }

    public static void draw(Renderer2D renderer,ItemStack stack,float x,float y,float size) {
        Integer index=INDICES.get(stack);
        if (index==null) return;
        Icon icon=ICONS[index];
        if (icon!=null) renderer.drawRgbaTextureRegion(icon.texture,x,y,size,size,icon.u0,icon.v0,icon.u1,icon.v1,0xFFFFFFFF);
    }

    public static int cachedCount() {
        int count=0;
        for (Icon icon:ICONS) if (icon!=null) count++;
        return count;
    }
}
