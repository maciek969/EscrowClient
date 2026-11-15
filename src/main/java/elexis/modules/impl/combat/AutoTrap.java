package elexis.modules.impl.combat;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.*;
import elexis.modules.impl.combat.javelin.JavelinCombat;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;

/** Javelin's five-tick movement trap, using an available solid hotbar block. */
@FunctionAdd(name="AutoTrap", alias="AutoTrap", category=Category.Combat, description="Перекрывает предсказанное движение цели блоком")
public final class AutoTrap extends Function {
    private PlayerEntity target;
    private JavelinCombat.Placement rotation;

    @EventHandler public void onTick(EventUpdate.Fast event) {
        rotation = null;
        if (!JavelinCombat.ready()) { target = null; return; }
        int slot = JavelinCombat.find(stack -> stack.getItem() instanceof BlockItem block
                && block.getBlock() != Blocks.COBWEB && block.getBlock().getDefaultState().isSolid(), 0, 9);
        if (slot == -1) return;
        target = JavelinCombat.target(target, 8, true);
        if (target == null) return;
        BlockPos destination = BlockPos.ofFloored(JavelinCombat.predict(target, 5));
        if (JavelinCombat.blocked(destination, false)) return;
        rotation = JavelinCombat.supportFor(destination);
        if (rotation != null) JavelinCombat.place(rotation, slot);
    }

    public JavelinCombat.Placement getRotation() { return isState() ? rotation : null; }

    @Override public void onDisable() {
        target = null;
        rotation = null;
        super.onDisable();
    }
}
