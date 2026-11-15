package yabloko.client.hud.elements.component;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.nexusguard.protection.annotations.Native;
import yabloko.yabloko;
import yabloko.base.animations.base.Animation;
import yabloko.base.animations.base.Easing;
import yabloko.base.font.Fonts;
import yabloko.base.theme.Theme;
import yabloko.client.hud.elements.draggable.DraggableHudElement;
import yabloko.client.modules.api.setting.impl.BooleanSetting;
import yabloko.utility.render.display.base.BorderRadius;
import yabloko.utility.render.display.base.CustomDrawContext;
import yabloko.utility.render.display.base.color.ColorRGBA;
import yabloko.utility.render.display.shader.DrawUtil;

public class PotionsComponent extends DraggableHudElement {
   private final BooleanSetting s1 = new BooleanSetting("1", true);
   private final BooleanSetting s2 = new BooleanSetting("2", true);
   private final BooleanSetting s3 = new BooleanSetting("3", true);
   private final BooleanSetting s4 = new BooleanSetting("4", true);
   private final BooleanSetting s5 = new BooleanSetting("5", true);
   private final BooleanSetting s6 = new BooleanSetting("6", true);
   private final BooleanSetting s7 = new BooleanSetting("7", true);
   private final Animation widthAnimation;
   private final Animation xLine;
   private final Animation alpha;
   private final List<PotionsComponent.PotionItem> potionItems;

