package elexis.modules.impl.misc;

import elexis.client.events.impl.client.EventKey;
import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BindSetting;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.impl.render.visual.VisualGeometry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;
import java.util.regex.Pattern;

@FunctionAdd(name = "HWHelper", alias = "HWHelper", category = Category.Utilities, description = "HolyWorld item binds and radius")
public final class HWHelper extends Function {
    private static final Pattern LORE_RADIUS = Pattern.compile("(?iu)радиус\\s*:?\\s*(\\d+(?:[.,]\\d+)?)\\s*блок");
    public final BooleanSetting itemRadius = new BooleanSetting("Item radius", false);
    public final BindSetting trapKey = new BindSetting("Explosive trap bind", 0);
    public final BindSetting snowKey = new BindSetting("Snow bind", 0);
    public final BindSetting stanKey = new BindSetting("Stun bind", 0);
    public final BindSetting explosionKey = new BindSetting("Explosive gadget bind", 0);

    public HWHelper() { addSettings(itemRadius, trapKey, snowKey, stanKey, explosionKey); }

    @EventHandler public void input(EventKey event) {
        if (nullCheck() || event.screen() != null) return;
        if (event.isKeyDown(trapKey.get())) HelperItems.use(HelperItems.find(Items.PRISMARINE_SHARD, "Explosive trap", false));
        else if (event.isKeyDown(snowKey.get())) HelperItems.use(HelperItems.find(Items.SNOWBALL, "Snowball", false));
        else if (event.isKeyDown(stanKey.get())) HelperItems.use(HelperItems.find(Items.NETHER_STAR, "Stun", false));
        else if (event.isKeyDown(explosionKey.get())) HelperItems.use(HelperItems.find(Items.FIRE_CHARGE, "Explosive gadget", false));
    }

    @EventHandler public void sound(EventReceivePacket event) {
        if (!(event.getPacket() instanceof PlaySoundS2CPacket packet)) return;
        String trap = switch (packet.getSound().value().id().getPath()) {
            case "block.beacon.deactivate" -> "Stun";
            case "entity.generic.explode" -> "Explosive trap";
            default -> null;
        };
        if (trap == null) return;
        var connection = mc.getNetworkHandler();
        mc.execute(() -> {
            if (isState() && connection == mc.getNetworkHandler() && !nullCheck() && HelperItems.serverContains("holyworld")) {
                mc.player.sendMessage(Text.literal(String.format(Locale.ROOT, "%s detected: %.0f %.0f %.0f (15 sec.)",
                        trap, packet.getX(), packet.getY(), packet.getZ())), false);
            }
        });
    }

    @EventHandler public void render(EventRender.World event) {
        if (nullCheck() || !itemRadius.get()) return;
        double radius = preview(mc.player.getMainHandStack());
        if (radius == 0) radius = preview(mc.player.getOffHandStack());
        if (radius == 0) return;
        var feet = mc.player.getBlockPos();
        double y = feet.getY() + .025;
        int color = 0xE6000000 | ThemeColorUtils.getIconColor(0, 0) & 0xFFFFFF;
        try (var frame = new VisualGeometry.Frame(event)) {
            if (radius > 0) frame.ring(new Vec3d(mc.player.getX(), y, mc.player.getZ()), radius, color);
            else {
                int minX = feet.getX() - 1, minZ = feet.getZ() - 1;
                for (int i = 0; i <= 3; i++) {
                    frame.line(minX + i, y, minZ, minX + i, y, minZ + 3, color);
                    frame.line(minX, y, minZ + i, minX + 3, y, minZ + i, color);
                }
            }
        }
    }

    private static double preview(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) for (Text line : lore.lines()) {
            var matcher = LORE_RADIUS.matcher(line.getString());
            if (matcher.find()) return Math.min(50, Double.parseDouble(matcher.group(1).replace(',', '.')));
        }
        return stack.getName().getString().toLowerCase(Locale.ROOT).contains("трапк") ? -1 : 0;
    }
}
