package elexis.modules.impl.combat;

import elexis.Elexis;
import elexis.client.events.impl.entity.EventAttack;
import elexis.client.events.impl.entity.EventPostUpdate;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.*;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;

/** Javelin's web-specific critical sequence and one-tick timer recovery. */
@FunctionAdd(name="Criticals", alias="Criticals", category=Category.Combat, description="Critical attack in cobweb")
public final class Criticals extends Function {
    private boolean bagaTimer;

    @EventHandler public void onAttack(EventAttack event) {
        if (nullCheck() || !(event.getTarget() instanceof LivingEntity)) return;
        boolean web = false;
        for (BlockPos block : BlockPos.iterate(BlockPos.ofFloored(mc.player.getBoundingBox().minX, mc.player.getBoundingBox().minY, mc.player.getBoundingBox().minZ),
                BlockPos.ofFloored(mc.player.getBoundingBox().maxX, mc.player.getBoundingBox().maxY, mc.player.getBoundingBox().maxZ))) {
            if (mc.world.getBlockState(block).isOf(Blocks.COBWEB)) { web = true; break; }
        }
        if (!web) return;
        int groundY = mc.world.getBottomY() - 1;
        for (int y = mc.player.getBlockY(); y >= mc.world.getBottomY(); y--) {
            BlockPos block = new BlockPos(mc.player.getBlockX(), y, mc.player.getBlockZ());
            if (mc.world.getBlockState(block).isSolidBlock(mc.world, block)) { groundY = y; break; }
        }
        if (groundY != mc.player.getY()) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX(), mc.player.getY() - .03, mc.player.getZ(), false, false));
            bagaTimer = true;
        }
    }

    @EventHandler
    public void onPostUpdate(EventPostUpdate event) {
        if (mc.player == null || HitAura.target == null) {
            return;
        }
        if (!mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) || mc.player.isOnGround()) {
            return;
        }
        mc.player.fallDistance = 0.001F;
    }

    public boolean isSlowFallCrit() {
        return isState() && mc.player != null
                && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                && !mc.player.isOnGround()
                && mc.player.getVelocity().y < 0.0;
    }

    @EventHandler public void onTick(EventUpdate.Fast event) { bagaTimer = false; }

    public static float timerMultiplier() {
        var manager = Elexis.getFunctionManager();
        if (manager == null) return 1;
        for (Function module : manager.getFunctions()) {
            if (module instanceof Criticals criticals && criticals.isState() && criticals.bagaTimer) return .5f;
        }
        return 1;
    }

    @Override public void onDisable() { bagaTimer = false; super.onDisable(); }
}
