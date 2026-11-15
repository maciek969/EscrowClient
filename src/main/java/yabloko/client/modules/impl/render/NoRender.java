package yabloko.client.modules.impl.render;

public class NoRender {
   public static final NoRender INSTANCE = new NoRender();

   private NoRender() {
   }

   public boolean isRemoveFire() {
      return false;
   }
}
