package elexis.modules.impl.misc;

import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.api.Setting;
import elexis.modules.api.settings.impl.BooleanSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

@FunctionAdd(name = "SelverHelper", alias = "SelverHelper", category = Category.Player, description = "")
@Environment(EnvType.CLIENT)
public class ServerHelper extends Function {
    
    public static class KeyBind {
        private final BooleanSetting setting;
        private final Item item;
        
        public KeyBind(BooleanSetting setting, Item item) {
            this.setting = setting;
            this.item = item;
        }
        
        public BooleanSetting setting() {
            return setting;
        }
        
        public Item item() {
            return item;
        }
    }
    
    private final List<KeyBind> keyBinds = new ArrayList<>();
    
    public ServerHelper() {
        // Initialize with some default keybinds
        keyBinds.add(new KeyBind(new BooleanSetting("Pearl", true), Items.ENDER_PEARL));
        keyBinds.add(new KeyBind(new BooleanSetting("Gapple", true), Items.GOLDEN_APPLE));
        keyBinds.add(new KeyBind(new BooleanSetting("Crystal", true), Items.END_CRYSTAL));
        keyBinds.add(new KeyBind(new BooleanSetting("Totem", true), Items.TOTEM_OF_UNDYING));
        keyBinds.add(new KeyBind(new BooleanSetting("Soup", true), Items.MUSHROOM_STEW));
        keyBinds.add(new KeyBind(new BooleanSetting("Pot", true), Items.SPLASH_POTION));
        
        for (KeyBind bind : keyBinds) {
            addSettings(bind.setting());
        }
    }
    
    public List<KeyBind> getKeyBindings() {
        return keyBinds;
    }
}
