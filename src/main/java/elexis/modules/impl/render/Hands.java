package elexis.modules.impl.render;

import elexis.client.utils.render.color.basic.ThemeColorUtils;
import elexis.modules.api.*;
import elexis.modules.api.settings.api.Setting;           // <-- правильный пакет
import elexis.modules.api.settings.impl.*;
import elexis.modules.impl.render.javelin.crystalhands.*;

import java.util.function.Supplier;                       // <-- Supplier<Boolean>, не BooleanSupplier

/** Javelin first-person hand fill, shaders, mirror, glass, bloom and temporal trails. */
@FunctionAdd(name="Hands", alias="Hands", category=Category.Render, description="Эффекты рук Javelin")
public final class Hands extends Function {
   public final ModeSetting effect = new ModeSetting("Эффект рук", "Hands", "Hands", "BagaRuki", "ShaderHands");
   public final HandsGlassSettings glass = new HandsGlassSettings();
   public long startTime;
   public final ModeSetting mode = new ModeSetting("Режим", "Nothing", "Nothing", "Заливка", "Шейдер", "Зеркало", "Шейдер + Зеркало");

   public final BooleanSetting themeColor = new BooleanSetting("Авто цвет под тему", true);
   public final BooleanSetting itemColor = new BooleanSetting("Авто цвет под предмет", false);
   public final SliderSetting mirrorMix = new SliderSetting("Смешивание", 0.5F, 0.0F, 1.0F, 0.01F);
   public final SliderSetting mirrorDistortion = new SliderSetting("Искажение зеркала", 0.0F, 0.0F, 1.0F, 0.05F);
   public final SliderSetting mirrorBlur = new SliderSetting("Сила размытия", 1.0F, 0.1F, 3.0F, 0.05F);
   public final BooleanSetting rainbow = new BooleanSetting("Радужная", false);
   public final SliderSetting rainbowSpeed = new SliderSetting("Скорость радуги", 0.4F, 0.0F, 2.0F, 0.05F);
   public final SliderSetting rainbowScale = new SliderSetting("Масштаб радуги", 1.0F, 0.2F, 3.0F, 0.1F);
   public final ColorSetting fillColor = new ColorSetting("Цвет заливки", -48060);
   public final SliderSetting fillAlpha = new SliderSetting("Прозрачность заливки", 0.8F, 0.0F, 1.0F, 0.05F);
   public final BooleanSetting fillShading = new BooleanSetting("Сохранить тени", true);
   public final SliderSetting fillShadow = new SliderSetting("Сила теней", 0.3F, 0.0F, 1.0F, 0.05F);
   public final ModeSetting shaderStyle = new ModeSetting("Стиль шейдера", "Шейдер", "Шейдер", "HandShader", "Туманность", "Космос", "Градиент", "Homie");
   public final ColorSetting shaderColor = new ColorSetting("Цвет шейдера 1", -3632385);
   public final BooleanSetting secondColor = new BooleanSetting("Второй цвет", false);
   public final ColorSetting shaderColor2 = new ColorSetting("Цвет шейдера 2", -11534136);
   public final SliderSetting shaderSpeed = new SliderSetting("Скорость шейдера", 1.5F, 0.05F, 2.5F, 0.05F);
   public final SliderSetting shaderScale = new SliderSetting("Масштаб шейдера", 1.5F, 0.2F, 5.0F, 0.1F);
   public final SliderSetting shaderGlow = new SliderSetting("Свечение шейдера", 1.2F, 0.0F, 3.0F, 0.05F);
   public final SliderSetting quality = new SliderSetting("Качество шейдера", 2.0F, 1.0F, 3.0F, 1.0F);
   public final BooleanSetting stars = new BooleanSetting("Звёзды", true);
   public final SliderSetting shaderAlpha = new SliderSetting("Прозрачность шейдера", 1.0F, 0.0F, 1.0F, 0.05F);
   public final BooleanSetting shaderShading = new BooleanSetting("Сохранить тени шейдера", false);
   public final SliderSetting shaderShadow = new SliderSetting("Сила теней шейдера", 0.5F, 0.0F, 1.0F, 0.05F);
   public final BooleanSetting glow = new BooleanSetting("Глов", true);
   public final SliderSetting glowBlur = new SliderSetting("Размытие", 3.0F, 1.0F, 5.0F, 1.0F);
   public final SliderSetting brightness = new SliderSetting("Яркость", 1.0F, 0.5F, 5.0F, 0.1F);
   public final ColorSetting glowColor = new ColorSetting("Цвет глова 1", -11713);
   public final ColorSetting glowColor2 = new ColorSetting("Цвет глова 2", -54784);
   public final SliderSetting innerGlow = new SliderSetting("Внутреннее свечение", 0.65F, 0.0F, 1.5F, 0.05F);
   public final BooleanSetting outline = new BooleanSetting("Обводка", true);
   public final ColorSetting outlineColor = new ColorSetting("Цвет обводки", -1);
   public final SliderSetting outlineThickness = new SliderSetting("Толщина обводки", 1.0F, 0.5F, 2.0F, 0.5F);
   public final BooleanSetting trail = new BooleanSetting("Шлейф", true);
   public final ModeSetting trailMode = new ModeSetting("Режим шлейфа", "Обычный", "Обычный", "Энергия", "Ленты");
   public final SliderSetting energyLifetime = new SliderSetting("Длина энергии", 1.0F, 0.4F, 2.0F, 0.05F);
   public final SliderSetting energyThickness = new SliderSetting("Яркость энергии", 1.35F, 0.5F, 2.5F, 0.05F);
   public final SliderSetting energyTurbulence = new SliderSetting("Движение энергии", 1.3F, 0.0F, 2.0F, 0.05F);
   public final SliderSetting energySmoothness = new SliderSetting("Пульсация энергии", 1.0F, 0.0F, 2.0F, 0.05F);
   public final SliderSetting energyInertia = new SliderSetting("Шлейф за камерой", 0.05F, 0.0F, 2.0F, 0.05F);
   public final SliderSetting ribbonLifetime = new SliderSetting("Длина лент", 0.3F, 0.15F, 3.0F, 0.05F);
   public final SliderSetting ribbonWidth = new SliderSetting("Ширина лент", 0.55F, 0.25F, 1.5F, 0.05F);
   public final SliderSetting ribbonIntensity = new SliderSetting("Яркость лент", 1.0F, 0.0F, 2.5F, 0.05F);
   public final SliderSetting ribbonInertia = new SliderSetting("Сила лент за камерой", 0.1F, 0.0F, 2.0F, 0.05F);
   public final SliderSetting ribbonRise = new SliderSetting("Подъём лент", 0.32F, 0.0F, 1.0F, 0.01F);
   public final SliderSetting ribbonWobble = new SliderSetting("Изгиб лент", 2.0F, 0.0F, 2.0F, 0.05F);
   public final SliderSetting ribbonSpeed = new SliderSetting("Скорость волн", 1.5F, 0.0F, 3.0F, 0.05F);
   public final BooleanSetting ribbonSwingResponse = new BooleanSetting("Ленты при ударе", true);
   public final SliderSetting ribbonSwingLifetime = new SliderSetting("Длина при ударе", 0.0F, 0.15F, 3.0F, 0.05F);
   public final SliderSetting ribbonSwingPower = new SliderSetting("Сила при ударе", 1.15F, 0.0F, 2.5F, 0.05F);
   public final BooleanSetting ribbonEatingResponse = new BooleanSetting("Ленты при еде", true);
   public final SliderSetting ribbonEatingLifetime = new SliderSetting("Длина при еде", 0.65F, 0.15F, 3.0F, 0.05F);
   public final SliderSetting ribbonEatingPower = new SliderSetting("Сила при еде", 0.7F, 0.0F, 2.5F, 0.05F);
   public final SliderSetting normalFade = new SliderSetting("Скорость затухания", 0.04F, 0.002F, 0.2F, 0.002F);
   public final SliderSetting normalRise = new SliderSetting("Подъём", 0.2F, 0.0F, 1.5F, 0.05F);
   public final SliderSetting normalSway = new SliderSetting("Качание", 0.0F, 0.0F, 0.2F, 0.005F);
   public final SliderSetting normalInertia = new SliderSetting("Шлейф от камеры", 0.0F, 0.0F, 2.0F, 0.05F);
   public final SliderSetting normalTurbulence = new SliderSetting("Турбулентность", 0.02F, 0.0F, 0.6F, 0.01F);
   public final SliderSetting normalFlicker = new SliderSetting("Мерцание", 0.2F, 0.0F, 0.2F, 0.01F);
   public final BooleanSetting hitFade = new BooleanSetting("Сдув при ударе", false);
   public final SliderSetting hitFadeMul = new SliderSetting("Сила сдува", 4.0F, 1.0F, 10.0F, 0.5F);

