package elexis.modules.impl.combat;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.*;
import elexis.modules.api.settings.impl.SliderSetting;
import elexis.modules.impl.combat.javelin.ExplosionDamage;
import elexis.modules.impl.combat.javelin.JavelinCombat;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

/** Covers crystal bases that an opponent can use to cause unacceptable self damage. */
@FunctionAdd(name="AntiCrystal", alias="AntiCrystal", category=Category.Combat, description="Перекрывает опасные места установки кристаллов")
public final class AntiCrystal extends Function {
    public final SliderSetting minDamage = new SliderSetting("Мин дамаг по себе", 8, 1, 20, .5f);
    private final List<JavelinCombat.FadingBox> visuals = new ArrayList<>();
    private PlayerEntity target;
    private JavelinCombat.Placement lastData;

    public AntiCrystal() { addSettings(minDamage); }

    @EventHandler public void onTick(EventUpdate.Fast event) {
        lastData = null;
        if (!JavelinCombat.ready()) { target = null; return; }
        AutoTrap trap = JavelinCombat.module("AutoTrap", AutoTrap.class);
        AutoWeb web = JavelinCombat.module("AutoWeb", AutoWeb.class);
        if (trap != null && trap.getRotation() != null || web != null && web.getRotation() != null) return;
        int slot = JavelinCombat.find(stack -> stack.getItem() instanceof BlockItem block
                && block.getBlock() != Blocks.COBWEB && block.getBlock().getDefaultState().isSolid(), 0, 9);
        if (slot == -1) return;
        target = JavelinCombat.target(null, 14, false);
        if (target == null) return;
        double bestDamage = minDamage.get();
        for (BlockPos base : JavelinCombat.nearby(5)) {
            if (base.getY() >= mc.player.getY() - .5) continue;
            var state = mc.world.getBlockState(base);
            if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) continue;
            BlockPos cover = base.up();
            if (!mc.world.getBlockState(cover).isAir() || JavelinCombat.blocked(cover, true)) continue;
            JavelinCombat.Placement face = JavelinCombat.visibleFace(base, Direction.UP);
            if (face == null || target.getEyePos().squaredDistanceTo(face.point()) > 36) continue;
            var enemyRay = mc.world.raycast(new RaycastContext(target.getEyePos(), face.point().add(0,-.001,0),
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, target));
            if (enemyRay.getType() != HitResult.Type.BLOCK || !enemyRay.getBlockPos().equals(base)) continue;
            Vec3d explosion = Vec3d.ofBottomCenter(cover);
            double damage = ExplosionDamage.crystal(explosion, mc.player);
            if (damage < bestDamage) continue;
            if (ExplosionDamage.damage(explosion, mc.player, mc.player.getEntityPos(), 12, List.of(), List.of(cover)) > minDamage.get()) continue;
            lastData = face;
            bestDamage = damage;
        }
        if (lastData != null && JavelinCombat.place(lastData, slot)) {
            visuals.add(new JavelinCombat.FadingBox(lastData.destination(), 0xFF33CC33, System.nanoTime()));
        }
    }

    public JavelinCombat.Placement getLastData() { return isState() ? lastData : null; }

    @EventHandler public void onRender(EventRender.World event) {
        if (!nullCheck()) JavelinCombat.renderBoxes(event, visuals);
    }

    @Override public void onDisable() {
        target = null; lastData = null; visuals.clear(); super.onDisable();
    }
}
