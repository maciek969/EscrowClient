package elexis.modules.impl.render.javelin.crystalhands;
import elexis.modules.api.settings.impl.*;
import elexis.modules.api.settings.api.Setting;
import java.util.Arrays;
public final class HandsGlassSettings {
   public final BooleanSetting enableBlur = new BooleanSetting("Стекло · Блюр", true);
   public final SliderSetting blurRadius = new SliderSetting("Стекло · Сила размытия", 2.5F, 1.0F, 5.0F, 0.1F);
   public final SliderSetting blurIterations = new SliderSetting("Стекло · Качество", 3.0F, 1.0F, 8.0F, 1.0F);
   public final SliderSetting saturation = new SliderSetting("Стекло · Насыщенность", 1.0F, 0.0F, 2.0F, 0.1F);
   public final SliderSetting tintIntensity = new SliderSetting("Стекло · Сила оттенка", 0.2F, 0.0F, 1.0F, 0.01F);
   public final SliderSetting edgeSoftness = new SliderSetting("Стекло · Сглаживание краёв", 0.0F, 0.0F, 28.0F, 1.0F);
   public final ModeSetting colorMode = new ModeSetting("Стекло · Цвет", "Theme", "Theme", "Custom");
   public final ColorSetting tintColor = new ColorSetting("Стекло · Свой цвет", -16711681);
   public final BooleanSetting enableIce = new BooleanSetting("Стекло · Искажение", false);
   public final SliderSetting iceIntensity = new SliderSetting("Стекло · Сила искажения", 1.0F, 0.0F, 2.0F, 0.05F);
   public final SliderSetting refractReach = new SliderSetting("Стекло · Ширина искажения", 25.0F, 0.0F, 80.0F, 1.0F);
   public final BooleanSetting enableFire = new BooleanSetting("Стекло · Огонь", false);
   public final SliderSetting fireIntensity = new SliderSetting("Стекло · Сила огня", 1.18F, 0.2F, 2.0F, 0.02F);
   public final SliderSetting fireRadius = new SliderSetting("Стекло · Размер пламени", 15.0F, 4.0F, 40.0F, 1.0F);
   public final SliderSetting fireSpeed = new SliderSetting("Стекло · Скорость огня", 1.1F, 0.2F, 3.0F, 0.05F);
   public final SliderSetting fireHeight = new SliderSetting("Стекло · Высота дыма", 28.0F, 5.0F, 56.0F, 1.0F);
   public final SliderSetting fireTrail = new SliderSetting("Стекло · Сила дыма", 1.16F, 0.0F, 2.0F, 0.02F);
   public final ModeSetting fireColorMode = new ModeSetting("Стекло · Цвет огня", "Предмет", "Предмет", "Тема", "Свой");
   public final ColorSetting fireCustomColor = new ColorSetting("Стекло · Свой цвет огня", -32730);
   public final BooleanSetting fireBurnThrough = new BooleanSetting("Стекло · Прожиг", false);
   public final BooleanSetting enableEdgeGlow = new BooleanSetting("Стекло · Свечение краёв", true);
   public final SliderSetting edgeGlowIntensity = new SliderSetting("Стекло · Сила свечения", 0.2F, 0.0F, 1.0F, 0.01F);
   public final ModeSetting edgeGlowColorMode = new ModeSetting("Стекло · Цвет свечения", "Theme", "Theme", "Custom");
   public final ColorSetting edgeGlowCustom = new ColorSetting("Стекло · Свой цвет свечения", -16711681);
   public final BooleanSetting shimmer = new BooleanSetting("Стекло · Шиммер", true);
   public final SliderSetting shimmerWidth = new SliderSetting("Стекло · Ширина шиммера", 0.04F, 0.01F, 0.15F, 0.01F);
   public final SliderSetting shimmerPeriod = new SliderSetting("Стекло · Период шиммера", 5.0F, 1.0F, 15.0F, 0.5F);

    public HandsGlassSettings() {
      this.blurRadius.setVisible(this.enableBlur::getValue);
      this.blurIterations.setVisible(this.enableBlur::getValue);
      this.saturation.setVisible(this.enableBlur::getValue);
      this.tintIntensity.setVisible(this.enableBlur::getValue);
      this.edgeSoftness.setVisible(this.enableBlur::getValue);
      this.tintColor.setVisible(() -> this.colorMode.is("Custom"));
      this.iceIntensity.setVisible(this.enableIce::getValue);
      this.refractReach.setVisible(this.enableIce::getValue);
      this.fireIntensity.setVisible(this.enableFire::getValue);
      this.fireRadius.setVisible(this.enableFire::getValue);
      this.fireSpeed.setVisible(this.enableFire::getValue);
      this.fireHeight.setVisible(this.enableFire::getValue);
      this.fireTrail.setVisible(this.enableFire::getValue);
      this.fireColorMode.setVisible(this.enableFire::getValue);
      this.fireCustomColor.setVisible(() -> this.enableFire.get() && this.fireColorMode.is("Свой"));
      this.fireBurnThrough.setVisible(this.enableFire::getValue);
      this.edgeGlowIntensity.setVisible(this.enableEdgeGlow::getValue);
      this.edgeGlowColorMode.setVisible(this.enableEdgeGlow::getValue);
      this.edgeGlowCustom.setVisible(() -> this.enableEdgeGlow.get() && this.edgeGlowColorMode.is("Custom"));
      this.shimmer.setVisible(this.enableEdgeGlow::getValue);
      this.shimmerWidth.setVisible(() -> this.enableEdgeGlow.get() && this.shimmer.get());
      this.shimmerPeriod.setVisible(() -> this.enableEdgeGlow.get() && this.shimmer.get());
    }
    public Setting<?>[] settings() { return new Setting<?>[]{enableBlur, blurRadius, blurIterations, saturation, tintIntensity, edgeSoftness, colorMode, tintColor, enableIce, iceIntensity, refractReach, enableFire, fireIntensity, fireRadius, fireSpeed, fireHeight, fireTrail, fireColorMode, fireCustomColor, fireBurnThrough, enableEdgeGlow, edgeGlowIntensity, edgeGlowColorMode, edgeGlowCustom, shimmer, shimmerWidth, shimmerPeriod}; }
    public boolean contains(Setting<?> setting) { return Arrays.asList(settings()).contains(setting); }
}
