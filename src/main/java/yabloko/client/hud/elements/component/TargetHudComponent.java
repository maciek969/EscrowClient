package yabloko.client.hud.elements.component;

import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.nexusguard.protection.annotations.Native;
import yabloko.yabloko;
import yabloko.base.animations.base.Animation;
import yabloko.base.animations.base.Easing;
import yabloko.base.font.Fonts;
import yabloko.base.font.MsdfRenderer;
import yabloko.base.theme.Theme;
import yabloko.client.hud.elements.draggable.DraggableHudElement;
import yabloko.client.modules.impl.combat.Aura;
import yabloko.client.modules.impl.misc.NameProtect;
import yabloko.client.modules.impl.misc.ScoreboardHealth;
import yabloko.utility.game.player.PlayerIntersectionUtil;
import yabloko.utility.mixin.accessors.DrawContextAccessor;
import yabloko.utility.render.display.StencilUtil;
import yabloko.utility.render.display.base.BorderRadius;
import yabloko.utility.render.display.base.CustomDrawContext;
import yabloko.utility.render.display.base.color.ColorRGBA;
import yabloko.utility.render.display.shader.DrawUtil;

public class TargetHudComponent extends DraggableHudElement {
   private final Animation healthAnimation;
   private final Animation outdatedHealthAnimation;
   private final Animation gappleAnimation;
   private final Animation toggleAnimation;
   private final Animation toggleAnimationMetanoise;
   private LivingEntity target;