   public Hands() {
      addSettings(effect, mode, themeColor, itemColor, mirrorMix, mirrorDistortion, mirrorBlur, rainbow, rainbowSpeed, rainbowScale, fillColor, fillAlpha, fillShading, fillShadow, shaderStyle, shaderColor, secondColor, shaderColor2, shaderSpeed, shaderScale, shaderGlow, quality, stars, shaderAlpha, shaderShading, shaderShadow, glow, glowBlur, brightness, glowColor, glowColor2, innerGlow, outline, outlineColor, outlineThickness, trail, trailMode, energyLifetime, energyThickness, energyTurbulence, energySmoothness, energyInertia, ribbonLifetime, ribbonWidth, ribbonIntensity, ribbonInertia, ribbonRise, ribbonWobble, ribbonSpeed, ribbonSwingResponse, ribbonSwingLifetime, ribbonSwingPower, ribbonEatingResponse, ribbonEatingLifetime, ribbonEatingPower, normalFade, normalRise, normalSway, normalInertia, normalTurbulence, normalFlicker, hitFade, hitFadeMul);
      addSettings(glass.settings());

      // Задаём видимость каждой настройки ОДИН раз, с учётом:
      //  - glass-режима (BagaRuki) — все обычные настройки скрыты;
      //  - legacy shader (ShaderHands) — скрыты mode и shaderStyle;
      //  - базового условия конкретной настройки.
      for (var setting : getSettings()) {
         if (setting == effect || glass.contains(setting)) continue;
         Supplier<Boolean> base = baseVisible(setting);
         boolean hideOnLegacy = (setting == mode || setting == shaderStyle);
         setting.setVisible(() -> !isGlassMode()
                 && (!isLegacyShaderMode() || !hideOnLegacy)
                 && base.get());
      }
      for (var setting : glass.settings()) {
         setting.setVisible(this::isGlassMode);
      }
   }

