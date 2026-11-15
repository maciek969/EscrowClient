package elexis.modules.impl.render.visual;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.*;
import elexis.modules.api.settings.impl.*;
import net.minecraft.block.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import java.util.*;

public final class BlockVisuals {
    private BlockVisuals() {}

    /** Bounded incremental scan; never requests unloaded chunks. */
    public abstract static class Scan extends Function {
        protected final SliderSetting radius=new SliderSetting("Radius",12f,4f,32f,1f);
        protected final Set<BlockPos> matches=new LinkedHashSet<>();
        private BlockPos origin;
        private ClientWorld world;
        private int index, scannedRadius;
        protected Scan() { addSettings(radius); }
        protected abstract boolean matches(BlockPos p, BlockState state);
        protected int color(BlockPos p) { return 0xFFA99EFF; }
        @EventHandler public void update(EventUpdate.Slow event) {
            if(nullCheck()) { clear(); return; }
            int r=radius.get().intValue();
            if(world!=mc.world || origin==null || origin.getSquaredDistance(mc.player.getBlockPos())>16 || r!=scannedRadius) {
                clear(); world=mc.world; origin=mc.player.getBlockPos(); scannedRadius=r;
            }
            matches.removeIf(p->!loaded(p) || !matches(p,mc.world.getBlockState(p)) || !p.isWithinDistance(mc.player.getBlockPos(),r+2));
            int edge=r*2+1,total=edge*edge*edge;
            for(int n=0;n<2048;n++) {
                int i=index++ % total;
                BlockPos p=origin.add(i%edge-r,(i/edge)%edge-r,i/(edge*edge)-r);
                if(loaded(p) && matches(p,mc.world.getBlockState(p)) && matches.size()<512) matches.add(p);
                if(index>=total) { index=0; break; }
            }
        }
        private boolean loaded(BlockPos p) { return mc.world.isChunkLoaded(p.getX()>>4,p.getZ()>>4); }
        @EventHandler public void render(EventRender.World event) {
            if(nullCheck() || world!=mc.world || matches.isEmpty()) return;
            try(var frame=new VisualGeometry.Frame(event)) { for(BlockPos p:matches) frame.box(new Box(p).expand(0.002),color(p)); }
        }
        protected void clear() { matches.clear(); origin=null; world=null; index=0; }
        @Override public void onDisable() { clear(); super.onDisable(); }
    }

    @FunctionAdd(name="BlockESP",alias="Block ESP",category=Category.Render,description="Highlight of containers, ores and spawners")
    public static final class BlockESP extends Scan {
        private final ModeSetting blocks=new ModeSetting("Blocks","Containers","Containers","Ores","Spawners","All");
        public BlockESP() { addSettings(blocks); }
        @Override protected boolean matches(BlockPos p,BlockState state) {
            Block b=state.getBlock();
            String name=Registries.BLOCK.getId(b).getPath();
            boolean container=b instanceof ChestBlock || b instanceof EnderChestBlock || b instanceof ShulkerBoxBlock || b instanceof BarrelBlock;
            boolean ore=name.endsWith("_ore") || state.isOf(Blocks.ANCIENT_DEBRIS);
            return blocks.is("Containers") && container || blocks.is("Ores") && ore || blocks.is("Spawners") && b instanceof SpawnerBlock
                    || blocks.is("All") && (container || ore || b instanceof SpawnerBlock);
        }
    }

    @FunctionAdd(name="HoleESP",alias="Hole ESP",category=Category.Render,description="Safe holes from obsidian and bedrock")
    public static final class HoleESP extends Scan {
        @Override protected boolean matches(BlockPos p,BlockState state) {
            if(!state.isAir() || !mc.world.getBlockState(p.up()).isAir() || !mc.world.getBlockState(p.up(2)).isAir()) return false;
            for(Direction d:new Direction[]{Direction.DOWN,Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}) {
                BlockState wall=mc.world.getBlockState(p.offset(d));
                if(!wall.isOf(Blocks.BEDROCK) && !wall.isOf(Blocks.OBSIDIAN) && !wall.isOf(Blocks.CRYING_OBSIDIAN)) return false;
            }
            return true;
        }
        @Override protected int color(BlockPos p) { return mc.world.getBlockState(p.down()).isOf(Blocks.BEDROCK)?0xFF79EBC4:0xFFFFD179; }
    }

    @FunctionAdd(name="TrapIndicator",alias="Trap Indicator",category=Category.Render,description="Markers of nearby pistons and mechanisms")
    public static final class TrapIndicator extends Scan {
        @Override protected boolean matches(BlockPos p,BlockState state) {
            Block b=state.getBlock();
            return b instanceof PistonBlock || b instanceof PistonHeadBlock || state.isOf(Blocks.MOVING_PISTON);
        }
        @Override protected int color(BlockPos p) { return 0xFFFF8B75; }
        @EventHandler public void labels(EventRender.Screen.Hud event) {
            if(nullCheck()) return;
            int count=0;
            for(BlockPos p:matches) {
                VisualGeometry.label(event,Vec3d.ofCenter(p).add(0,0.8,0),String.format(Locale.US,"Trap %.0f m",Math.sqrt(p.getSquaredDistance(mc.player.getBlockPos()))),0xFFFF8B75);
                if(++count>=24) break;
            }
        }
    }

    @FunctionAdd(name="BlockOverlay",alias="Block Overlay",category=Category.Render,description="Highlight of the block under the crosshair")
    public static final class BlockOverlay extends Function {
        private final ColorSetting color=new ColorSetting("Color",0xFFB29CFF);
        public BlockOverlay() { addSettings(color); }
        @EventHandler public void render(EventRender.World event) {
            if(nullCheck() || !(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType()!=HitResult.Type.BLOCK) return;
            var shape=mc.world.getBlockState(hit.getBlockPos()).getOutlineShape(mc.world,hit.getBlockPos());
            if(shape.isEmpty()) return;
            try(var frame=new VisualGeometry.Frame(event)) {
                for(Box box:shape.getBoundingBoxes()) frame.box(box.offset(hit.getBlockPos()).expand(0.002),color.get());
            }
        }
    }
}
