package elexis.client.events.impl.client;

import elexis.client.utils.client.IMinecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;

public record EventKey(Screen screen, InputUtil.Type type, int key, int action) implements IMinecraft {
    public boolean isKeyDown(int key) {
        return isKeyDown(key, mc.currentScreen == null);
    }

    public boolean isKeyDown(int key, boolean screen) {
        return matchesBind(key) && action == 1 && screen;
    }

    public boolean isKeyReleased(int key) {
        return isKeyReleased(key, mc.currentScreen == null);
    }

    public boolean isKeyReleased(int key, boolean screen) {
        return matchesBind(key) && action == 0 && screen;
    }

    public boolean matchesBind(int bind) {
        if (bind == 0 || bind == -1) {
            return false;
        }
        if (type == InputUtil.Type.MOUSE) {
            return bind < 0 && key == -(bind + 1);
        }
        return bind > 0 && type == InputUtil.Type.KEYSYM && key == bind;
    }

    public int getKey() {
        return this.key;
    }
    public int getAction() {
        return this.action;
    }
}