   /** Исходное (базовое) условие видимости настройки, без учёта glass/legacy-режимов. */
   private Supplier<Boolean> baseVisible(Setting<?> setting) {
      if (setting == mirrorMix || setting == mirrorDistortion || setting == mirrorBlur) return this::isMirrorMode;
      if (setting == rainbow) return this::isFillMode;
      if (setting == rainbowSpeed || setting == rainbowScale)
         return () -> this.isFillMode() && this.rainbow.getValue();
      if (setting == fillColor)
         return () -> this.isFillMode() && !this.rainbow.getValue() && !this.themeColor.getValue();
      if (setting == fillAlpha || setting == fillShading) return this::isFillMode;
      if (setting == fillShadow)
         return () -> this.isFillMode() && this.fillShading.getValue();
      if (setting == shaderStyle) return this::isShaderMode;
      if (setting == shaderColor)
         return () -> this.isShaderMode() && !this.themeColor.getValue();
      if (setting == secondColor) return this::isShaderMode;
      if (setting == shaderColor2)
         return () -> this.isShaderMode() && this.secondColor.getValue() && !this.themeColor.getValue();
      if (setting == shaderSpeed || setting == shaderScale || setting == shaderGlow
              || setting == stars || setting == shaderAlpha || setting == shaderShading)
         return this::isShaderMode;
      if (setting == quality)
         return () -> this.isShaderMode() && !this.shaderStyle.is("Градиент");
      if (setting == shaderShadow)
         return () -> this.isShaderMode() && this.shaderShading.getValue();
      if (setting == glowBlur || setting == brightness || setting == innerGlow)
         return this.glow::getValue;
      if (setting == glowColor || setting == glowColor2)
         return () -> this.glow.getValue() && !this.themeColor.getValue() && !this.itemColor.getValue();
      if (setting == outlineColor)
         return () -> this.outline.getValue() && !this.themeColor.getValue() && !this.itemColor.getValue();
      if (setting == outlineThickness) return this.outline::getValue;
      if (setting == trail) return this.glow::getValue;
      if (setting == trailMode) return this::isTrailEnabled;
      if (setting == energyLifetime || setting == energyThickness || setting == energyTurbulence
              || setting == energySmoothness || setting == energyInertia)
         return this::isEnergyTrail;
      if (setting == ribbonLifetime || setting == ribbonWidth || setting == ribbonIntensity
              || setting == ribbonInertia || setting == ribbonRise || setting == ribbonWobble
              || setting == ribbonSpeed || setting == ribbonSwingResponse || setting == ribbonEatingResponse)
         return this::isRibbonTrail;
      if (setting == ribbonSwingLifetime || setting == ribbonSwingPower)
         return () -> this.isRibbonTrail() && this.ribbonSwingResponse.getValue();
      if (setting == ribbonEatingLifetime || setting == ribbonEatingPower)
         return () -> this.isRibbonTrail() && this.ribbonEatingResponse.getValue();
      if (setting == normalFade || setting == normalRise || setting == normalSway
              || setting == normalInertia || setting == normalTurbulence || setting == normalFlicker
              || setting == hitFade)
         return this::isNormalTrail;
      if (setting == hitFadeMul)
         return () -> this.isNormalTrail() && this.hitFade.getValue();
      // mode, themeColor, itemColor и все прочие — всегда видимы (кроме glass/legacy-гейтов выше).
      return () -> true;
   }

