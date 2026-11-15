package elexis.modules.impl.combat;

import elexis.Elexis;
import elexis.client.events.impl.entity.EventBlockInteract;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.player.ServerUtil;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

@FunctionAdd(name = "AutoExplosion", alias = "AutoExplosion", category = Category.Combat, description = "Автоматически ставит и взрывает кристалл после обсидиана")
public class AutoExplosion extends Function {
    private final BooleanSetting safeYourSelf = new BooleanSetting("Не взрывать себя", true);
    private final BooleanSetting safeFriend = new BooleanSetting("Не взрывать друзей", true);
    private final SliderSetting radius = new SliderSetting("Радиус", 5.0f, 1.0f, 10.0f, 0.1f);
    private final BooleanSetting onlyPvP = new BooleanSetting("Только в PvP", false);

    private BlockPos obsidianPos;
    private int ticksAction;
    private int crystalSlot = -1;
    private int previousSlot = -1;

    public AutoExplosion() {
        addSettings(safeYourSelf, safeFriend, radius, onlyPvP);
    }

    @EventHandler
    public void onBlockInteract(EventBlockInteract event) {
        if (nullCheck() || obsidianPos != null) return;
        var crystalAura = Elexis.getFunctionManager().getCrystalAura();
        if (crystalAura != null && crystalAura.isPlacing()) return;
        if (onlyPvP.get() && !ServerUtil.isPvp()) return;
        if (!event.getStack().isOf(Items.OBSIDIAN)) return;

        int slot = getSlotForCrystal();
        if (slot == -1) return;

        obsidianPos = event.getPlacePos();
        crystalSlot = slot;
        previousSlot = mc.player.getInventory().getSelectedSlot();
        ticksAction = 0;
    }

    @EventHandler
    public void onUpdate(EventUpdate.Fast event) {
        if (nullCheck()) {
            resetAction();
            return;
        }
        if (obsidianPos == null) return;

        switch (ticksAction) {
            case 0 -> selectSlot(crystalSlot);
            case 1 -> placeCrystal(obsidianPos);
            case 2 -> selectSlot(previousSlot);
            case 3 -> {
                BlockPos crystalPos = obsidianPos.up();
                if (canExplode(crystalPos)) attackCrystal(crystalPos);
                resetAction();
                return;
            }
        }
        ticksAction++;
    }

    private void placeCrystal(BlockPos obsidian) {
        if (mc.interactionManager == null) return;

        Vec3d hitLoc = new Vec3d(obsidian.getX() + 0.5, obsidian.getY() + 1.0, obsidian.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitLoc, Direction.UP, obsidian, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void attackCrystal(BlockPos crystalPos) {
        if (mc.interactionManager == null) return;

        List<EndCrystalEntity> crystals = mc.world.getEntitiesByClass(
                EndCrystalEntity.class,
                new Box(crystalPos).expand(0.6),
                crystal -> crystal.isAlive() && crystal.getBlockPos().equals(crystalPos)
        );
        for (EndCrystalEntity crystal : crystals) {
            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.swingHand(Hand.MAIN_HAND);
            break;
        }
    }

    private boolean canExplode(BlockPos crystalPos) {
        if (safeYourSelf.get() && mc.player.getY() >= crystalPos.getY()) return false;
        return !safeFriend.get() || !isFriendNearby(crystalPos);
    }

    private boolean isFriendNearby(BlockPos position) {
        if (Elexis.getInstance() == null || Elexis.getInstance().getFriendStorage() == null) return false;

        double range = radius.get();
        Box area = new Box(
                position.getX() - range, position.getY() - range, position.getZ() - range,
                position.getX() + range, position.getY() + range, position.getZ() + range
        );
        for (Entity entity : mc.world.getEntitiesByClass(Entity.class, area, entity -> entity instanceof PlayerEntity)) {
            if (entity instanceof PlayerEntity player && Elexis.getInstance().getFriendStorage().isFriend(player.getName().getString())) {
                return true;
            }
        }
        return false;
    }

    private int getSlotForCrystal() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.END_CRYSTAL)) return i;
        }
        return -1;
    }

    private void selectSlot(int slot) {
        if (slot < 0 || slot > 8 || mc.getNetworkHandler() == null) return;
        mc.player.getInventory().setSelectedSlot(slot);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private void resetAction() {
        obsidianPos = null;
        ticksAction = 0;
        crystalSlot = -1;
        previousSlot = -1;
    }

    public boolean isPlacingCrystal() {
        return isState() && obsidianPos != null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetAction();
    }
}
