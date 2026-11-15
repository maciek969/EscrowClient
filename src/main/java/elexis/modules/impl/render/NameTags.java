package elexis.modules.impl.render;

import elexis.Elexis;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.math.Mathf;
import elexis.client.utils.render.main.newrender.core.Renderer2D;
import elexis.client.utils.render.main.newrender.text.FontRegistry;
import elexis.modules.impl.render.javelin.target.NameTagText;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.MultiBooleanSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.GlTextureView;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Javelin name plates with native, resource-pack-aware item atlas rendering. */
@FunctionAdd(name="NameTags", alias="NameTags", category=Category.Render,
        description="Теги Javelin с головами, здоровьем и экипировкой")
public final class NameTags extends Function {
    private static final float ITEM_SIZE=13, ITEM_GAP=2, TEXT_SIZE=14;
    private static final int BACKGROUND=0x73000000, FRIEND_BACKGROUND=0x734BE38C;
    private static final int ICON_Y=-1536, MAX_ICONS=512;
    private static final EquipmentSlot[] EQUIPMENT={EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static NameTags instance;
    public final MultiBooleanSetting targets=new MultiBooleanSetting("Цели",
            new BooleanSetting("Игроки",true),new BooleanSetting("Предметы",true));
    public final BooleanSetting playerHead=new BooleanSetting("Голова игрока",true);
    public final BooleanSetting itemIcons=new BooleanSetting("Иконки предметов",true);
    private final List<Entry> entries=new ArrayList<>();
    private final List<Icon> icons=new ArrayList<>();
    private final Map<ItemStack,Integer> iconIndices=new IdentityHashMap<>();
    private final Map<String,Text> serverNames=new HashMap<>();
    private ClientWorld world;
    private float tickDelta=1;

    private record Entry(float x,float y,Text label,List<ItemStack> equipment,boolean friend,Identifier skin,ItemStack nameItem) {}
    private record Icon(int texture,float u0,float v0,float u1,float v1) {}
    private record Run(String text,int color) {}

    public NameTags() {
        instance=this;
        addSettings(targets,playerHead,itemIcons);
    }

    public boolean shouldHideVanillaPlayerLabels() {
        return isState() && targets.get("Игроки");
    }

    public void rememberVanillaPlayerLabel(Text playerName,Text displayName) {
        if(playerName!=null && displayName!=null) serverNames.put(playerName.getString(),displayName.copy());
    }

    @EventHandler public void world(EventRender.World event) { tickDelta=event.getTicks(); }

    /** Called during native GUI preparation, using the same extraction path as HudItemIcons. */
    public static void prepareIcons(DrawContext context) {
        if(instance!=null) instance.prepare(context);
    }

    private void prepare(DrawContext context) {
        entries.clear();
        icons.clear();
        iconIndices.clear();
        if(!isState() || nullCheck() || mc.options.hudHidden) return;
        if(world!=mc.world) { serverNames.clear(); world=mc.world; }
        double pixelScale=mc.getWindow().getScaleFactor();
        for(Entity entity:mc.world.getEntities()) {
            if(entity instanceof PlayerEntity player && targets.get("Игроки")) {
                if(player==mc.player && mc.options.getPerspective().isFirstPerson()) continue;
                Vec3d position=project(player.getLerpedPos(tickDelta).add(0,player.getHeight()+0.4,0));
                if(position==null) continue;
                Text name=serverNames.getOrDefault(player.getName().getString(),
                        player.hasCustomName() ? player.getCustomName() : player.getDisplayName());
                if(name==null) name=player.getName();
                name=NameTagText.normalize(elexis.modules.impl.misc.NameProtect.display(name)).copy()
                        .append(" §7[§c"+Math.round(player.getHealth()+player.getAbsorptionAmount())+"§7]");
                List<ItemStack> equipment=new ArrayList<>(6);
                if(itemIcons.get()) for(EquipmentSlot slot:EQUIPMENT) {
                    ItemStack stack=player.getEquippedStack(slot);
                    if(!stack.isEmpty()) { equipment.add(stack); queue(context,stack); }
                }
                Identifier skin=playerHead.get() && player instanceof AbstractClientPlayerEntity client
                        ? client.getSkin().body().texturePath() : null;
                boolean friend=player!=mc.player && Elexis.getInstance().getFriendStorage().isFriend(player.getGameProfile().name());
                entries.add(new Entry((float)(position.x*pixelScale),(float)(position.y*pixelScale),name,equipment,friend,skin,ItemStack.EMPTY));
            } else if(entity instanceof ItemEntity item && targets.get("Предметы")) {
                double offset=mc.player.getEyePos().distanceTo(item.getBoundingBox().getCenter())/20;
                Vec3d position=project(item.getLerpedPos(tickDelta).add(0,item.getHeight()+offset,0));
                if(position==null) continue;
                ItemStack stack=item.getStack();
                Text label=item.hasCustomName() ? item.getCustomName() : stack.getName();
                label=NameTagText.normalize(label);
                if(stack.getCount()>1) label=label.copy().append(" §7x"+stack.getCount());
                ItemStack nameItem=itemIcons.get() ? stack : ItemStack.EMPTY;
                if(!nameItem.isEmpty()) queue(context,nameItem);
                entries.add(new Entry((float)(position.x*pixelScale),(float)(position.y*pixelScale),label,List.of(),false,null,nameItem));
            }
        }
    }

    private Vec3d project(Vec3d position) {
        Vec3d projected=Mathf.worldSpaceToScreenSpace(position);
        return projected==null || projected.z<0 || projected.z>=1 || !Double.isFinite(projected.x+projected.y) ? null : projected;
    }

    private void queue(DrawContext context,ItemStack stack) {
        if(iconIndices.containsKey(stack) || icons.size()>=MAX_ICONS) return;
        int index=icons.size();
        iconIndices.put(stack,index);
        icons.add(null);
        context.drawItem(stack,-4096-index*20,ICON_Y,7500+index);
    }

    /** Captured by GuiRenderer after vanilla has resolved the complete GUI item model. */
    public static void captureIcon(int x,int y,int texture,float u,float v,int pixels,int atlasSize) {
        NameTags tags=instance;
        if(tags==null || y!=ICON_Y || atlasSize<=0) return;
        int index=(-4096-x)/20;
        if(index<0 || index>=tags.icons.size() || x!=-4096-index*20) return;
        float span=(float)pixels/atlasSize;
        tags.icons.set(index,new Icon(texture,u,v,u+span,v-span));
    }

    @EventHandler public void render(EventRender.Screen.Hud event) {
        if(nullCheck() || mc.options.hudHidden || world!=mc.world) return;
        Renderer2D renderer=event.getRenderer();
        for(Entry entry:entries) {
            List<Run> runs=runs(entry.label);
            float textWidth=0;
            for(Run run:runs) textWidth+=renderer.measureText(FontRegistry.SF_SEMIBOLD,run.text,TEXT_SIZE).width;
            float inset=entry.skin!=null || !entry.nameItem.isEmpty() ? 16 : 0;
            float height=inset>0 ? 18 : 17;
            float width=textWidth+inset+8;
            float left=entry.x-width/2, top=entry.y-2;
            int background=entry.friend ? FRIEND_BACKGROUND : BACKGROUND;
            if(!entry.equipment.isEmpty()) {
                float itemsWidth=entry.equipment.size()*ITEM_SIZE+(entry.equipment.size()-1)*ITEM_GAP+6;
                float itemsX=entry.x-itemsWidth/2, itemsY=top-22;
                renderer.rect(itemsX,itemsY,itemsWidth,19,0,background);
                for(int i=0;i<entry.equipment.size();i++) drawItem(renderer,entry.equipment.get(i),itemsX+3+i*15,itemsY+3,true);
            }
            renderer.rect(left,top,width,height,0,background);
            if(entry.skin!=null) drawHead(renderer,entry.skin,left+4,top+(height-13)/2);
            else if(!entry.nameItem.isEmpty()) drawItem(renderer,entry.nameItem,left+4,top+(height-13)/2,false);
            float textX=left+4+inset;
            float baseline=top+height/2+FontRegistry.centeredBaselineOffset(FontRegistry.SF_SEMIBOLD,'A',TEXT_SIZE);
            for(Run run:runs) {
                renderer.text(FontRegistry.SF_SEMIBOLD,textX,baseline,TEXT_SIZE,run.text,run.color);
                textX+=renderer.measureText(FontRegistry.SF_SEMIBOLD,run.text,TEXT_SIZE).width;
            }
        }
    }

    private List<Run> runs(Text text) {
        List<Run> result=new ArrayList<>();
        StringBuilder content=new StringBuilder();
        int[] color={0xFFFFFFFF};
        text.asOrderedText().accept((index,style,codePoint)->{
            int next=style.getColor()==null ? 0xFFFFFFFF : 0xFF000000|style.getColor().getRgb();
            if(next!=color[0] && !content.isEmpty()) { result.add(new Run(content.toString(),color[0])); content.setLength(0); }
            color[0]=next;
            content.appendCodePoint(codePoint);
            return true;
        });
        if(!content.isEmpty()) result.add(new Run(content.toString(),color[0]));
        return result;
    }

    private void drawItem(Renderer2D renderer,ItemStack stack,float x,float y,boolean count) {
        Integer index=iconIndices.get(stack);
        Icon icon=index==null ? null : icons.get(index);
        if(icon!=null) renderer.drawRgbaTextureRegion(icon.texture,x,y,13,13,icon.u0,icon.v0,icon.u1,icon.v1,0xFFFFFFFF);
        if(count && stack.getCount()>1) {
            String label=Integer.toString(stack.getCount());
            float width=renderer.measureText(FontRegistry.SF_SEMIBOLD,label,9).width;
            renderer.text(FontRegistry.SF_SEMIBOLD,x+14-width,y+16,9,label,0xFFFFFFFF);
        }
    }

    private void drawHead(Renderer2D renderer,Identifier skin,float x,float y) {
        if(!(mc.getTextureManager().getTexture(skin).getGlTextureView() instanceof GlTextureView texture)) return;
        int id=texture.texture().getGlId();
        renderer.drawRgbaTextureRegionRounded(id,x,y,13,13,0.125f,0.125f,0.25f,0.25f,4.55f,0xFFFFFFFF);
        renderer.drawRgbaTextureRegionRounded(id,x,y,13,13,0.625f,0.125f,0.75f,0.25f,4.55f,0xFFFFFFFF);
    }

    @Override public void onDisable() {
        entries.clear(); icons.clear(); iconIndices.clear(); serverNames.clear(); world=null;
        super.onDisable();
    }
}
