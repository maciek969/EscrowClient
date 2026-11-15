package elexis.modules.impl.render.visual;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.*;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import java.util.*;
import java.util.regex.Pattern;

@FunctionAdd(name="WardenESP",alias="Warden ESP",category=Category.Render,description="Warden city chests and server respawn timers")
public final class WardenESP extends Function {
    private static final Pattern TIME=Pattern.compile("(\\d{2}):(\\d{2})");
    private final Set<BlockPos> chests=new HashSet<>();
    private final Map<BlockPos,Long> timers=new HashMap<>();
    private ClientWorld world;
    private int tick;
    @EventHandler public void update(EventUpdate.Slow event) {
        if(world!=mc.world) { clear(); world=mc.world; }
        if(nullCheck() || !mc.world.getRegistryKey().equals(World.OVERWORLD)) { clear(); return; }
        if(tick++%20==0) {
            chests.clear();
            // Original Visual module is specific to this server arena. Only inspect loaded chunks.
            for(int x=-130;x<=-121;x++) for(int z=-130;z<=-121;z++) {
                if(!mc.world.isChunkLoaded(x,z)) continue;
                for(var block:mc.world.getChunk(x,z).getBlockEntities().values()) {
                    BlockPos p=block.getPos();
                    if(block instanceof ChestBlockEntity && p.getX()>=-2070 && p.getX()<=-1921
                            && p.getZ()>=-2076 && p.getZ()<=-1929 && p.getY()>=-60 && p.getY()<=-35) chests.add(p.toImmutable());
                }
            }
        }
        long now=System.nanoTime();
        for(var e:mc.world.getEntities()) if(e instanceof ArmorStandEntity stand && stand.hasCustomName()) {
            var matcher=TIME.matcher(stand.getCustomName().getString());
            if(!matcher.find()) continue;
            long seconds=Integer.parseInt(matcher.group(1))*60L+Integer.parseInt(matcher.group(2));
            for(BlockPos p:chests) if(p.getX()==stand.getBlockX() && p.getZ()==stand.getBlockZ()) timers.put(p,now+seconds*1_000_000_000L);
        }
        timers.entrySet().removeIf(entry->entry.getValue()<now || !chests.contains(entry.getKey()));
    }
    @EventHandler public void render(EventRender.World event) {
        if(nullCheck() || world!=mc.world || chests.isEmpty()) return;
        try(var frame=new VisualGeometry.Frame(event)) { for(BlockPos p:chests) frame.box(new Box(p),timers.containsKey(p)?0xFFFFC685:0xFF96E7CD); }
    }
    @EventHandler public void labels(EventRender.Screen.Hud event) {
        if(nullCheck() || world!=mc.world) return;
        long now=System.nanoTime();
        for(BlockPos p:chests) {
            Long until=timers.get(p);
            long seconds=until==null?0:Math.max(0,(until-now)/1_000_000_000L);
            VisualGeometry.label(event,Vec3d.ofCenter(p).add(0,0.8,0),until==null?"Chest":String.format(Locale.US,"%02d:%02d",seconds/60,seconds%60),0xFFFFDFAC);
        }
    }
    private void clear() { chests.clear();timers.clear();world=null;tick=0; }
    @Override public void onDisable() { clear();super.onDisable(); }
}
