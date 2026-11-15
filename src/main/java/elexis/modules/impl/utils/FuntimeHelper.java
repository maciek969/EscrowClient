package elexis.modules.impl.utils;

import elexis.client.events.impl.client.EventKey;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.player.PlayerInventoryUtil;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BindSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

@FunctionAdd(name = "FuntimeHelper", alias = "FuntimeHelper", category = Category.Utilities, description = "Быстрый юз предметов FunTime")
@Environment(EnvType.CLIENT)
public class FuntimeHelper extends Function {
    private final List<ItemBind> binds = new ArrayList<>();

    public FuntimeHelper() {
        this.binds.add(new ItemBind(Items.SUGAR, new BindSetting("Сахар", -1)));
        this.binds.add(new ItemBind(Items.ENDER_EYE, new BindSetting("Око Эндера", -1)));
        this.binds.add(new ItemBind(Items.NETHERITE_SCRAP, new BindSetting("Незеритовый Лом", -1)));
        this.binds.add(new ItemBind(Items.SNOWBALL, new BindSetting("Снежок", -1)));
        this.binds.add(new ItemBind(Items.GLOWSTONE_DUST, new BindSetting("Луминарий", -1)));
        this.binds.forEach(bind -> this.addSettings(bind.setting()));
    }

    @EventHandler
    public void onKey(EventKey event) {
        if (nullCheck()) {
            return;
        }

        // Срабатываем при нажатии (action == 1), а не при отпускании
        if (event.getAction() != 1) {
            return;
        }

        for (ItemBind bind : this.binds) {
            int key = bind.setting().get();
            if (key == -1) continue;

            if (event.isKeyDown(key)) {
                PlayerInventoryUtil.swapAndUse(bind.item(), false);
            }
        }
    }

    private record ItemBind(Item item, BindSetting setting) {
    }
}
