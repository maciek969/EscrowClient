package elexis.modules.impl.utils;

import elexis.client.events.impl.client.EventKey;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.player.PlayerInventoryUtil;
import elexis.client.utils.player.ServerUtil;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BindSetting;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.SliderSetting;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

@FunctionAdd(name = "ServerAssistant", alias = "Server Assistant", category = Category.Utilities)
public class ServerAssistant extends Function {
    public static ModeSetting server = new ModeSetting("Ваш сервер", "FunTime",
            "FunTime", "HolyWorld", "SpookyTime", "ReallyWorld", "Кастом", "Без разницы");
    public static BooleanSetting multiActionBypass = new BooleanSetting("Обход MultiAction", false);
    public static SliderSetting tickToRunAction = new SliderSetting("Тики до выполнения", 3, 1, 5, 1).setVisible(() -> server.is("Кастом"));
    public static SliderSetting tickToReturnKeys = new SliderSetting("Тики до включения клавиш", 4, 1, 8, 1).setVisible(() -> server.is("Кастом"));;

    List<KeyBind> keyBindings = new ArrayList<>();

    public void initialize() {
        keyBindings.add(new KeyBind(Items.FIREWORK_STAR, new BindSetting("Анти-полёт", -1).setVisible(ServerUtil::isReallyWorld), 0));
        keyBindings.add(new KeyBind(Items.FLOWER_BANNER_PATTERN, new BindSetting("Свиток опыта", -1).setVisible(ServerUtil::isReallyWorld), 0));
        keyBindings.add(new KeyBind(Items.PRISMARINE_SHARD, new BindSetting("Взрывная трапка", -1).setVisible(ServerUtil::isHolyWorld), 5));
        keyBindings.add(new KeyBind(Items.POPPED_CHORUS_FRUIT, new BindSetting("Обычная трапка", -1).setVisible(ServerUtil::isHolyWorld), 0));
        keyBindings.add(new KeyBind(Items.NETHER_STAR, new BindSetting("Стан", -1).setVisible(ServerUtil::isHolyWorld), 30));
        keyBindings.add(new KeyBind(Items.FIRE_CHARGE, new BindSetting("Взрывная штучка", -1).setVisible(ServerUtil::isHolyWorld), 0));
        keyBindings.add(new KeyBind(Items.SNOWBALL, new BindSetting("Снежок", -1).setVisible(() -> ServerUtil.isCopyTime() || ServerUtil.isHolyWorld()), 0));
        keyBindings.add(new KeyBind(Items.PHANTOM_MEMBRANE, new BindSetting("Божья аура", -1).setVisible(ServerUtil::isCopyTime), 0));
        keyBindings.add(new KeyBind(Items.NETHERITE_SCRAP, new BindSetting("Трапка", -1).setVisible(ServerUtil::isCopyTime), 0));
        keyBindings.add(new KeyBind(Items.DRIED_KELP, new BindSetting("Пласт", -1).setVisible(ServerUtil::isCopyTime), 0));
        keyBindings.add(new KeyBind(Items.SUGAR, new BindSetting("Явная пыль", -1).setVisible(ServerUtil::isCopyTime), 10));
        keyBindings.add(new KeyBind(Items.FIRE_CHARGE, new BindSetting("Огненный смерч", -1).setVisible(ServerUtil::isCopyTime), 10));
        keyBindings.add(new KeyBind(Items.ENDER_EYE, new BindSetting("Дезориентация", -1).setVisible(ServerUtil::isCopyTime), 10));
        keyBindings.forEach(bind -> addSettings(bind.setting));
        addSettings();
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @EventHandler
    public void onKey(EventKey e) {
        keyBindings.stream().filter(bind -> e.isKeyReleased(bind.setting.get()) && bind.setting.isVisible() && validDistance(bind.distance)).forEach(bind -> PlayerInventoryUtil.swapAndUse(bind.item));
    }

    public record KeyBind(Item item, BindSetting setting, float distance) {
    }

    public ServerAssistant() {
        addSettings(server, multiActionBypass, tickToRunAction, tickToReturnKeys);
        initialize();
    }

    private boolean validDistance(float dist) {
        // todo
        return dist == 0 || mc.world.getPlayers().stream().anyMatch(p -> p != mc.player && mc.player.distanceTo(p) <= dist);
    //    return dist == 0 || mc.world.getPlayers().stream().anyMatch(p -> p != mc.player && !Elexis.getInstance().getFriendStorage().isFriend(p.getName().getString()) && mc.player.distanceTo(p) <= dist);
    }
}