   public boolean isGlassMode() {
      return this.effect.is("BagaRuki");
   }

   public boolean isLegacyShaderMode() {
      return this.effect.is("ShaderHands");
   }

   public void clearEffects() { HandsEffectRenderer.release(); }

   public float getRainbowSpeed() {
      return this.rainbowSpeed.get();
   }

   public int getQualitySamples() {
      int value = Math.round(this.quality.get());
      return value == 1 ? 3 : (value == 3 ? 8 : 5);
   }

   public int getShaderColor() {
      return this.themeColor.get() ? ThemeColorUtils.getIconColor(0, 0) : this.shaderColor.get();
   }

   public float getShaderGlow() {
      return this.shaderGlow.get();
   }

   public float getEnergySmoothness() {
      return this.energySmoothness.get();
   }

   public float getRibbonLifetime() {
      return this.ribbonLifetime.get();
   }

   public float getMirrorMix() {
      return this.mirrorMix.get();
   }

   public float getGlowBrightness() {
      return this.brightness.get();
   }

   public float getRibbonSwingLifetime() {
      return this.ribbonSwingLifetime.get();
   }

   public int getRenderMode() {
      if (this.isLegacyShaderMode()) {
         return 1;
      } else if (this.isNothingMode()) {
         return 4;
      } else {
         return this.isFillMode() ? 0 : (this.mode.is("Шейдер") ? 1 : (this.mode.is("Зеркало") ? 2 : 3));
      }
   }

   public int getShaderColor2() {
      if (this.themeColor.get()) {
         return this.secondColor.get() ? ThemeColorUtils.getIconColor(0, 90) : ThemeColorUtils.getIconColor(0, 0);
      } else {
         return this.secondColor.get() ? this.shaderColor2.get() : this.shaderColor.get();
      }
   }

   public float getRibbonEatingLifetime() {
      return this.ribbonEatingLifetime.get();
   }

   public int getFillColor() {
      return this.themeColor.get() ? ThemeColorUtils.getIconColor(0, 0) : this.fillColor.get();
   }

   public boolean isFillShading() {
      return this.fillShading.get();
   }

   public boolean isRibbonSwingResponse() {
      return this.ribbonSwingResponse.get();
   }

   public boolean isFillMode() {
      return !this.isLegacyShaderMode() && this.mode.is("Заливка");
   }

   public boolean isNothingMode() {
      return !this.isLegacyShaderMode() && this.mode.is("Nothing");
   }

   public boolean isOutlineEnabled() {
      return this.outline.get();
   }

   public boolean isShaderMode() {
      return this.isLegacyShaderMode() || this.mode.is("Шейдер") || this.isShaderMirrorMode();
   }

   public boolean isShaderShading() {
      return this.shaderShading.get();
   }

   public float getGlowBlur() {
      return this.glowBlur.get();
   }

   public boolean isEnergyTrail() {
      return this.isTrailEnabled() && this.trailMode.is("Энергия");
   }

   public float getEnergyThickness() {
      return this.energyThickness.get();
   }

   public float getShaderScale() {
      return this.shaderScale.get();
   }

   public float getRibbonEatingPower() {
      return this.ribbonEatingPower.get();
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.startTime = System.currentTimeMillis();
   }

   public float getNormalSway() {
      return this.normalSway.get();
   }

   public float getFillAlpha() {
      return this.fillAlpha.get();
   }

   public int getMirrorColor() {
      return ThemeColorUtils.getIconColor(0, 0);
   }

   public float getRibbonWobble() {
      return this.ribbonWobble.get();
   }

