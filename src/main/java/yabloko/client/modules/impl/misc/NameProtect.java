package yabloko.client.modules.impl.misc;

public class NameProtect {
   public static final NameProtect INSTANCE = new NameProtect();

   public boolean isEnabled() {
      return false;
   }

   public static String getCustomName() {
      return "Player";
   }
}