   public PotionsComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
      this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
      this.xLine = new Animation(170L, Easing.SINE_OUT);
      this.alpha = new Animation(200L, Easing.CUBIC_OUT);
      this.potionItems = new CopyOnWriteArrayList();
   }

   @Native
   public void render(CustomDrawContext ctx) {
      if (mc.player != null) {
         this.updatePotions();
         float posX = this.getX();
         float posY = this.getY();
         float defaultWidth = 76.0F;
         float height = 18.0F;
         this.potionItems.sort(Comparator.comparing((pi) -> pi.name));
         boolean isFound = false;
         float durationWidth = 0.0F;

         Iterator<PotionsComponent.PotionItem> iterator = this.potionItems.iterator();
         while(iterator.hasNext()) {
            PotionsComponent.PotionItem item = iterator.next();
            item.animation.update(item.active);
            if (item.animation.getValue() != 0.0F) {
               String duration = this.formatDuration(item.durationTicks);
               durationWidth = Math.max(durationWidth, Fonts.REGULAR.getWidth(duration, 6.75F));
               height += 12.0F * item.animation.getValue();
               this.alpha.update(1.0F);
               isFound = true;
            }
         }

         this.xLine.update(durationWidth);
         if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
            this.alpha.update(0.0F);
         }

         if (mc.currentScreen instanceof ChatScreen) {
            this.alpha.update(1.0F);
         }

         Theme theme = yabloko.getInstance().getThemeManager().getCurrentTheme();
         DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, posY, this.widthAnimation.getValue(), height, BorderRadius.all(4.0F), new ColorRGBA(18, 21, 27, 255.0F * this.alpha.getValue()), new ColorRGBA(18, 21, 27, 255.0F * this.alpha.getValue()), new ColorRGBA(37, 42, 53, 255.0F * this.alpha.getValue()), new ColorRGBA(37, 42, 53, 255.0F * this.alpha.getValue()));
         ctx.drawText(Fonts.ICONS2.getFont(7.0F), "\uf6e1", posX + 5.5F, posY + 6.0F, theme.getColor().withAlpha(255.0F * this.alpha.getValue()));
         ctx.drawText(Fonts.MEDIUM.getFont(7.25F), "Potions", posX + 18.0F, posY + 5.75F, (new ColorRGBA(-1)).withAlpha(255.0F * this.alpha.getValue()));
         posY += 18.0F;

         if (this.s1.isEnabled()) {
            iterator = this.potionItems.iterator();
            while(iterator.hasNext()) {
               PotionsComponent.PotionItem item = iterator.next();
               if (item.animation.getValue() != 0.0F) {
                  String name = I18n.translate(item.name, new Object[0]);
                  String amp = this.getAmplifierText(item.amplifier);
                  String duration = this.formatDuration(item.durationTicks);
                  Identifier icon = this.getEffectIcon((StatusEffect)item.effect.getEffectType().value());
                  float rowY = posY + item.animation.getValue() * 3.0F - 3.0F;
                  float rowAlpha = item.animation.getValue() * this.alpha.getValue();
                  float elementsWidth = Fonts.REGULAR.getWidth(name, 6.75F) + Fonts.REGULAR.getWidth(amp, 6.75F) + Fonts.REGULAR.getWidth(duration, 6.75F) + 48.0F;

                  if (this.s4.isEnabled()) {
                     ctx.drawTexture(icon, posX + 6.0F, rowY + 2.75F, 7.0F, 7.0F, ColorRGBA.WHITE.withAlpha(rowAlpha * 255.0F));
                  }

                  if (this.s5.isEnabled()) {
                     ctx.drawText(Fonts.REGULAR.getFont(6.75F), name, posX + 16.0F, rowY + 3.75F, (new ColorRGBA(-1)).withAlpha(rowAlpha * 255.0F));
                  }

                  if (Integer.parseInt(amp) > 0 && this.s6.isEnabled()) {
                     ctx.drawText(Fonts.REGULAR.getFont(6.5F), amp, posX + Fonts.REGULAR.getWidth(name, 6.75F) + 18.0F, rowY + 3.75F, theme.getColor().withAlpha(rowAlpha * 255.0F));
                  }

                  if (this.s7.isEnabled()) {
                     ctx.drawText(Fonts.REGULAR.getFont(6.5F), duration, posX + this.widthAnimation.getValue() - 7.0F - Fonts.REGULAR.getWidth(duration, 6.75F), rowY + 3.75F, (new ColorRGBA(-1)).withAlpha(rowAlpha * 255.0F));
                  }

                  if (elementsWidth > defaultWidth) {
                     defaultWidth = elementsWidth;
                  }

                  posY += 12.0F * item.animation.getValue();
               }
            }
         }

         this.widthAnimation.update(defaultWidth);
         this.width = this.widthAnimation.getValue();
         this.height = height;
      }
   }

   private String getAmplifierText(int amplifier) {
      return String.valueOf(amplifier + 1);
   }

   private String formatDuration(int durationTicks) {
      int totalSeconds = durationTicks / 20;
      int minutes = totalSeconds / 60;
      int seconds = totalSeconds % 60;
      return String.format("%02d:%02d", minutes, seconds);
   }

   private Identifier getEffectIcon(StatusEffect effect) {
      String id = effect.getTranslationKey().replace("effect.minecraft.", "").replace("effect.", "");
      return Identifier.of("minecraft", "textures/mob_effect/" + id + ".png");
   }

   @Native
   public void updatePotions() {
      if (mc.player != null) {
         Map<String, StatusEffectInstance> currentEffects = mc.player.getStatusEffects().stream().collect(Collectors.toMap((e) -> Text.translatable(e.getTranslationKey()).getString() + ":" + e.getAmplifier(), (e) -> e, (e1, e2) -> e1));
         this.potionItems.forEach((item) -> {
            String key = item.name + ":" + item.amplifier;
            StatusEffectInstance effect = currentEffects.get(key);
            if (effect != null) {
               item.durationTicks = effect.getDuration();
               if (!item.active) {
                  item.animation.setValue(1.0F);
               }

               item.active = true;
               currentEffects.remove(key);
            } else {
               item.active = false;
            }
         });
         currentEffects.forEach((key, effect) -> this.potionItems.add(new PotionsComponent.PotionItem(Text.translatable(effect.getTranslationKey()).getString(), effect.getAmplifier(), effect.getDuration(), effect)));
         this.potionItems.removeIf((item) -> !item.active && item.animation.getValue() == 0.0F);
      }
   }

   private static class PotionItem {
      String name;
      int amplifier;
      int durationTicks;
      boolean active;
      StatusEffectInstance effect;
      Animation animation;

      PotionItem(String name, int amplifier, int durationTicks, StatusEffectInstance effect) {
         this.animation = new Animation(250L, Easing.CUBIC_OUT);
         this.name = name;
         this.amplifier = amplifier;
         this.durationTicks = durationTicks;
         this.active = true;
         this.effect = effect;
      }
   }

   private static class PotionModule {
      private final Animation animation;
      private StatusEffectInstance effect;

      public PotionModule(StatusEffectInstance effect) {
         this.animation = new Animation(150L, 0.01F, Easing.QUAD_IN_OUT);
         this.effect = effect;
      }

      public boolean isDelete() {
         return this.animation.getValue() == 0.0F;
      }
   }
}
