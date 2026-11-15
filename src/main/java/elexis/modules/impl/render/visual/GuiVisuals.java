package elexis.modules.impl.render.visual;

import elexis.modules.api.*;
import elexis.modules.api.settings.impl.*;

public final class GuiVisuals {
    private GuiVisuals() {}

    @FunctionAdd(name="BetterMinecraft",alias="Better Minecraft",category=Category.Render,description="Плавное появление игровых экранов")
    public static final class BetterMinecraft extends Function {
        public final SliderSetting duration=new SliderSetting("Animation (ms)",180f,60f,500f,10f);
        public final BooleanSetting inventoryOnly=new BooleanSetting("Inventory only",false);
        public BetterMinecraft() { addSettings(duration,inventoryOnly); }
    }

    @FunctionAdd(name="ClickGui",alias="ClickGui",category=Category.Render,description="Оформление меню клиента (открывается Right Shift)")
    public static final class ClickGui extends Function {
        public final SliderSetting duration=new SliderSetting("Animation (ms)",200f,60f,500f,10f);
        public final SliderSetting background=new SliderSetting("Background opacity",140f,0f,220f,5f);
        public final BooleanSetting blur=new BooleanSetting("Background blur",false);
        public ClickGui() { addSettings(duration,background,blur); }
    }
}
