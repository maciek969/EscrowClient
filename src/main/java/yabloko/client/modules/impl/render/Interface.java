package yabloko.client.modules.impl.render;

import org.joml.Vector2f;
import yabloko.base.animations.base.Animation;
import yabloko.base.animations.base.Easing;
import yabloko.client.modules.api.Category;
import yabloko.client.modules.api.Module;

public class Interface implements Module {
   public static final Interface INSTANCE = new Interface();

   private Interface() {
   }

   public Vector2f getNearest(float x, float y) {
      return new Vector2f(x, y);
   }

   public boolean isEnableHotBar() {
      return false;
   }

   public boolean isEnableTab() {
      return false;
   }

   public boolean isEnableScoreBar() {
      return false;
   }

   @Override
   public boolean isEnabled() {
      return false;
   }

   @Override
   public int getKeyCode() {
      return -1;
   }

   @Override
   public String getName() {
      return "Interface";
   }

   @Override
   public Category getCategory() {
      return new Category();
   }

   @Override
   public Animation getAnimation() {
      return new Animation(0L, Easing.QUAD_IN_OUT);
   }

   @Override
   public int getBind() {
      return -1;
   }
}
