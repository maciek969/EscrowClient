package elexis.client.utils.text;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class RussianText {
    private static final Charset CP1251 = Charset.forName("windows-1251");
    private static boolean russian = true;
    private static final Map<String, String> RU = new HashMap<>();
    private static final Map<String, String> EN = new HashMap<>();

    static {
        add("Combat", "Комбат");
        add("Movement", "Движение");
        add("Render", "Визуал");
        add("Player", "Игрок");
        add("Utilities", "Утилиты");

        add("HitAura", "Аура");
        add("Hit Aura", "Аура");
        add("Aura", "Аура");
        add("NoWeb", "Анти-паутина");
        add("AutoSprint", "Авто-спринт");
        add("Auto Sprint", "Авто-спринт");
        add("NoSlow", "Без замедления");
        add("NoFallDamage", "Без урона от падения");
        add("ElytraTarget", "Элитра-таргет");
        add("ElytraBooster", "Бустер элитры");
        add("ElytraMotion", "Движение элитры");
        add("GuiMove", "Ходьба в GUI");
        add("AirStuck", "Зависание в воздухе");
        add("AutoTotem", "Авто-тотем");
        add("AutoArmor", "Авто-броня");
        add("ChestStealer", "Сборщик сундуков");
        add("ItemScroller", "Прокрутка предметов");
        add("ClickPearl", "Клик-перл");
        add("FullBright", "Полная яркость");
        add("ViewModel", "Модель рук");
        add("CustomSwing", "Анимация взмаха");
        add("Item Physic", "Физика предметов");
        add("Box 3D", "3D боксы");
        add("Box3D", "3D боксы");
        add("Baby Boy", "Маленький игрок");
        add("BabyBoy", "Маленький игрок");
        add("Crosshair", "Прицел");
        add("KillEffect", "Эффект убийства");
        add("Kill Effect", "Эффект убийства");
        add("NameTags", "Теги имён");
        add("TargetESP", "Подсветка цели");
        add("SeeInvisible", "Видеть невидимых");
        add("CustomWorld", "Кастомный мир");
        add("NoRneder", "Без рендера");
        add("NoRender", "Без рендера");
        add("CameraTweaks", "Настройки камеры");
        add("CameraSmothAI", "Плавная камера");
        add("ChinaHat", "Китайская шляпа");
        add("JumpCircle", "Круг прыжка");
        add("Predictions", "Предсказания");
        add("Particles", "Частицы");
        add("Themes", "Темы");
        add("Cosmetic", "Косметика");
        add("FireLook", "Вид огня");
        add("NoPlayer", "Без игрока");
        add("Arrows", "Стрелки");
        add("Hands", "Руки");
        add("Wings", "Крылья");
        add("Trails", "Следы");

        add("Mode", "Режим");
        add("Rotation mode", "Режим ротации");
        add("Snap mode", "Режим снапа");
        add("Attack timing", "Тайминг удара");
        add("Other before attack", "Прочее до удара");
        add("Extra settings", "Доп. настройки");
        add("Targets", "Цели");
        add("Players", "Игроки");
        add("Naked", "Голые");
        add("Mobs", "Мобы");
        add("Animals", "Животные");
        add("Friends", "Друзья");
        add("Speed", "Скорость");
        add("Climb Speed", "Скорость подъёма");
        add("Center Dot", "Точка в центре");
        add("Gap", "Зазор");
        add("Expand Power", "Сила расширения");
        add("Length", "Длина");
        add("Width", "Ширина");
        add("Search", "Поиск");
        add("Edit Cosmetic", "Редактировать косметику");
        add("Size", "Размер");
        add("Watermark", "Ватермарка");
        add("Hotkeys", "Клавиши");
        add("TargetHud", "Таргет HUD");
        add("Effects", "Эффекты");
        add("Notifications", "Уведомления");
        add("Staff list", "Список персонала");
        add("Compass", "Компас");
        add("Hotbar", "Хотбар");
        add("Minecraft UI", "Интерфейс Minecraft");
        add("Debug", "Отладка");
        add("Username", "Ник");
        add("Server IP", "IP сервера");
        add("Blur", "Размытие");

        add("ReallyWorld", "ReallyWorld");
        add("Vanilla", "Обычный");
        add("Classic", "Классический");
        add("Custom", "Кастомный");
        add("Auto", "Авто");
        add("Fast", "Быстрый");
        add("Dynamic", "Динамичный");
        add("Smooth", "Плавный");
        add("Random", "Случайный");
        add("None", "Нет");
        add("Default", "Обычный");
        add("Free", "Свободный");
        add("Yes", "Да");
        add("No", "Нет");
        add("Down", "Вниз");
        add("Left Hand", "Левая рука");
        add("Slide", "Скольжение");
        add("Stab", "Укол");
        add("Spin", "Вращение");
        add("Push", "Толчок");
        add("Knife", "Нож");

        add("Hit through blocks", "Бить через блоки");
        add("Weapon only", "Бить только оружием");
        add("Do not hit while eating", "Не бить если ешь");
        add("Do not hit in container", "Не бить в контейнере");
        add("Break shield", "Ломать щит");
        add("Release shield on hit", "Отжимать щит при ударе");
        add("Hit and eat food", "Бить и есть еду");
        add("Legit sprint", "Легитный спринт");
        add("Attack only with weapon", "Бить только оружием");
        add("Ignore walls", "Бить через блоки");
        add("Dynamic timing", "Динамичный");
        add("Attack Timing", "Тайминг удара");
        add("Before Attack", "Прочее до удара");
        add("Extra Settings", "Доп. настройки");
        add("Shield Break", "Ломать щит");
        add("Shield break", "Ломать щит");
        add("Unpress shield", "Отжимать щит при ударе");
        add("Unpress shield on hit", "Отжимать щит при ударе");

        add("SpookyTime", "Спуки");
        add("WellMine", "ВеллМайн");
        add("LonyGrief", "ЛониГриф");
        add("Polar", "Полярный");
        add("Snap", "Снап");
        add("WhiteRise", "ВайтРайз");
        add("CakeWorld", "КейкВорлд");
        add("Stellar Sloth", "Стеллар");
        add("METAHVH", "Мета HVH");
        add("HvH", "HvH");
        add("Droid", "Дроид");
        add("AresMine", "АресМайн");
        add("Grim", "Грим");
        add("SlotAC", "Слот AC");
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
        String fixed = fixMojibake(text);
        return russian ? RU.getOrDefault(fixed, fixed) : EN.getOrDefault(fixed, EN.getOrDefault(text, fixed));
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