   public TargetHudComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
      this.healthAnimation = new Animation(250L, Easing.CUBIC_OUT);
      this.outdatedHealthAnimation = new Animation(650L, Easing.CUBIC_OUT);
      this.gappleAnimation = new Animation(250L, Easing.CUBIC_OUT);
      this.toggleAnimation = new Animation(250L, Easing.CUBIC_OUT);
      this.toggleAnimationMetanoise = new Animation(1850L, Easing.CUBIC_OUT);
   }

   @Native
   public void render(CustomDrawContext ctx) {
      Aura aura = Aura.INSTANCE;
      LivingEntity target = mc.currentScreen instanceof ChatScreen ? mc.player : aura.getTarget();
      this.setTarget(target);
      if (this.toggleAnimationMetanoise.getValue() != 0.0F && this.target != null) {
         this.renderTargetHud(ctx, this.target, this.toggleAnimation.getValue());
      }
   }

   @Native
   private void renderTargetHud(CustomDrawContext ctx, LivingEntity target, float animation) {
      float posX = this.x;
      float posY = this.y;
      float width = 110.0F;
      float height = 42.0F;
      Theme theme = yabloko.getInstance().getThemeManager().getCurrentTheme();
      float hp = ScoreboardHealth.INSTANCE.isEnabled() ? PlayerIntersectionUtil.getHealth(target) : target.getHealth();
      this.healthAnimation.update(hp / target.getMaxHealth());
      if (this.outdatedHealthAnimation.getValue() < this.healthAnimation.getValue()) {
         this.outdatedHealthAnimation.setValue(this.healthAnimation.getValue());
         this.outdatedHealthAnimation.setStartValue(this.healthAnimation.getValue());
      } else {
         this.outdatedHealthAnimation.update(hp / target.getMaxHealth());
      }

      this.gappleAnimation.update(target.getAbsorptionAmount() / target.getMaxHealth());
      StencilUtil.push();
      DrawUtil.drawMetanoise(ctx.getMatrices(), posX, posY, width, height, this.toggleAnimationMetanoise.getValue(), 4.0F, new ColorRGBA(0, 0, 0, 150), new ColorRGBA(140, 140, 140, 135));
      StencilUtil.read(1);
      DrawUtil.drawBlur(ctx.getMatrices(), posX, posY, width, height, 14.0F, BorderRadius.all(4.0F), new ColorRGBA(18, 21, 27, 235.0F * animation));
      DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, posY, width, height, BorderRadius.all(4.0F), new ColorRGBA(17, 21, 28, 225.0F * animation), new ColorRGBA(17, 21, 28, 225.0F * animation), new ColorRGBA(37, 42, 53, 225.0F * animation), new ColorRGBA(37, 42, 53, 225.0F * animation));
      DrawUtil.drawMetanoise(ctx.getMatrices(), posX, posY, width, height, this.toggleAnimationMetanoise.getValue(), 4.0F, new ColorRGBA(0, 0, 0, 80.0F * animation), new ColorRGBA(140, 140, 140, 105.0F * animation));

      Identifier skinTextures = null;
      Iterator<PlayerListEntry> iterator = mc.getNetworkHandler().getPlayerList().iterator();
      while(iterator.hasNext()) {
         PlayerListEntry playerListEntry = iterator.next();
         if (playerListEntry.getProfile().name().equals(target.getNameForScoreboard())) {
            skinTextures = playerListEntry.getSkinTextures().body().id();
         }
      }

      if (skinTextures == null) {
         skinTextures = DefaultSkinHelper.getSteve().body().id();
      }

      float headSize = 26.0F;
      float headX = posX + 5.0F;
      float headY = posY + 5.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), headX - 1.0F, headY - 1.0F, headSize + 2.0F, headSize + 2.0F, BorderRadius.all(2.0F), theme.getColor().withAlpha(animation * 145.0F));
      DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(), skinTextures, headX, headY, headSize, BorderRadius.all(0.0F), ColorRGBA.WHITE.withAlpha(animation * 255.0F));

      String targetName = target == mc.player ? NameProtect.getCustomName() : target.getNameForScoreboard();
      String hpText = String.format("%.0f HP", hp) + (target.getAbsorptionAmount() > 0.0F ? String.format(" +%.1f", target.getAbsorptionAmount()).replace(",", ".") : "");
      MsdfRenderer.renderText(Fonts.MEDIUM, targetName, 8.25F, ColorRGBA.WHITE.withAlpha(animation * 255.0F).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), posX + 36.0F, posY + 7.0F, 0.0F, true, 0.75F, 1.0F, 68.0F);
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), hpText, posX + 36.0F, posY + 20.5F, ColorRGBA.WHITE.withAlpha(animation * 230.0F));

      float barX = posX + 5.0F;
      float barY = posY + 34.0F;
      float barMaxWidth = width - 10.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, barMaxWidth, 4.5F, BorderRadius.all(2.0F), new ColorRGBA(10, 12, 16, animation * 185.0F));
      DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, MathHelper.clamp(barMaxWidth * this.outdatedHealthAnimation.getValue(), 0.0F, barMaxWidth), 4.5F, BorderRadius.all(2.0F), theme.getSecondColor().darker(0.35F).withAlpha(animation * 210.0F), theme.getSecondColor().darker(0.35F).withAlpha(animation * 210.0F), theme.getColor().darker(0.35F).withAlpha(animation * 210.0F), theme.getColor().darker(0.35F).withAlpha(animation * 210.0F));
      if (this.gappleAnimation.getValue() < this.healthAnimation.getValue()) {
         DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, MathHelper.clamp(barMaxWidth * this.healthAnimation.getValue(), 0.0F, barMaxWidth), 4.5F, BorderRadius.all(2.0F), theme.getSecondColor().withAlpha(animation * 255.0F), theme.getSecondColor().withAlpha(animation * 255.0F), theme.getColor().withAlpha(animation * 255.0F), theme.getColor().withAlpha(animation * 255.0F));
      }

      DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, MathHelper.clamp(barMaxWidth * this.gappleAnimation.getValue(), 0.0F, barMaxWidth), 4.5F, BorderRadius.all(2.0F), new ColorRGBA(255, 209, 0, animation * 255.0F), new ColorRGBA(255, 209, 0, animation * 255.0F), new ColorRGBA(255, 246, 20, animation * 255.0F), new ColorRGBA(255, 246, 20, animation * 255.0F));
      StencilUtil.pop();
      if (target instanceof PlayerEntity) {
         this.drawArmor(ctx, (PlayerEntity)target, posX + 6.0F, posY - 12.0F);
      }

      this.width = width;
      this.height = height;
   }

   private void drawArmor(CustomDrawContext ctx, PlayerEntity player, float posX, float posY) {
      float boxSizeItem = 10.0F;
      float paddingItem = 0.0F;
      float iconX = posX + (5.0F - this.toggleAnimation.getValue() * 5.0F);
      float iconY = posY + 1.0F + (5.0F - this.toggleAnimation.getValue() * 5.0F);
      ItemStack[] items = new ItemStack[]{player.getMainHandStack(), player.getOffHandStack(), player.getEquippedStack(EquipmentSlot.HEAD), player.getEquippedStack(EquipmentSlot.CHEST), player.getEquippedStack(EquipmentSlot.LEGS), player.getEquippedStack(EquipmentSlot.FEET)};

      for(ItemStack stack : items) {
         if (!stack.isEmpty()) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate((double)iconX + ((double)boxSizeItem - 9.6D) / 2.0D, (double)iconY + ((double)boxSizeItem - 9.6D) / 2.0D, 0.0D);
            ctx.getMatrices().scale(0.6F * this.toggleAnimation.getValue(), 0.6F * this.toggleAnimation.getValue(), 0.6F * this.toggleAnimation.getValue());
            ctx.drawItem(stack, 0, 0);
            ((DrawContextAccessor)ctx).callDrawItemBar(stack, 0, 0);
            ((DrawContextAccessor)ctx).callDrawCooldownProgress(stack, 0, 0);
            ctx.getMatrices().pop();
            iconX += boxSizeItem + paddingItem;
         }
      }
   }

   public void setTarget(LivingEntity target) {
      if (target == null) {
         this.toggleAnimation.update(0.0F);
         this.toggleAnimationMetanoise.update(0.0F);
         this.toggleAnimationMetanoise.setDuration(2200L);
         this.toggleAnimationMetanoise.setEasing(Easing.CIRC_OUT);
         if (this.toggleAnimationMetanoise.getValue() == 0.0F) {
            this.target = null;
         }
      } else {
         this.target = target;
         this.toggleAnimationMetanoise.update(1.0F);
         this.toggleAnimationMetanoise.setDuration(1300L);
         this.toggleAnimationMetanoise.setEasing(Easing.CIRC_OUT);
         this.toggleAnimation.update(1.0F);
      }
   }
}
