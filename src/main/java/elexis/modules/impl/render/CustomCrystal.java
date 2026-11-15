package elexis.modules.impl.render;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.modules.api.*;
import elexis.modules.api.settings.impl.*;
import elexis.modules.impl.render.javelin.crystalhands.*;
import net.minecraft.util.Identifier;
import java.util.Arrays;

/** Javelin crystal model controls, animated UV shaders and astral model explosions. */
@FunctionAdd(name="CustomCrystal", alias="CustomCrystal", category=Category.Render, description="Размер, текстуры, шейдеры и астральные модели кристаллов")
public final class CustomCrystal extends Function {
    private static CustomCrystal instance;
    public final SliderSetting size = new SliderSetting("Размер", 1.0F, 0.2F, 3.0F, 0.05F);
    public final SliderSetting spinSpeed = new SliderSetting("Скорость вращения", 1.0F, 0.0F, 4.0F, 0.05F);
    public final SliderSetting bobHeight = new SliderSetting("Высота покачивания", 1.0F, 0.0F, 3.0F, 0.05F);
    public final BooleanSetting showBase = new BooleanSetting("Подставка", true);
    public final BooleanSetting showBeam = new BooleanSetting("Луч", true);
    public final ModeSetting texture = new ModeSetting("Текстура", "Ванильная", "Ванильная", "Своя", "Шейдер", "Астральная");
    public final ModeSetting customTexture = new ModeSetting("Файл", "mellstroy", "mellstroy", "mellstroy_shock", "aqua", "ember", "void", "prism");
    public final ModeSetting shaderMode = new ModeSetting("Шейдер", "Туманность", "Туманность", "Электроразряд", "Северное сияние", "Сетка", "Магма", "Голограмма", "Сингулярность");
    public final ModeSetting shaderStyle = new ModeSetting("Стиль заливки", "Шейдер", "Шейдер", "Заливка");
    public final ModeSetting shaderAnimation = new ModeSetting("Анимация шейдера", "Вращение", "Вращение", "Пульсация", "Сканирование", "Вспышки");
    public final SliderSetting shaderSpeed = new SliderSetting("Скорость шейдера", 1.0F, 0.1F, 4.0F, 0.1F);
    public final SliderSetting shaderOpacity = new SliderSetting("Прозрачность", 85.0F, 10.0F, 100.0F, 1.0F);
    public final ModeSetting shaderColor = new ModeSetting("Цвет шейдера", "Тема", "Тема", "Свой");
    public final ColorSetting customColor = new ColorSetting("Свой цвет", -7504649);
    public final ColorSetting astralColor = new ColorSetting("Цвет астрала", -48395);
    public final ModeSetting astralShape = new ModeSetting("Подрежим астрала", "Классика", Arrays.stream(CrystalShape.values()).map(v -> v.label).toArray(String[]::new));
    public final ModeSetting astralAnimation = new ModeSetting("Анимация астрала", "Вращение", Arrays.stream(CrystalAnimation.values()).map(v -> v.label).toArray(String[]::new));
    public final SliderSetting astralSpeed = new SliderSetting("Скорость эффектов", 1.0F, 0.0F, 3.0F, 0.05F);
    public final SliderSetting astralLineWidth = new SliderSetting("Толщина линий", 1.0F, 0.5F, 2.0F, 0.05F);
    public final SliderSetting astralBrightness = new SliderSetting("Яркость астрала", 1.05F, 0.2F, 1.5F, 0.05F);
    public final BooleanSetting astralThroughWalls = new BooleanSetting("Астрал сквозь стены", true);
    public final BooleanSetting astralExplosion = new BooleanSetting("Анимация взрыва", true);
    public final ModeSetting astralExplosionType = new ModeSetting("Эффект взрыва", "Подъём", Arrays.stream(CrystalExplosion.values()).map(v -> v.label).toArray(String[]::new));
    public final SliderSetting astralExplosionTime = new SliderSetting("Длительность взрыва", 0.85F, 0.3F, 1.5F, 0.05F);

