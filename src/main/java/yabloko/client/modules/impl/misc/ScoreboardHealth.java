package yabloko.client.modules.impl.misc;

public class ScoreboardHealth {
   public static final ScoreboardHealth INSTANCE = new ScoreboardHealth();

   private ScoreboardHealth() {
   }

   public boolean isEnabled() {
      return false;
   }
}
