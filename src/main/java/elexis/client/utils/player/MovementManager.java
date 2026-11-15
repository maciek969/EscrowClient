package elexis.client.utils.player;

import elexis.client.utils.client.IMinecraft;
import elexis.modules.impl.combat.HitAura;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.util.HashSet;
import java.util.Set;

public class MovementManager implements IMinecraft {
    private static final MovementManager INSTANCE = new MovementManager();
    private final Set<String> lockRequests = new HashSet<>();

    public static MovementManager getInstance() {
        return INSTANCE;
    }

    public void lockMovement(String moduleName) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        lockRequests.add(moduleName);
        HitAura.canSwap = true;
        setMovementKeys(false);
    }

    public void unlockMovement(String moduleName) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        lockRequests.remove(moduleName);
        if (lockRequests.isEmpty() && mc.currentScreen == null) {
            setMovementKeys(true);
            HitAura.canSwap = false;
        }
    }

    private void setMovementKeys(boolean state) {
        var window = mc.getWindow();
        KeyBinding[] movementKeys = new KeyBinding[]{
                mc.options.forwardKey,
                mc.options.backKey,
                mc.options.leftKey,
                mc.options.rightKey,
                mc.options.jumpKey,
                mc.options.sprintKey
        };

        for (KeyBinding keyBinding : movementKeys) {
            boolean pressed = state && InputUtil.isKeyPressed(window, keyBinding.getDefaultKey().getCode());
            keyBinding.setPressed(pressed);
        }
    }
}
