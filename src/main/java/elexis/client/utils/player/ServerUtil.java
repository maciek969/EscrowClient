package elexis.client.utils.player;

import elexis.client.utils.client.IMinecraft;
import elexis.client.utils.math.time.StopWatch;
import elexis.modules.impl.utils.ServerAssistant;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.scoreboard.*;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class ServerUtil implements IMinecraft {
    final Pattern ANARCHY_PATTERN = Pattern.compile("\\d+");

    @Getter
    public int anarchy;
    @Getter
    final StopWatch pvpWatch = new StopWatch();

    public void tick() {
        anarchy = getAnarchyMode();
        if (inPvp()) pvpWatch.reset();
    }

    int getAnarchyMode() {
        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) return -1;

        switch (getServer()) {
            case "FunTime" -> {
                String[] string = objective.getDisplayName().getString().split("-");
                if (string.length > 1) return parseAnarchy(string[1]);
            }
            case "HolyWorld" -> {
                for (ScoreboardEntry scoreboardEntry : scoreboard.getScoreboardEntries(objective)) {
                    String text = Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name()).getString();
                    if (!text.isEmpty()) {
                        String string = StringUtils.substringBetween(text, "#", " -в—†-");
                        if (string != null && !string.isEmpty()) return parseAnarchy(string);
                    }
                }
            }
        }
        return -1;
    }

    int parseAnarchy(String text) {
        Matcher matcher = ANARCHY_PATTERN.matcher(text);
        if (!matcher.find()) return -1;

        try {
            return Integer.parseInt(matcher.group());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public boolean inPvp() {
        return mc.inGameHud.getBossBarHud().bossBars.values().stream().map(c -> c.getName().getString().toLowerCase()).anyMatch(s -> s.contains("pvp") || s.contains("РїРІРї"));
    }

    public String getServer() {
        return ServerAssistant.server.get();
    }

    public boolean isCopyTime() {return getServer().equals("FunTime") || getServer().equals("SpookyTime");}
    public boolean isFunTime() {return getServer().equals("FunTime");}
    public boolean isReallyWorld() {return getServer().equals("ReallyWorld");}
    public boolean isHolyWorld() {return getServer().equals("HolyWorld");}

    public boolean isPvp() {
        return !pvpWatch.finished(500);
    }
}