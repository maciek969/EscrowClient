package elexis.modules.impl.movement;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.entity.SlowWalkingEvent;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.player.PlayerInventoryUtil;
import elexis.client.utils.player.PlayerUtils;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

@FunctionAdd(name = "NoSlow", alias = "NoSlow", category = Category.Movement, description = "Убирает замедление при использовании предметов")
@Environment(EnvType.CLIENT)
public class NoSlow extends Function {
    private final ModeSetting mode = new ModeSetting("Режим", "Новый Грим", "Новый Грим", "Старый Грим", "Грим", "Грим тики");
    private final BooleanSetting ignoreGaps = new BooleanSetting("Не работать на геплы и чарки", false)
            .hidden(() -> !mode.is("Грим") && !mode.is("Грим тики"));
    private float ticks;

    public NoSlow() {
        addSettings(mode, ignoreGaps);
    }

    @EventHandler
    public void onSlowWalking(SlowWalkingEvent event) {
        if (shouldCancelSlowdown()) {
            event.cancel();
        }
    }

    @EventHandler
    public void onUpdate(EventUpdate.Fast event) {
        if (nullCheck()) {
            ticks = 0;
            return;
        }

        if (mc.player.getActiveHand() == Hand.MAIN_HAND || mc.player.getActiveHand() == Hand.OFF_HAND) {
            ticks++;
        } else {
            ticks = 0;
        }

        if (!mc.player.isUsingItem()) {
            return;
        }

        if (mode.is("Новый Грим") && mc.player.getItemUseTime() < 7) {
            PlayerInventoryUtil.updateSlots();
            PlayerInventoryUtil.closeScreen(true);
        }

        if (mode.is("Грим") && mc.player.getItemUseTime() == 4 && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.DROP_ALL_ITEMS,
                    BlockPos.ORIGIN,
                    mc.player.getHorizontalFacing()
            ));
        }
    }

    public boolean shouldCancelSlowdown() {
        if (!isState() || nullCheck() || !mc.player.isUsingItem()) {
            return false;
        }

        ItemStack activeStack = mc.player.getActiveItem();
        if ((mode.is("Грим") || mode.is("Грим тики")) && ignoreGaps.get() && isGapple(activeStack)) {
            return false;
        }

        if (mode.is("Старый Грим")) {
            Hand first = mc.player.getActiveHand();
            Hand second = first == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
            if (mc.player.getOffHandStack().getUseAction() == UseAction.NONE || mc.player.getMainHandStack().getUseAction() == UseAction.NONE) {
                PlayerUtils.interactItem(first);
                PlayerUtils.interactItem(second);
                return true;
            }
            return false;
        }

        if (mode.is("Новый Грим")) {
            return mc.player.getItemUseTime() >= 7;
        }

        if (mode.is("Грим тики")) {
            if (ticks > 4 && mc.player.getItemUseTime() > 3) {
                ticks = 1.7f;
                return true;
            }
            return false;
        }

        if (mode.is("Грим")) {
            return mc.player.getActiveHand() != Hand.OFF_HAND && mc.player.getItemUseTime() > 4;
        }

        return false;
    }

    private boolean isGapple(ItemStack stack) {
        return stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE);
    }
}
