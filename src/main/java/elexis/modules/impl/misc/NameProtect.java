package elexis.modules.impl.misc;

import elexis.Elexis;
import elexis.mixins.accessors.JavelinChatAccessor;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.api.settings.impl.MultiBooleanSetting;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@FunctionAdd(name = "NameProtect", alias = "NameProtect", category = Category.Utilities,
        description = "Скрывает ник, друзей, клан, сервер и координаты на экране")
public final class NameProtect extends Function {
    private static final String[] RANKS = {"Выкл", "PLAYER", "HERO", "TITAN", "AVENGER", "GOD", "IMPERATOR", "DRAGON", "OVERLORD", "ADMIN"};
    private static final String[] SYMBOLS = {"", "ꔀ", "ꔄ", "ꔈ", "ꔒ", "ꕁ", "ꔤ", "ꔨ", "ꔖ", "ꔷ"};
    private static final Pattern CLAN = Pattern.compile("(?iu)\\[[a-zа-яё0-9_]{1,10}]\\s?|(?<=Клан: ).+");
    private static final Pattern GRIEF = Pattern.compile("(?iu)Анархия-?\\s?\\d+|ГРИФ\\s?#?\\s?\\d+");
    private static final Pattern DEBUG_COORDINATES = Pattern.compile("(?i)^\\s*(XYZ:|Block:|Chunk:|Facing:|Targeted Block:|Targeted Fluid:).*");
    public final ModeSetting nick = new ModeSetting("Ник", "vorkis.lox", "vorkis.lox", "protected", "***", "you", "player");
    private final BooleanSetting hideGrief = new BooleanSetting("Скрыть гриф", true);
    private final BooleanSetting hideClan = new BooleanSetting("Скрыть клан", true);
    private final ModeSetting donate = new ModeSetting("Донат", "Выкл", RANKS);
    private final MultiBooleanSetting streamer = new MultiBooleanSetting("Стример режим",
            new BooleanSetting("Скрыть координаты", true), new BooleanSetting("Менять бренд", true));
    public final ModeSetting tabPosition = new ModeSetting("Место в табе", "Выкл", "Выкл", "Верх", "Низ");
    private String lastSettings;

    public NameProtect() { addSettings(nick, hideGrief, hideClan, donate, streamer, tabPosition); }

    public static NameProtect active() {
        var manager = Elexis.getFunctionManager();
        if (manager == null || mc.player == null) return null;
        var module = manager.getNameProtect();
        return module != null && module.isState() ? module : null;
    }

    public boolean hideCoordinates() { return streamer.get("Скрыть координаты"); }
    public boolean hideBrand() { return streamer.get("Менять бренд"); }

    public static String display(String text) {
        NameProtect module = active();
        return module == null ? text : module.patchString(text, ownNameIn(text));
    }

    public static Text display(Text text) {
        NameProtect module = active();
        if (module == null || text == null) return text;
        String plain = text.getString();
        boolean own = ownNameIn(plain);
        if (plain.equals(module.patchString(plain, own))) return text;
        // Walk styled runs so team colors and custom rank fonts survive the substitution.
        var output = Text.empty();
        text.visit((style, part) -> {
            output.append(Text.literal(module.patchString(part, own)).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        // Servers sometimes split a protected token across components.
        String rendered = output.getString();
        String safe = module.patchString(rendered, own);
        return rendered.equals(safe) ? output : Text.literal(safe).setStyle(text.getStyle());
    }

    private static boolean ownNameIn(String text) {
        return text != null && text.toLowerCase(java.util.Locale.ROOT)
                .contains(mc.player.getGameProfile().name().toLowerCase(java.util.Locale.ROOT));
    }

    private String patchString(String input, boolean own) {
        if (input == null || input.isEmpty()) return input;
        if (hideCoordinates() && DEBUG_COORDINATES.matcher(input).matches()) return input.split(":", 2)[0] + ": •••";
        String result = replace(input, mc.player.getGameProfile().name(), nick.get());
        result = replace(result, mc.getSession().getUsername(), nick.get());
        for (var friend : Elexis.getInstance().getFriendStorage().getFriends()) result = replace(result, friend.getName(), "Друг");
        if (hideGrief.get()) result = GRIEF.matcher(result).replaceAll("Скрыто");
        if (hideClan.get()) result = CLAN.matcher(result).replaceAll("");
        if (hideBrand()) {
            var connection = mc.getNetworkHandler();
            var server = mc.getCurrentServerEntry();
            if (connection != null) result = replace(result, connection.getBrand(), "server");
            if (server != null) result = replace(result, server.address, "server");
        }
        if (own && donate.getIndex() > 0) {
            var team = mc.player.getScoreboardTeam();
            if (team != null) {
                String decoration = team.getPrefix().getString() + team.getSuffix().getString();
                for (int i = 1; i < SYMBOLS.length; i++) {
                    if (decoration.contains(SYMBOLS[i])) return result.replace(SYMBOLS[i], SYMBOLS[donate.getIndex()]);
                }
            }
        }
        return result;
    }

    private static String replace(String text, String token, String value) {
        if (token == null || token.isBlank() || token.equalsIgnoreCase(value)) return text;
        return Pattern.compile(Pattern.quote(token), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(text).replaceAll(Matcher.quoteReplacement(value));
    }

    public static List<String> debugLines(List<String> lines) {
        if (active() == null) return lines;
        var result = new ArrayList<String>(lines.size());
        for (String line : lines) result.add(display(line));
        return result;
    }

    @elexis.client.utils.eventbus.EventHandler
    public void refreshChat(elexis.client.events.impl.entity.EventUpdate.Fast event) {
        String settings = nick.get() + hideGrief.get() + hideClan.get() + donate.get() + hideBrand() + hideCoordinates();
        if (!settings.equals(lastSettings)) {
            lastSettings = settings;
            refresh();
        }
    }

    private static void refresh() {
        if (mc.inGameHud != null) ((JavelinChatAccessor)mc.inGameHud.getChatHud()).elexis$refresh();
    }

    @Override public void onEnable() { super.onEnable(); refresh(); }
    @Override public void onDisable() { super.onDisable(); lastSettings = null; refresh(); }
}