   public boolean isHitFade() {
      return this.hitFade.get();
   }

   public boolean isRibbonEatingResponse() {
      return this.ribbonEatingResponse.get();
   }

   public float getEnergyInertia() {
      return this.energyInertia.get();
   }

   public float getNormalInertia() {
      return this.normalInertia.get();
   }

   public float getNormalRise() {
      return this.normalRise.get();
   }

   public float getRibbonSwingPower() {
      return this.ribbonSwingPower.get();
   }

   public boolean isMirrorMode() {
      return !this.isLegacyShaderMode() && (this.mode.is("Зеркало") || this.isShaderMirrorMode());
   }

   public float getShaderSpeed() {
      return this.shaderSpeed.get();
   }

   public boolean isRibbonTrail() {
      return this.isTrailEnabled() && this.trailMode.is("Ленты");
   }

   public int getOutlineColor() {
      if (this.itemColor.get()) {
         return -1;
      } else {
         return this.themeColor.get() ? ThemeColorUtils.getIconColor(0, 0) : this.outlineColor.get();
      }
   }

   public float getRainbowScale() {
      return this.rainbowScale.get();
   }

   public float getMirrorDistortion() {
      return this.mirrorDistortion.get();
   }

   public int getShaderStyle() {
      if (this.isLegacyShaderMode()) {
         return 4;
      } else if (this.shaderStyle.is("HandShader")) {
         return 4;
      } else if (this.shaderStyle.is("Туманность")) {
         return 1;
      } else if (this.shaderStyle.is("Космос")) {
         return 2;
      } else if (this.shaderStyle.is("Градиент")) {
         return 3;
      } else {
         return this.shaderStyle.is("Homie") ? 5 : 0;
      }
   }

   public float getOutlineThickness() {
      return this.outlineThickness.get();
   }

   public float getNormalFade() {
      return this.normalFade.get();
   }

   public float getRibbonRise() {
      return this.ribbonRise.get();
   }

   public float getRibbonIntensity() {
      return this.ribbonIntensity.get();
   }

   public float getRibbonSpeed() {
      return this.ribbonSpeed.get();
   }

   public boolean isNormalTrail() {
      return this.isTrailEnabled() && this.trailMode.is("Обычный");
   }

   public int getGlowColor() {
      return this.themeColor.get() ? ThemeColorUtils.getIconColor(0, 0) : this.glowColor.get();
   }

   public float getEnergyLifetime() {
      return this.energyLifetime.get();
   }

   public float getMirrorBlur() {
      return this.mirrorBlur.get();
   }

   public float getNormalFlicker() {
      return this.normalFlicker.get();
   }

   public float getRibbonWidth() {
      return this.ribbonWidth.get();
   }

   public int getGlowColor2() {
      return this.themeColor.get() ? ThemeColorUtils.getIconColor(0, 90) : this.glowColor2.get();
   }

   public float getFillShadow() {
      return this.fillShadow.get();
   }

   public boolean isShaderMirrorMode() {
      return !this.isLegacyShaderMode() && this.mode.is("Шейдер + Зеркало");
   }

   public float getRibbonInertia() {
      return this.ribbonInertia.get();
   }

   public float getHitFadeMul() {
      return this.hitFadeMul.get();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.clearEffects();
   }

   public float getNormalTurbulence() {
      return this.normalTurbulence.get();
   }

   public boolean needsCapture() {
      return this.isState() && !this.isGlassMode() && (!this.isNothingMode() || this.glow.get() || this.outline.get());
   }

   public float getInnerGlow() {
      return this.innerGlow.get();
   }

   public float getAnimationTime() {
      return (float)(System.currentTimeMillis() - this.startTime) / 1000.0F;
   }

   public boolean isTrailEnabled() {
      return this.glow.get() && this.trail.get();
   }

   public float getShaderAlpha() {
      return this.shaderAlpha.get();
   }

   public boolean isItemColor() {
      return this.itemColor.get();
   }

   public boolean hasStars() {
      return this.stars.get();
   }

   public boolean isRainbow() {
      return this.rainbow.get();
   }

   public int getTrailMode() {
      return this.isRibbonTrail() ? 2 : (this.isEnergyTrail() ? 1 : 0);
   }

   public float getShaderShadow() {
      return this.shaderShadow.get();
   }

   public boolean isGlowEnabled() {
      return this.glow.get();
   }

   public float getEnergyTurbulence() {
      return this.energyTurbulence.get();
   }
}
