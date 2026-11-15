package elexis.modules.impl.player;

import elexis.client.events.impl.entity.EventAttack;
import elexis.client.events.impl.client.EventSendPacket;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;

@FunctionAdd(name = "AutoTool", alias = "Auto Tool", category = Category.Player,
        description = "Picks a tool for the block and restores the previous slot, like Javelin")
public final class AutoTool extends Function {
    private int lastActionAge = Integer.MIN_VALUE;
    private int miningSlot = -1;
    private Object miningWorld;

    @EventHandler
    public void onAttack(EventAttack event) {
        if (mc.player != null) lastActionAge = mc.player.age;
    }

    @EventHandler
    public void onSend(EventSendPacket event) {
        if (mc.player != null && event.getPacket() instanceof PlayerInteractItemC2SPacket) {
            lastActionAge = mc.player.age;
        }
    }

    public int begin(BlockPos position) {
        if (miningWorld != mc.world) {
            miningWorld = mc.world;
            lastActionAge = Integer.MIN_VALUE;
            miningSlot = -1;
        }
        if (!isState() || nullCheck() || mc.player.isUsingItem() || mc.player.isSpectator()
                || lastActionAge != Integer.MIN_VALUE && mc.player.age - lastActionAge <= 1) return -1;
        BlockState state = mc.world.getBlockState(position);
        int bestSlot = -1;
        float bestSpeed = 1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;
            float speed = destroySpeed(stack, state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        if (bestSlot < 0) return -1;
        miningSlot = bestSlot;
        int oldSlot = mc.player.getInventory().getSelectedSlot();
        if (oldSlot != bestSlot) select(bestSlot);
        return oldSlot;
    }

    public void end(int oldSlot) {
        if (mc.player != null && oldSlot >= 0 && mc.player.getInventory().getSelectedSlot() == miningSlot
                && oldSlot != miningSlot) select(oldSlot);
        miningSlot = -1;
    }

    public boolean isMining() {
        return miningSlot >= 0;
    }

    private void select(int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
        sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    public float breakingSpeed(BlockState state, float vanillaSpeed) {
        if (!isState() || nullCheck() || miningSlot < 0) return vanillaSpeed;
        float speed = destroySpeed(mc.player.getInventory().getStack(miningSlot), state);
        return mc.player.isOnGround() ? speed : speed / 2;
    }

    private float destroySpeed(ItemStack stack, BlockState state) {
        float speed = stack.getMiningSpeedMultiplier(state);
        if (speed > 1) {
            var efficiency = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY);
            int level = EnchantmentHelper.getLevel(efficiency, stack);
            if (level > 0) speed += level * level + 1;
        }
        if (StatusEffectUtil.hasHaste(mc.player)) speed *= 1 + (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2f;
        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            speed *= switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 0.00081f;
            };
        }
        speed *= (float) mc.player.getAttributeValue(EntityAttributes.BLOCK_BREAK_SPEED);
        if (mc.player.isSubmergedIn(FluidTags.WATER)) speed *= (float) mc.player.getAttributeValue(EntityAttributes.SUBMERGED_MINING_SPEED);
        return mc.player.isOnGround() ? speed : speed / 3;
    }

    @Override
    public void onDisable() {
        miningSlot = -1;
        lastActionAge = Integer.MIN_VALUE;
        miningWorld = null;
        super.onDisable();
    }
}
