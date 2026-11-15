package elexis.modules.impl.combat;

import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.*;
import elexis.modules.impl.combat.javelin.JavelinCombat;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Javelin's four-tick cobweb prediction with movement-packet confirmation. */
@FunctionAdd(name="AutoWeb", alias="AutoWeb", category=Category.Combat, description="Places cobweb by player movement")
public final class AutoWeb extends Function {
    private final ConcurrentLinkedQueue<EntityS2CPacket> movements = new ConcurrentLinkedQueue<>();
    private final List<JavelinCombat.FadingBox> visuals = new ArrayList<>();
    private PlayerEntity target;
    private JavelinCombat.Placement rotation;
    private int confirmationTicks;

    @EventHandler public void onPacket(EventReceivePacket event) {
        if (event.getPacket() instanceof EntityS2CPacket movement) movements.add(movement);
    }

    @EventHandler public void onTick(EventUpdate.Fast event) {
        rotation = null;
        if (!JavelinCombat.ready()) { clear(); return; }
        EntityS2CPacket movement;
        while ((movement = movements.poll()) != null) {
            if (confirmationTicks > 0 && movement.getEntity(mc.world) == target) confirmationTicks--;
        }
        AutoTrap trap = JavelinCombat.module("AutoTrap", AutoTrap.class);
        if (trap != null && trap.getRotation() != null) return;
        int slot = JavelinCombat.find(Items.COBWEB, 0, 9);
        if (slot == -1) return;
        target = JavelinCombat.target(target, 12, true);
        if (target == null) return;
        BlockPos destination = BlockPos.ofFloored(JavelinCombat.predict(target, 4));
        // A web attached above another web is preferred until the server confirms movement.
        if (mc.world.getBlockState(destination.down()).isOf(Blocks.COBWEB)) {
            rotation = JavelinCombat.supportFor(destination);
        }
        if (rotation == null && confirmationTicks == 0 && !mc.world.getBlockState(destination.up()).isOf(Blocks.COBWEB)) {
            rotation = JavelinCombat.supportFor(destination);
        }
        if (rotation != null && JavelinCombat.place(rotation, slot)) {
            visuals.add(new JavelinCombat.FadingBox(rotation.destination(), 0xFFFFFFFF, System.nanoTime()));
            confirmationTicks = target.isGliding() ? 3 : 1;
        }
    }

    @EventHandler public void onRender(EventRender.World event) {
        if (!nullCheck()) JavelinCombat.renderBoxes(event, visuals);
    }

    public JavelinCombat.Placement getRotation() { return isState() ? rotation : null; }

    private void clear() {
        movements.clear(); visuals.clear(); target = null; rotation = null; confirmationTicks = 0;
    }

    @Override public void onDisable() { clear(); super.onDisable(); }
}
