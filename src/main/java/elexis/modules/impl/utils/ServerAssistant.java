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
    public static ModeSetting server = new ModeSetting("Your server", "FunTime",
            "FunTime", "HolyWorld", "SpookyTime", "ReallyWorld", "Custom", "Any");
    public static BooleanSetting multiActionBypass = new BooleanSetting("MultiAction bypass", false);
    public static SliderSetting tickToRunAction = new SliderSetting("Ticks until action", 3, 1, 5, 1).setVisible(() -> server.is("Custom"));
    public static SliderSetting tickToReturnKeys = new SliderSetting("Ticks until keys enabled", 4, 1, 8, 1).setVisible(() -> server.is("Custom"));;

    List<KeyBind> keyBindings = new ArrayList<>();

    public void initialize() {
        keyBindings.add(new KeyBind(Items.FIREWORK_STAR, new BindSetting("Anti Fly", -1).setVisible(ServerUtil::isReallyWorld), 0));
        keyBindings.add(new KeyBind(Items.FLOWER_BANNER_PATTERN, new BindSetting("XP scroll", -1).setVisible(ServerUtil::isReallyWorld), 0));
        keyBindings.add(new KeyBind(Items.PRISMARINE_SHARD, new BindSetting("Explosive trap", -1).setVisible(ServerUtil::isHolyWorld), 5));
        keyBindings.add(new KeyBind(Items.POPPED_CHORUS_FRUIT, new BindSetting("Normal trap", -1).setVisible(ServerUtil::isHolyWorld), 0));
        keyBindings.add(new KeyBind(Items.NETHER_STAR, new BindSetting("Stun", -1).setVisible(ServerUtil::isHolyWorld), 30));
        keyBindings.add(new KeyBind(Items.FIRE_CHARGE, new BindSetting("Explosive gadget", -1).setVisible(ServerUtil::isHolyWorld), 0));
        keyBindings.add(new KeyBind(Items.SNOWBALL, new BindSetting("Snowball", -1).setVisible(() -> ServerUtil.isCopyTime() || ServerUtil.isHolyWorld()), 0));
        keyBindings.add(new KeyBind(Items.PHANTOM_MEMBRANE, new BindSetting("God aura", -1).setVisible(ServerUtil::isCopyTime), 0));
        keyBindings.add(new KeyBind(Items.NETHERITE_SCRAP, new BindSetting("Trap", -1).setVisible(ServerUtil::isCopyTime), 0));
        keyBindings.add(new KeyBind(Items.DRIED_KELP, new BindSetting("Slab", -1).setVisible(ServerUtil::isCopyTime), 0));
        keyBindings.add(new KeyBind(Items.SUGAR, new BindSetting("Visible dust", -1).setVisible(ServerUtil::isCopyTime), 10));
        keyBindings.add(new KeyBind(Items.FIRE_CHARGE, new BindSetting("Firestorm", -1).setVisible(ServerUtil::isCopyTime), 10));
        keyBindings.add(new KeyBind(Items.ENDER_EYE, new BindSetting("Disorientation", -1).setVisible(ServerUtil::isCopyTime), 10));
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