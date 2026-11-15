package elexis.mixins.render;

import com.google.common.base.MoreObjects;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import elexis.Elexis;
import elexis.client.events.impl.render.RenderHandsEvent;
import elexis.client.events.impl.render.RenderItemEvent;
import elexis.modules.impl.combat.auraProcess.rotationProcess.impl.FreeLookUtil;
import elexis.modules.impl.render.javelin.crystalhands.HandsEffectRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Shadow
    private ItemStack mainHand;
    @Shadow
    private ItemStack offHand;
    @Shadow
    private float equipProgressMainHand;
    @Shadow
    private float lastEquipProgressMainHand;
    @Shadow
    private float equipProgressOffHand;
    @Shadow
    private float lastEquipProgressOffHand;

    @Shadow
    private void renderFirstPersonItem(
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light
    ) {
        throw new AssertionError();
    }

    @Inject(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void Elexis$renderItemLike1218(
            float tickProgress,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            ClientPlayerEntity player,
            int light,
            CallbackInfo ci
    ) {
        ci.cancel();
        MinecraftClient mc = MinecraftClient.getInstance();
        Elexis$flushFirstPersonPass(mc);
        Elexis.getEventBus().post(new RenderHandsEvent.Pre(matrices, null));
        float swingProgress = player.getHandSwingProgress(tickProgress);
        Hand preferredHand = MoreObjects.firstNonNull(player.preferredHand, Hand.MAIN_HAND);
        float pitch = player.getLerpedPitch(tickProgress);
        float renderPitch = MathHelper.lerp(tickProgress, player.lastRenderPitch, player.renderPitch);
        float renderYaw = MathHelper.lerp(tickProgress, player.lastRenderYaw, player.renderYaw);

        if (FreeLookUtil.active) {
            float cameraPitch = mc.gameRenderer.getCamera().getPitch();
            float cameraYaw = mc.gameRenderer.getCamera().getYaw();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((cameraPitch - renderPitch) * 0.1F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((cameraYaw - renderYaw) * 0.1F));
        } else {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((player.getPitch(tickProgress) - renderPitch) * 0.1F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((player.getYaw(tickProgress) - renderYaw) * 0.1F));
        }

        var manager = Elexis.getFunctionManager();
        var viewModel = manager == null ? null : manager.getViewModel();
        boolean renderMainHand = true;
        boolean renderOffHand = true;
        ItemStack mainStack = player.getMainHandStack();
        ItemStack offStack = player.getOffHandStack();
        boolean hasBow = mainStack.isOf(Items.BOW) || offStack.isOf(Items.BOW);
        boolean hasCrossbow = mainStack.isOf(Items.CROSSBOW) || offStack.isOf(Items.CROSSBOW);

        if (hasBow || hasCrossbow) {
            if (player.isUsingItem()) {
                ItemStack activeStack = player.getActiveItem();
                Hand activeHand = player.getActiveHand();
                if (activeStack.isOf(Items.BOW) || activeStack.isOf(Items.CROSSBOW)) {
                    renderMainHand = activeHand == Hand.MAIN_HAND;
                    renderOffHand = activeHand == Hand.OFF_HAND;
                }
            } else if (Elexis$isChargedCrossbow(mainStack)) {
                renderOffHand = false;
            }
        }

        if (renderMainHand) {
            float handSwing = preferredHand == Hand.MAIN_HAND ? swingProgress : 0.0F;
            float equip = 1.0F - MathHelper.lerp(tickProgress, this.lastEquipProgressMainHand, this.equipProgressMainHand);
            matrices.push();
            Elexis.getEventBus().post(new RenderHandsEvent.Pre(matrices, Hand.MAIN_HAND));
            Elexis.getEventBus().post(new RenderItemEvent.Pre(matrices, Hand.MAIN_HAND));
            if (viewModel != null && viewModel.isState()) {
                viewModel.apply(matrices, player.getMainArm());
            }
            this.renderFirstPersonItem(player, tickProgress, pitch, Hand.MAIN_HAND, handSwing, this.mainHand, equip, matrices, queue, light);
            Elexis.getEventBus().post(new RenderItemEvent.Post(matrices, Hand.MAIN_HAND));
            Elexis.getEventBus().post(new RenderHandsEvent.Post(matrices, Hand.MAIN_HAND));
            matrices.pop();
        }

        if (renderOffHand) {
            float handSwing = preferredHand == Hand.OFF_HAND ? swingProgress : 0.0F;
            float equip = 1.0F - MathHelper.lerp(tickProgress, this.lastEquipProgressOffHand, this.equipProgressOffHand);
            matrices.push();
            Elexis.getEventBus().post(new RenderHandsEvent.Pre(matrices, Hand.OFF_HAND));
            Elexis.getEventBus().post(new RenderItemEvent.Pre(matrices, Hand.OFF_HAND));
            if (viewModel != null && viewModel.isState()) {
                viewModel.apply(matrices, player.getMainArm().getOpposite());
            }
            this.renderFirstPersonItem(player, tickProgress, pitch, Hand.OFF_HAND, handSwing, this.offHand, equip, matrices, queue, light);
            Elexis.getEventBus().post(new RenderItemEvent.Post(matrices, Hand.OFF_HAND));
            Elexis.getEventBus().post(new RenderHandsEvent.Post(matrices, Hand.OFF_HAND));
            matrices.pop();
        }

        var hands = manager == null ? null : manager.getHands();
        if (hands != null && hands.isState()) {
            HandsEffectRenderer.render(hands, this.mainHand.getItem(), this.offHand.getItem(),
                    () -> Elexis$flushFirstPersonPass(mc));
        } else {
            Elexis$flushFirstPersonPass(mc);
        }
        Elexis.getEventBus().post(new RenderHandsEvent.Post(matrices, null));
    }

    private boolean Elexis$isChargedCrossbow(ItemStack stack) {
        return stack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(stack);
    }

    private static void Elexis$flushFirstPersonPass(MinecraftClient mc) {
        mc.gameRenderer.getEntityRenderDispatcher().render();
        mc.getBufferBuilders().getEntityVertexConsumers().draw();
    }

    @WrapOperation(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V",
                    ordinal = 2
            )
    )
    private void elexis$renderCustomSwing(
            HeldItemRenderer instance,
            float progress,
            MatrixStack matrices,
            int armX,
            Arm arm,
            Operation<Void> original,
            @Local(argsOnly = true) Hand hand,
            @Local(argsOnly = true) ItemStack stack,
            @Local(argsOnly = true, ordinal = 3) float equipProgress
    ) {
        var manager = Elexis.getFunctionManager();
        var swing = manager == null ? null : manager.getCustomSwing();
        if (swing == null || !swing.apply(matrices, stack, hand, arm, progress, equipProgress)) {
            original.call(instance, progress, matrices, armX, arm);
        }
    }
}
