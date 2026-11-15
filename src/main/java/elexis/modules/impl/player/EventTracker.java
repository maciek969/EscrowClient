package elexis.modules.impl.player;

import elexis.Elexis;
import elexis.client.ClientColors;
import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.events.impl.client.EventWorldEmit;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.MultiBooleanSetting;
import elexis.modules.impl.player.eventtracker.SpecialPotionType;
import elexis.modules.impl.render.Interface;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.WorldEvents;

import java.awt.Color;

@FunctionAdd(name = "EventTracker", alias = "Event Tracker", category = Category.Player, description = "Показывает игровые события вокруг тебя")
public class EventTracker extends Function {
    private final MultiBooleanSetting trackedEvents = new MultiBooleanSetting("Events to track",
            new BooleanSetting("Totem pop", true),
            new BooleanSetting("Potion use", true),
            new BooleanSetting("Pickup Item", true)
    );

    public EventTracker() {
        addSettings(trackedEvents);
    }

    @EventHandler
    public void onReceivePacket(EventReceivePacket event) {
        if (mc.world == null || mc.player == null || Interface.active() == null) {
            return;
        }

        if (event.getPacket() instanceof EntityStatusS2CPacket status && trackedEvents.isSelected("Totem pop")) {
            Entity entity = status.getEntity(mc.world);
            if (entity instanceof LivingEntity living && status.getStatus() == EntityStatuses.USE_TOTEM_OF_UNDYING) {
                ItemStack protect = living.getMainHandStack();
                if (protect.getItem() != Items.TOTEM_OF_UNDYING) {
                    protect = living.getOffHandStack();
                }

                boolean friend = Elexis.getInstance().getFriendStorage().isFriend(living.getName().getString());
                MutableText message = Text.empty()
                        .append(Text.of(living.getName().getString()).copy().withColor(friend ? ClientColors.FRIEND_COLOR.getRGB() : Color.WHITE.getRGB()))
                        .append(" потерял тотем ")
                        .append(protect.getName());
                Interface.notify(message, 0xFFF0C344, 4000, new ItemStack(Items.TOTEM_OF_UNDYING));
            }
        }

        if (event.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket packet && trackedEvents.isSelected("Pickup Item")) {
            if (packet.getSyncId() == 0 && mc.player.currentScreenHandler != null) {
                int slot = packet.getSlot();
                if (slot >= 0 && slot < mc.player.currentScreenHandler.slots.size()
                        && mc.player.currentScreenHandler.getSlot(slot).getStack().isEmpty()
                        && !packet.getStack().isEmpty()) {
                    Interface.notify(
                            Text.of("Подобран предмет ").copy().append(packet.getStack().getFormattedName()),
                            0xFF52BEFF,
                            4000
                    );
                }
            }
        }
    }

    @EventHandler
    public void onWorldEmit(EventWorldEmit event) {
        if (mc.world == null || !trackedEvents.isSelected("Potion use") || event.getEventId() != WorldEvents.SPLASH_POTION_SPLASHED) {
            return;
        }

        float range = 4.71f;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.getBoundingBox().getCenter().distanceTo(event.getPos().toCenterPos()) > range) {
                continue;
            }

            StatusEffect effect = findVanillaEffect(event.getData());
            if (effect != null) {
                Interface.notify(
                        player.getName().copy().append(" использовал зелье ").append(effect.getName().copy().withColor(effect.getColor())),
                        0xFF000000 | effect.getColor(),
                        4000
                );
                continue;
            }

            SpecialPotionType special = findSpecial(event.getData());
            if (special != null) {
                Interface.notify(
                        player.getName().copy().append(" получил баф ").append(Text.of(special.getDisplayName()).copy().withColor(special.getBaseColor())),
                        0xFF000000 | special.getBaseColor(),
                        4000
                );
            }
        }
    }

    private StatusEffect findVanillaEffect(int color) {
        int parsed = unpackColor(color);
        for (RegistryEntry<StatusEffect> entry : Registries.STATUS_EFFECT.getIndexedEntries()) {
            StatusEffect effect = entry.value();
            if (new Color(effect.getColor()).getRGB() == parsed) {
                return effect;
            }
        }
        return null;
    }

    private SpecialPotionType findSpecial(int color) {
        int parsed = unpackColor(color);
        for (SpecialPotionType type : SpecialPotionType.values()) {
            if (type.getBaseColor() == parsed || type.getColorVariations().contains(parsed) || colorDistance(type.getBaseColor(), parsed) < 30.0) {
                return type;
            }
        }
        return null;
    }

    private static int unpackColor(int color) {
        float red = (float) (color >> 16 & 255) / 255.0f;
        float green = (float) (color >> 8 & 255) / 255.0f;
        float blue = (float) (color & 255) / 255.0f;
        return new Color(red, green, blue).getRGB();
    }

    private static double colorDistance(int first, int second) {
        Color a = new Color(first, true);
        Color b = new Color(second, true);
        int dr = a.getRed() - b.getRed();
        int dg = a.getGreen() - b.getGreen();
        int db = a.getBlue() - b.getBlue();
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }
}
