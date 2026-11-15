package yabloko.client.hud.elements.component;

import java.util.Iterator;
import net.minecraft.client.gui.screen.ChatScreen;
import ru.nexusguard.protection.annotations.Native;
import yabloko.yabloko;
import yabloko.base.animations.base.Animation;
import yabloko.base.animations.base.Easing;
import yabloko.base.font.Fonts;
import yabloko.base.theme.Theme;
import yabloko.client.hud.elements.draggable.DraggableHudElement;
import yabloko.client.modules.api.Module;
import yabloko.utility.render.display.Keyboard;
import yabloko.utility.render.display.base.BorderRadius;
import yabloko.utility.render.display.base.CustomDrawContext;
import yabloko.utility.render.display.base.color.ColorRGBA;
import yabloko.utility.render.display.shader.DrawUtil;

public class KeybindsComponent extends DraggableHudElement {
   private final Animation widthAnimation;
   private final Animation xLine;
   private final Animation alpha;

   public KeybindsComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
      this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
      this.xLine = new Animation(170L, Easing.SINE_OUT);
      this.alpha = new Animation(200L, Easing.CUBIC_OUT);
   }

   @Native
   public void render(CustomDrawContext ctx) {
      float posX = this.getX();
      float posY = this.getY();
      float defaultWidth = 72.0F;
      float height = 18.0F;
      boolean isFound = false;
      Iterator<Module> iterator = yabloko.getInstance().getModuleManager().getModules().iterator();

      while(iterator.hasNext()) {
         Module module = iterator.next();
         if (module.isEnabled() && module.getKeyCode() != -1) {
            this.alpha.update(1.0F);
            isFound = true;
         }
      }

      if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
         this.alpha.update(0.0F);
      }

      if (mc.currentScreen instanceof ChatScreen) {
         this.alpha.update(1.0F);
      }

      float bindWidth = 0.0F;
      iterator = yabloko.getInstance().getModuleManager().getModules().iterator();
      while(iterator.hasNext()) {
         Module module = iterator.next();
         if (module.getAnimation().getValue() != 0.0F && module.getKeyCode() != -1) {
            float localBindWidth = Fonts.REGULAR.getWidth(Keyboard.getKeyName(module.getKeyCode()), 6.75F);
            if (localBindWidth > bindWidth) {
               bindWidth = localBindWidth;
            }

            height += 12.0F * module.getAnimation().getValue();
         }
      }

      this.xLine.update(bindWidth);
      Theme theme = yabloko.getInstance().getThemeManager().getCurrentTheme();
      DrawUtil.drawRoundedRect(ctx.getMatrices(), posX, posY, this.widthAnimation.getValue(), height, BorderRadius.all(4.0F), new ColorRGBA(18, 21, 27, 255.0F * this.alpha.getValue()), new ColorRGBA(18, 21, 27, 255.0F * this.alpha.getValue()), new ColorRGBA(37, 42, 53, 255.0F * this.alpha.getValue()), new ColorRGBA(37, 42, 53, 255.0F * this.alpha.getValue()));
      ctx.drawText(Fonts.ICONS2.getFont(7.0F), "\uf11c", posX + 5.5F, posY + 6.0F, theme.getColor().withAlpha(255.0F * this.alpha.getValue()));
      ctx.drawText(Fonts.MEDIUM.getFont(7.25F), "KeyBinds", posX + 18.0F, posY + 5.75F, (new ColorRGBA(-1)).withAlpha(255.0F * this.alpha.getValue()));
      posY += 18.0F;

      iterator = yabloko.getInstance().getModuleManager().getModules().iterator();
      while(iterator.hasNext()) {
         Module module = iterator.next();
         if (module.getAnimation().getValue() != 0.0F && module.getKeyCode() != -1) {
            String bind = Keyboard.getKeyName(module.getKeyCode());
            String moduleName = module.getName();
            float rowY = posY + module.getAnimation().getValue() * 3.0F - 3.0F;
            float rowAlpha = module.getAnimation().getValue() * this.alpha.getValue();
            float elementsWidth = Fonts.REGULAR.getWidth(moduleName, 6.75F) + Fonts.REGULAR.getWidth(bind, 6.75F) + 47.0F;

            ctx.drawText(Fonts.ICONS.getFont(5.0F), module.getCategory().getIcon(), posX + 7.0F, rowY + 4.25F, theme.getColor().withAlpha(rowAlpha * 255.0F));
            ctx.drawText(Fonts.REGULAR.getFont(6.75F), moduleName, posX + 15.0F, rowY + 3.75F, (new ColorRGBA(-1)).withAlpha(rowAlpha * 255.0F));
            ctx.drawText(Fonts.REGULAR.getFont(6.5F), bind, posX + this.widthAnimation.getValue() - 7.0F - Fonts.REGULAR.getWidth(bind, 6.75F), rowY + 3.75F, (new ColorRGBA(-1)).withAlpha(rowAlpha * 255.0F));

            if (elementsWidth > defaultWidth) {
               defaultWidth = elementsWidth;
            }

            posY += 12.0F * module.getAnimation().getValue();
         }
      }

      this.widthAnimation.update(defaultWidth);
      this.width = this.widthAnimation.getValue();
      this.height = height;
   }
}
