package elexis.modules.impl.render;

import elexis.Elexis;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.ModeListSetting;

@FunctionAdd(name = "NoRender", alias = "No Render", category = Category.Render)
public class NoRneder extends Function {

    public static final ModeListSetting modes = new ModeListSetting("Режимы",
            new BooleanSetting("Огонь", false),
            new BooleanSetting("Плохие эффекты", false),
            new BooleanSetting("Оверлей блоков", false),
            new BooleanSetting("Тряска экрана", false),
            new BooleanSetting("Анимация тотема", false),
            new BooleanSetting("Боссбар", false),
            new BooleanSetting("Тени", false),
            new BooleanSetting("Дым", false),
            new BooleanSetting("Виньетка", false),
            new BooleanSetting("Стрелы в игроке", false),
            new BooleanSetting("Свечение игроков", false),
            new BooleanSetting("Оверлей лавы", false),
            new BooleanSetting("Оверлей воды", false),
            new BooleanSetting("Погода", false),
            new BooleanSetting("Скорборд", false),
            new BooleanSetting("Сердца иссушения", false),
            new BooleanSetting("Пузырьки воздуха", false),
            new BooleanSetting("Арморстенды", false),
            new BooleanSetting("Друзья", false),
            new BooleanSetting("Тайтлы", false),
            new BooleanSetting("Растительность", false)
    );

    public NoRneder() {
        addSettings(modes);
    }

    public static boolean is(String modeName) {
        elexis.modules.api.FunctionManager fm = Elexis.getFunctionManager();
        if (fm == null) return false;
        NoRneder mod = (NoRneder) fm.getFunctionByName("NoRender");
        return mod != null && mod.isState() && modes.isSelected(modeName);
    }
}