    public CustomCrystal() {
        instance = this;
        addSettings(size, spinSpeed, bobHeight, showBase, showBeam, texture, customTexture, shaderMode, shaderStyle, shaderAnimation, shaderSpeed, shaderOpacity, shaderColor, customColor, astralColor, astralShape, astralAnimation, astralSpeed, astralLineWidth, astralBrightness, astralThroughWalls, astralExplosion, astralExplosionType, astralExplosionTime);
        this.customTexture.setVisible(() -> this.texture.is("Своя"));
        this.shaderStyle.setVisible(() -> this.texture.is("Шейдер"));
        this.shaderMode.setVisible(() -> this.texture.is("Шейдер") && this.shaderStyle.is("Шейдер"));
        this.shaderAnimation.setVisible(() -> this.texture.is("Шейдер") && this.shaderStyle.is("Шейдер"));
        this.shaderSpeed.setVisible(() -> this.texture.is("Шейдер") && this.shaderStyle.is("Шейдер"));
        this.shaderOpacity.setVisible(() -> this.texture.is("Шейдер"));
        this.shaderColor.setVisible(() -> this.texture.is("Шейдер") && (this.shaderStyle.is("Заливка") || !this.shaderMode.is("Магма")));
        this.customColor
                .setVisible(() -> this.texture.is("Шейдер") && this.shaderColor.is("Свой") && (this.shaderStyle.is("Заливка") || !this.shaderMode.is("Магма")));
        this.astralColor.setVisible(() -> this.texture.is("Астральная"));
        this.astralShape.setVisible(() -> this.texture.is("Астральная"));
        this.astralAnimation.setVisible(() -> this.texture.is("Астральная"));
        this.astralSpeed.setVisible(() -> this.texture.is("Астральная"));
        this.astralLineWidth.setVisible(() -> this.texture.is("Астральная"));
        this.astralBrightness.setVisible(() -> this.texture.is("Астральная"));
        this.astralThroughWalls.setVisible(() -> this.texture.is("Астральная"));
        this.astralExplosion.setVisible(() -> this.texture.is("Астральная") || this.texture.is("Шейдер"));
        this.astralExplosionType.setVisible(() -> (this.texture.is("Астральная") || this.texture.is("Шейдер")) && this.astralExplosion.get());
        this.astralExplosionTime.setVisible(() -> (this.texture.is("Астральная") || this.texture.is("Шейдер")) && this.astralExplosion.get());
        this.showBase.setVisible(() -> !this.texture.is("Астральная"));
    }

    /** Алиас для {@link #astralOptions()} — используется CrystalEffects. */
    public static CrystalOptions astralCrystalOptions() {
        return astralOptions();
    }

    @EventHandler public void tick(EventUpdate.Slow event) {
        if (effectsActive()) CrystalEffects.tick(); else CrystalEffects.clear();
    }
    @EventHandler public void render(EventRender.World event) {
        if (texture.is("Шейдер")) CrystalShaderTexture.update();
        if (effectsActive()) CrystalEffects.render(event);
    }
    @Override public void onDisable() { CrystalEffects.clear(); super.onDisable(); }
    public static CustomCrystal get() { return instance; }
    public static boolean on() { return instance != null && instance.isState(); }
    public static float scale() { return on() ? instance.size.get() : 1f; }
    public static float spin() { return on() ? instance.spinSpeed.get() : 1f; }
    public static float bob() { return on() ? instance.bobHeight.get() : 1f; }
    public static boolean base() { return !on() || instance.showBase.get(); }
    public static boolean beam() { return !on() || instance.showBeam.get(); }
    public static boolean shaderActive() { return on() && instance.texture.is("Шейдер"); }
    public static boolean astralActive() { return on() && instance.texture.is("Астральная") && !CrystalAstralTexture.failed(); }
    public static boolean effectsActive() { return shaderActive() || astralActive(); }
    public static int astralRgb() { return on() ? instance.astralColor.get() & 0xFFFFFF : 0xFF42F5; }
    public static Identifier textureOverride() {
        if (!on()) return null;
        if (instance.texture.is("Своя")) return Identifier.of("javelin", "textures/crystal/" + instance.customTexture.get() + ".png");
        return shaderActive() ? CrystalShaderTexture.identifier() : null;
    }
    public static CrystalOptions astralOptions() {
        CustomCrystal c = instance;
        if (c == null) {
            // Модуль ещё не инициализирован — возвращаем безопасные дефолты,
            // чтобы CrystalEffects не падал с NPE.
            return new CrystalOptions(CrystalShape.CLASSIC, CrystalAnimation.ROTATION,
                    1f, 1f, 1f, false, false, CrystalExplosion.RISE, 0.85f);
        }
        boolean astral = astralActive();
        return new CrystalOptions(CrystalShape.from(c.astralShape.get()), CrystalAnimation.from(c.astralAnimation.get()),
                astral ? c.astralSpeed.get() : c.shaderSpeed.get(), c.astralLineWidth.get(),
                astral ? c.astralBrightness.get() : 1f, astral && c.astralThroughWalls.get(),
                c.astralExplosion.get(), CrystalExplosion.from(c.astralExplosionType.get()), c.astralExplosionTime.get());
    }
    public static String shaderModeName() { return on() && !instance.shaderStyle.is("Заливка") ? instance.shaderMode.get() : "Flat"; }
    public static String shaderAnimationName() { return on() ? instance.shaderAnimation.get() : "Вращение"; }
    public static float shaderSpeedValue() { return on() ? instance.shaderSpeed.get() : 1f; }
    public static float shaderOpacityValue() { return on() ? instance.shaderOpacity.get()/100f : 1f; }
    public static int shaderRgb() { return on() && instance.shaderColor.is("Свой") ? instance.customColor.get() : ThemeColorUtils.getIconColor(0,0); }
}