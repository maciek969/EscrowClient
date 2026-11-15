package elexis.modules.impl.misc;

import elexis.Elexis;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;

@FunctionAdd(name = "ScoreboardHealth", alias = "ScoreboardHealth", category = Category.Utilities,
        description = "Health of nearby players by score under the name")
public final class ScoreboardHealth extends Function {
    public static float read(LivingEntity entity, float vanilla) {
        if (!(entity instanceof AbstractClientPlayerEntity player) || player == mc.player || vanilla <= 0f
                || mc.world == null || mc.player == null || player.getEntityWorld() != mc.world
                || player.squaredDistanceTo(mc.player) >= 100 || !mc.isOnThread()) return vanilla;
        var manager = Elexis.getFunctionManager();
        if (manager == null || !manager.getScoreboardHealth().isState()) return vanilla;
        var board = mc.world.getScoreboard();
        var objective = board.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
        if (objective == null) return vanilla;
        var score = board.getScore(player, objective);
        return score != null && score.getScore() > 0 ? score.getScore() : vanilla;
    }
}
