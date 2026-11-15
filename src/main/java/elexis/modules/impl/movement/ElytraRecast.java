package elexis.modules.impl.movement;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

@FunctionAdd(name = "ElytraRecast", alias = "Elytra Recast", category = Category.Movement, description = "Повторно включает полёт на элитрах")
public final class ElytraRecast extends Function {
    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck()
                || !mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).isOf(Items.ELYTRA)
                || mc.player.isOnGround()
                || mc.player.isGliding()) {
            return;
        }
        sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        mc.player.startGliding();
    }
}
