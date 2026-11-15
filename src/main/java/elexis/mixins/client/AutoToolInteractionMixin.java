package elexis.mixins.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import elexis.Elexis;
import elexis.modules.impl.player.AutoTool;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class AutoToolInteractionMixin {
    @WrapMethod(method = {"attackBlock", "updateBlockBreakingProgress"})
    private boolean elexis$selectMiningTool(BlockPos position, Direction direction, Operation<Boolean> original) {
        var manager = Elexis.getFunctionManager();
        AutoTool tool = manager == null ? null : (AutoTool) manager.getFunctionByName("AutoTool");
        int oldSlot = tool == null || tool.isMining() ? -1 : tool.begin(position);
        try {
            return original.call(position, direction);
        } finally {
            if (tool != null && oldSlot >= 0) tool.end(oldSlot);
        }
    }
}
