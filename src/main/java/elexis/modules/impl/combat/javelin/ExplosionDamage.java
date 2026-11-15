package elexis.modules.impl.combat.javelin;

import elexis.client.utils.client.IMinecraft;
import net.minecraft.block.Blocks;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.Difficulty;

import java.util.List;

/** Javelin's eight-corner explosion sampling, including predicted, removed and future blocks. */
public final class ExplosionDamage implements IMinecraft {
    private ExplosionDamage() {}

    public static float crystal(Vec3d source, PlayerEntity target) {
        return damage(source, target, target.getEntityPos(), 12, List.of(), List.of());
    }

    public static float anchor(BlockPos source, PlayerEntity target) {
        return damage(Vec3d.ofCenter(source), target, target.getEntityPos(), 10, List.of(source), List.of());
    }

    public static float damage(Vec3d source, PlayerEntity target, Vec3d position, double diameter,
                               List<BlockPos> air, List<BlockPos> futureBlocks) {
        if (mc.world == null || target == null || target.isInvulnerable() || mc.world.getDifficulty() == Difficulty.PEACEFUL) return 0;
        double distanceRatio = position.squaredDistanceTo(source) / (diameter * diameter);
        if (distanceRatio > 1) return 0;
        Box box = target.getBoundingBox().offset(position.subtract(target.getEntityPos()));
        int visible = 0;
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    Vec3d sample = new Vec3d(MathHelper.lerp(x, box.minX, box.maxX),
                            MathHelper.lerp(y, box.minY, box.maxY), MathHelper.lerp(z, box.minZ, box.maxZ));
                    Boolean blocked = BlockView.raycast(sample, source, air, (ignored, block) -> {
                        var state = ignored.contains(block) ? Blocks.AIR.getDefaultState()
                                : futureBlocks.contains(block) ? Blocks.OBSIDIAN.getDefaultState() : mc.world.getBlockState(block);
                        return state.getCollisionShape(mc.world, block).raycast(sample, source, block) != null ? Boolean.TRUE : null;
                    }, ignored -> Boolean.FALSE);
                    if (!blocked) visible++;
                }
            }
        }
        double impact = (1 - distanceRatio) * visible / 8d;
        float damage = (float)Math.floor((impact * impact + impact) / 2 * 7 * diameter + 1);
        var sourceType = mc.world.getDamageSources().explosion(null, null);
        damage = DamageUtil.getDamageLeft(target, damage, sourceType, target.getArmor(),
                (float)target.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS));
        var resistance = target.getStatusEffect(StatusEffects.RESISTANCE);
        if (resistance != null) damage = Math.max(0, damage * (25 - (resistance.getAmplifier() + 1) * 5) / 25f);
        int protection = 0;
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack stack = target.getEquippedStack(slot);
            for (var enchantment : stack.getEnchantments().getEnchantmentEntries()) {
                if (enchantment.getKey().matchesKey(Enchantments.PROTECTION)) protection += enchantment.getIntValue();
                if (enchantment.getKey().matchesKey(Enchantments.BLAST_PROTECTION)) protection += enchantment.getIntValue() * 2;
            }
        }
        return DamageUtil.getInflictedDamage(damage, Math.min(20, protection));
    }
}
