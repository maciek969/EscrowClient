package elexis.client.utils.text;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class RussianText {
    private static final Charset CP1251 = Charset.forName("windows-1251");
    private static boolean russian = false;
    private static final Map<String, String> RU = new HashMap<>();
    private static final Map<String, String> EN = new HashMap<>();

    static {
        add("Combat", "Combat");
        add("Movement", "Movement");
        add("Render", "Render");
        add("Player", "Player");
        add("Utilities", "Utilities");

        add("HitAura", "Aura");
        add("Hit Aura", "Aura");
        add("Aura", "Aura");
        add("NoWeb", "NoWeb");
        add("AutoSprint", "Auto Sprint");
        add("Auto Sprint", "Auto Sprint");
        add("NoSlow", "NoSlow");
        add("NoFallDamage", "No fall damage");
        add("ElytraTarget", "Elytra target");
        add("ElytraBooster", "Elytra Booster");
        add("ElytraMotion", "Elytra Motion");
        add("GuiMove", "GUI move");
        add("AirStuck", "AirStuck");
        add("AutoTotem", "Auto Totem");
        add("AutoArmor", "Auto Armor");
        add("ChestStealer", "Chest stealer");
        add("ItemScroller", "Item scroller");
        add("ClickPearl", "Click Pearl");
        add("FullBright", "Full bright");
        add("ViewModel", "View model");
        add("CustomSwing", "Swing animation");
        add("Item Physic", "Item physics");
        add("Box 3D", "3D boxes");
        add("Box3D", "3D boxes");
        add("Baby Boy", "Baby player");
        add("BabyBoy", "Baby player");
        add("Crosshair", "Crosshair");
        add("KillEffect", "Kill effect");
        add("Kill Effect", "Kill effect");
        add("NameTags", "Name tags");
        add("TargetESP", "Target ESP");
        add("SeeInvisible", "See invisible");
        add("CustomWorld", "Custom world");
        add("NoRneder", "No render");
        add("NoRender", "No render");
        add("CameraTweaks", "Camera tweaks");
        add("CameraSmothAI", "Smooth camera");
        add("ChinaHat", "China hat");
        add("JumpCircle", "Jump circle");
        add("Predictions", "Predictions");
        add("Particles", "Particles");
        add("Themes", "Themes");
        add("Cosmetic", "Cosmetic");
        add("FireLook", "Fire look");
        add("NoPlayer", "No player");
        add("Arrows", "Arrows");
        add("Hands", "Hands");
        add("Wings", "Wings");
        add("Trails", "Trails");

        add("Mode", "Mode");
        add("Rotation mode", "Rotation mode");
        add("Snap mode", "Snap mode");
        add("Attack timing", "Attack timing");
        add("Other before attack", "Other before attack");
        add("Extra settings", "Extra settings");
        add("Targets", "Targets");
        add("Players", "Players");
        add("Naked", "Naked");
        add("Mobs", "Mobs");
        add("Animals", "Animals");
        add("Friends", "Friends");
        add("Speed", "Speed");
        add("Climb Speed", "Climb speed");
        add("Center Dot", "Center dot");
        add("Gap", "Gap");
        add("Expand Power", "Expand power");
        add("Length", "Length");
        add("Width", "Width");
        add("Search", "Search");
        add("Edit Cosmetic", "Edit cosmetic");
        add("Size", "Size");
        add("Watermark", "Watermark");
        add("Hotkeys", "Hotkeys");
        add("TargetHud", "Target HUD");
        add("Effects", "Effects");
        add("Notifications", "Notifications");
        add("Staff list", "Staff list");
        add("Compass", "Compass");
        add("Hotbar", "Hotbar");
        add("Minecraft UI", "Minecraft UI");
        add("Debug", "Debug");
        add("Username", "Username");
        add("Server IP", "Server IP");
        add("Blur", "Blur");

        add("ReallyWorld", "ReallyWorld");
        add("Vanilla", "Default");
        add("Classic", "Classic");
        add("Custom", "Custom");
        add("Auto", "Auto");
        add("Fast", "Fast");
        add("Dynamic", "Dynamic");
        add("Smooth", "Smooth");
        add("Random", "Random");
        add("None", "None");
        add("Default", "Default");
        add("Free", "Free");
        add("Yes", "Yes");
        add("No", "None");
        add("Down", "Down");
        add("Left Hand", "Left hand");
        add("Slide", "Slide");
        add("Stab", "Stab");
        add("Spin", "Spin");
        add("Push", "Push");
        add("Knife", "Knife");

        add("Hit through blocks", "Hit through blocks");
        add("Weapon only", "Weapon only");
        add("Do not hit while eating", "Do not hit while eating");
        add("Do not hit in container", "Do not hit in container");
        add("Break shield", "Break shield");
        add("Release shield on hit", "Release shield on hit");
        add("Hit and eat food", "Hit and eat food");
        add("Legit sprint", "Legit sprint");
        add("Attack only with weapon", "Weapon only");
        add("Ignore walls", "Hit through blocks");
        add("Dynamic timing", "Dynamic");
        add("Attack Timing", "Attack timing");
        add("Before Attack", "Other before attack");
        add("Extra Settings", "Extra settings");
        add("Shield Break", "Break shield");
        add("Shield break", "Break shield");
        add("Unpress shield", "Release shield on hit");
        add("Unpress shield on hit", "Release shield on hit");

        add("SpookyTime", "Spooky");
        add("WellMine", "WellMine");
        add("LonyGrief", "LonyGrief");
        add("Polar", "Polar");
        add("Snap", "Snap");
        add("WhiteRise", "WhiteRise");
        add("CakeWorld", "CakeWorld");
        add("Stellar Sloth", "Stellar");
        add("METAHVH", "Meta HVH");
        add("HvH", "HvH");
        add("Droid", "Droid");
        add("AresMine", "AresMine");
        add("Grim", "Grim");
        add("SlotAC", "SlotAC");
    }

    private RussianText() {}

    public static boolean isRussian() {
        return russian;
    }

    public static void setRussian(boolean value) {
        russian = value;
    }

    public static String languageName() {
        return russian ? "RU" : "English";
    }

    private static void add(String en, String ru) {
        RU.put(en, ru);
        EN.put(ru, en);
        EN.put(fixMojibake(ru), en);
    }

    public static String tr(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return fixMojibake(text);
    }

    public static String fixMojibake(String text) {
        if (text == null || text.isEmpty() || !looksBroken(text)) {
            return text;
        }
        String current = text;
        for (int i = 0; i < 4 && looksBroken(current); i++) {
            try {
                String decoded = new String(current.getBytes(CP1251), StandardCharsets.UTF_8);
                if (score(decoded) > score(current)) {
                    current = decoded;
                } else {
                    break;
                }
            } catch (Exception ignored) {
                break;
            }
        }
        return current;
    }

    private static boolean looksBroken(String text) {
        return text.indexOf('\u0420') >= 0
                || text.indexOf('\u0421') >= 0
                || text.indexOf('\u0403') >= 0
                || text.indexOf('\u040b') >= 0
                || text.indexOf('\u0432') >= 0
                || text.indexOf('?') >= 0;
    }

    private static int score(String text) {
        int score = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= '\u0410' && c <= '\u044f') || c == '\u0451' || c == '\u0401') score += 3;
            if (c == '\u0420' || c == '\u0421' || c == '\u0403' || c == '\u040b' || c == '\u0432' || c == '?') score -= 2;
        }
        return score;
    }
}
