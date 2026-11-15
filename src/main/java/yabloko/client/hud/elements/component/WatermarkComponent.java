package yabloko.client.hud.elements.component;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;
import ru.nexusguard.protection.annotations.Native;
import yabloko.yabloko;
import yabloko.base.font.Fonts;
import yabloko.base.theme.Theme;
import yabloko.client.hud.elements.draggable.DraggableHudElement;
import yabloko.client.modules.impl.misc.NameProtect;
import yabloko.utility.render.display.base.BorderRadius;
import yabloko.utility.render.display.base.CustomDrawContext;
import yabloko.utility.render.display.base.color.ColorRGBA;
import yabloko.utility.render.display.shader.DrawUtil;

public class WatermarkComponent extends DraggableHudElement {
   public WatermarkComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
   }

   @Native
   public void render(CustomDrawContext ctx) {
      float x = this.getX();
      float y = this.getY();
      Theme theme = yabloko.getInstance().getThemeManager().getCurrentTheme();
      String clientName = this.getAnimatedClientName();
      String fps = mc.getCurrentFps() + "fps";
      PlayerListEntry list = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
      String playerName = NameProtect.INSTANCE.isEnabled() ? NameProtect.getCustomName() : mc.player.getNameForScoreboard();
      String ping = list != null ? list.getLatency() + "ms" : "0ms";

      float avatarSize = 12.0F;
      float iconBlockW = avatarSize + 7.0F;
      float sepW = 11.0F;
      float iconW = 8.0F;
      float height = 18.0F;
      float width = iconBlockW
            + Fonts.REGULAR.getWidth(clientName, 7.25F) + 4.0F
            + sepW + iconW + Fonts.REGULAR.getWidth(playerName, 7.25F) + 4.0F
            + sepW + iconW + Fonts.REGULAR.getWidth(ping, 7.25F) + 4.0F
            + sepW + iconW + Fonts.REGULAR.getWidth(fps, 7.25F) + 6.0F;

      DrawUtil.drawRoundedRect(ctx.getMatrices(), x - 0.5F, y - 1.5F, width, height, BorderRadius.all(4.0F), new ColorRGBA(18, 22, 29, 246), new ColorRGBA(18, 22, 29, 246), new ColorRGBA(30, 36, 47, 246), new ColorRGBA(30, 36, 47, 246));
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 1.0F, y + height - 3.0F, width - 3.0F, 1.0F, BorderRadius.all(0.5F), new ColorRGBA(96, 163, 255, 160), new ColorRGBA(70, 146, 255, 160), new ColorRGBA(70, 146, 255, 160), new ColorRGBA(96, 163, 255, 160));

      float avatarX = x + 2.5F;
      float avatarY = y + 1.5F;
      Identifier avatarTex = yabloko.getInstance().getDiscordManager() != null ? yabloko.getInstance().getDiscordManager().getAvatarId() : null;
      if (avatarTex != null) {
         DrawUtil.drawRoundedTextureWithUV(ctx.getMatrices(), avatarTex, avatarX, avatarY, avatarSize, avatarSize, BorderRadius.all(3.0F), ColorRGBA.WHITE, 0.0F, 0.0F, 1.0F, 1.0F);
      } else {
         ctx.drawTexture(Identifier.of("yabloko", "icons/yabloko_logo.png"), avatarX, avatarY, avatarSize, avatarSize, ColorRGBA.WHITE);
      }

      float cx = x + iconBlockW;
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), clientName, cx, y + 4.75F, new ColorRGBA(255, 255, 255, 255));
      cx += Fonts.REGULAR.getWidth(clientName, 7.25F) + 4.0F;

      DrawUtil.drawRoundedRect(ctx.getMatrices(), cx + 3.75F, y + 6.75F, 2.0F, 2.0F, BorderRadius.all(0.5F), theme.getColor());
      cx += sepW;
      ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf007", cx, y + 5.25F, theme.getColor());
      cx += iconW;
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), playerName, cx, y + 4.75F, new ColorRGBA(255, 255, 255, 255));
      cx += Fonts.REGULAR.getWidth(playerName, 7.25F) + 4.0F;

      DrawUtil.drawRoundedRect(ctx.getMatrices(), cx + 3.75F, y + 6.75F, 2.0F, 2.0F, BorderRadius.all(0.5F), theme.getColor());
      cx += sepW;
      ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf1eb", cx, y + 5.15F, theme.getColor());
      cx += iconW;
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), ping, cx, y + 4.75F, new ColorRGBA(255, 255, 255, 255));
      cx += Fonts.REGULAR.getWidth(ping, 7.25F) + 4.0F;

      DrawUtil.drawRoundedRect(ctx.getMatrices(), cx + 3.75F, y + 6.75F, 2.0F, 2.0F, BorderRadius.all(0.5F), theme.getColor());
      cx += sepW;
      ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf624", cx, y + 5.45F, theme.getColor());
      cx += iconW;
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), fps, cx, y + 4.75F, new ColorRGBA(255, 255, 255, 255));

      this.width = width;
      this.height = height;
   }

   private String getAnimatedClientName() {
      long time = System.currentTimeMillis() % 4600L;
      String cursor = System.currentTimeMillis() % 700L < 350L ? "|" : "";
      if (time < 900L) {
         return "Yabloko".substring(0, Math.max(1, (int)(time / 130L) + 1)) + cursor;
      }

      if (time < 1700L) {
         return "Yabloko" + cursor;
      }

      if (time < 2500L) {
         int letters = Math.max(0, 7 - (int)((time - 1700L) / 115L));
         return (letters == 0 ? "" : "Yabloko".substring(0, letters)) + cursor;
      }

      if (time < 3100L) {
         return "Dlc".substring(0, Math.max(1, (int)((time - 2500L) / 200L) + 1)) + cursor;
      }

      if (time < 4000L) {
         return "Dlc" + cursor;
      }

      int letters = Math.max(0, 3 - (int)((time - 4000L) / 200L));
      return (letters == 0 ? "" : "Dlc".substring(0, letters)) + cursor;
   }
}
