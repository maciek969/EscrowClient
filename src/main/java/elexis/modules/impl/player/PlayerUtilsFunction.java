package elexis.modules.impl.player;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.mixins.accessors.LEntityAccessor;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;

import java.util.concurrent.ThreadLocalRandom;

@FunctionAdd(name = "PlayerUtils", alias = "Player Utils", category = Category.Player)
public class PlayerUtilsFunction extends Function {
    public static BooleanSetting NoJumpDelay = new BooleanSetting("No Jump Delay", true);
    BooleanSetting jumpRandomization = new BooleanSetting("Jump randomization", false).setVisible(() -> NoJumpDelay.get());
    public static BooleanSetting NoPush = new BooleanSetting("No Push", true);

    public PlayerUtilsFunction() {
        addSettings(NoJumpDelay, jumpRandomization, NoPush);
    }

    @EventHandler
    public void onUpdate(EventUpdate.Slow e) {
        int delay = (jumpRandomization.get() ?
                ThreadLocalRandom.current().nextInt(2,6) : ThreadLocalRandom.current().nextInt(1,3));

        if (((LEntityAccessor) mc.player).getLastJumpCooldown() > delay) {
            ((LEntityAccessor) mc.player).setLastJumpCooldown(delay - 1);
        }
    }
}