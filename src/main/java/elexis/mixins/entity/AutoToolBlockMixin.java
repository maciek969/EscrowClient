package elexis.mixins.entity;

import elexis.Elexis;
import elexis.client.utils.client.IMinecraft;
import elexis.modules.impl.player.AutoTool;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractBlock.class)
public abstract class AutoToolBlockMixin {
    @Redirect(method = "calcBlockBreakingDelta", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;getBlockBreakingSpeed(Lnet/minecraft/block/BlockState;)F"))
    private float elexis$javelinToolSpeed(PlayerEntity player, BlockState state) {
        float speed = player.getBlockBreakingSpeed(state);
        var manager = Elexis.getFunctionManager();
        AutoTool tool = manager == null ? null : (AutoTool) manager.getFunctionByName("AutoTool");
        return player == IMinecraft.mc.player && tool != null ? tool.breakingSpeed(state, speed) : speed;
    }
}
