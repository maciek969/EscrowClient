package elexis.mixins.render;

import elexis.modules.impl.render.NoRneder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

@Mixin(BlockRenderManager.class)
public class NoRenderVegetationMixin {

    private static final Set<Block> VEGETATION_BLOCKS = Set.of(
            Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN,
            Blocks.DEAD_BUSH, Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID,
            Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.RED_TULIP, Blocks.ORANGE_TULIP,
            Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER,
            Blocks.LILY_OF_THE_VALLEY, Blocks.SUNFLOWER, Blocks.LILAC, Blocks.ROSE_BUSH,
            Blocks.PEONY, Blocks.WITHER_ROSE, Blocks.TORCHFLOWER, Blocks.PITCHER_PLANT,
            Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM,
            Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS,
            Blocks.SUGAR_CANE, Blocks.BAMBOO, Blocks.BAMBOO_SAPLING,
            Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING,
            Blocks.JUNGLE_SAPLING, Blocks.ACACIA_SAPLING, Blocks.DARK_OAK_SAPLING,
            Blocks.CHERRY_SAPLING, Blocks.MANGROVE_PROPAGULE,
            Blocks.VINE, Blocks.CAVE_VINES, Blocks.CAVE_VINES_PLANT,
            Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT,
            Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT,
            Blocks.LILY_PAD, Blocks.SEA_PICKLE, Blocks.SEAGRASS, Blocks.TALL_SEAGRASS,
            Blocks.KELP, Blocks.KELP_PLANT
    );

    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true, require = 0)
    private void noRender$vegetation(BlockState state, BlockPos pos, BlockRenderView world,
                                      MatrixStack stack, VertexConsumer vertexConsumer,
                                      boolean cull, List<?> list, CallbackInfo ci) {
        if (NoRneder.is("Растительность") && VEGETATION_BLOCKS.contains(state.getBlock())) {
            ci.cancel();
        }
    }
}
