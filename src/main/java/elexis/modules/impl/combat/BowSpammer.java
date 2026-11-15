package elexis.modules.impl.combat;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@FunctionAdd(name = "BowSpammer", alias = "BowSpammer", category = Category.Combat, description = "Fast bow spam")
@Environment(EnvType.CLIENT)
public class BowSpammer extends Function {
    private final SliderSetting chargeTicks = new SliderSetting("Charge Ticks", 4.0F, 1.0F, 20.0F, 1.0F);
    private int ticks;

    public BowSpammer() {
        this.addSettings(this.chargeTicks);
    }

    @Override
    public void onDisable() {
        this.ticks = 0;
        if (mc.player != null && mc.player.isUsingItem() && mc.player.getActiveItem().isOf(Items.BOW)) {
            mc.player.stopUsingItem();
        }
        super.onDisable();
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck() || mc.interactionManager == null) {
            return;
        }

        Hand hand = getBowHand();
        if (hand == null) {
            this.ticks = 0;
            return;
        }

        if (!mc.player.isUsingItem() || !mc.player.getActiveItem().isOf(Items.BOW)) {
            mc.interactionManager.interactItem(mc.player, hand);
            mc.player.setCurrentHand(hand);
            this.ticks = 0;
            return;
        }

        if (mc.player.getActiveHand() != hand) {
            this.ticks = 0;
            return;
        }

        this.ticks++;
        if (this.ticks < Math.round(this.chargeTicks.get())) {
            return;
        }

        sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
        mc.player.stopUsingItem();
        this.ticks = 0;

        mc.interactionManager.interactItem(mc.player, hand);
        mc.player.setCurrentHand(hand);
    }

    private Hand getBowHand() {
        if (mc.player.getMainHandStack().isOf(Items.BOW)) {
            return Hand.MAIN_HAND;
        }
        if (mc.player.getOffHandStack().isOf(Items.BOW)) {
            return Hand.OFF_HAND;
        }
        return null;
    }
}