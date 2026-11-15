package elexis.modules.impl.combat;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.ModeSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared bot filter for combat modules.
 * Old mode excludes player entities absent from the server's player list.
 * New mode excludes players whose names contain "Bot"/"bot" or start with "Reserved".
 */
@FunctionAdd(name = "AntiBot", alias = "AntiBot", category = Category.Combat, description = "Фильтрует ботов из списка целей")
public class AntiBot extends Function {
    private final ModeSetting mode = new ModeSetting("Выбор режима", "Новый", "Старый", "Новый");
    /** Player entities excluded by the selected filter. */
    public static List<Entity> bot = new ArrayList<>();

    public AntiBot() {
        addSettings(mode);
    }

    @EventHandler
    public void onUpdate(EventUpdate.Fast event) {
        if (mc.world == null || mc.player == null) return;
        if (mode.is("Новый")) {
            newMatrix();
        } else if (mode.is("Старый")) {
            oldMatrix();
        }
    }

    /**
     * Exclude entities absent from the server's player list. A listed player may
     * legitimately have zero latency on LAN or when a server does not report ping.
     */
    public void oldMatrix() {
        bot.clear();
        if (mc.world == null || mc.player == null || mc.getNetworkHandler() == null) return;

        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity == mc.player) continue;

            var entry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());

            if (entry == null) {
                bot.add(entity);
            }
        }
    }

    /**
     * New mode: mark as bot by name pattern.
     */
    public void newMatrix() {
        if (mc.world == null || mc.player == null) return;

        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity == mc.player) continue;

            String name = entity.getName().getString();
            boolean isBot = name.contains("Bot") || name.contains("bot") || name.startsWith("Reserved");
            if (isBot) {
                if (!bot.contains(entity)) bot.add(entity);
            } else {
                bot.remove(entity);
            }
        }
    }

    public static boolean checkBot(LivingEntity entity) {
        return entity instanceof PlayerEntity && bot.contains(entity);
    }

    @Override
    public void onDisable() {
        bot.clear();
        super.onDisable();
    }
}
