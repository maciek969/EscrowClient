package elexis.modules.impl.player;

import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.client.utils.player.PlayerInventoryUtil;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;

import java.util.Comparator;

@FunctionAdd(name = "AutoArmor", alias = "AutoArmor", category = Category.Player, description = "Equips best armor")
@Environment(EnvType.CLIENT)
public class AutoArmor extends Function {
    private long lastSwap;

    @EventHandler
    public void onUpdate(EventUpdate.Slow event) {
        if (nullCheck() || System.currentTimeMillis() - lastSwap < 180L) {
            return;
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack current = mc.player.getEquippedStack(slot);
            if (slot == EquipmentSlot.CHEST && current.isOf(Items.ELYTRA)) {
                continue;
            }
            Slot best = PlayerInventoryUtil.getSlot(s -> isArmorFor(s.getStack(), slot), Comparator.comparingDouble(s -> armorScore(s.getStack())));
            if (best != null && armorScore(best.getStack()) > armorScore(current) + 0.01D) {
                PlayerInventoryUtil.moveItem(best, armorSlotId(slot), false, true);
                lastSwap = System.currentTimeMillis();
                return;
            }
        }
    }

    private boolean isArmorFor(ItemStack stack, EquipmentSlot slot) {
        return stack != null && armorSlot(stack) == slot && !isAlmostBroken(stack);
    }

    private boolean isAlmostBroken(ItemStack stack) {
        return stack != null && stack.isDamageable() && stack.getMaxDamage() > 0 && (double) stack.getDamage() / stack.getMaxDamage() > 0.98D;
    }

    private double armorScore(ItemStack stack) {
        if (stack == null || isAlmostBroken(stack)) {
            return -1.0D;
        }
        double material = materialScore(stack);
        EquipmentSlot slot = armorSlot(stack);
        if (slot == null || material < 0.0D) {
            return -1.0D;
        }
        double slotBonus = switch (slot) {
            case CHEST -> 3.0D;
            case LEGS -> 2.0D;
            case HEAD, FEET -> 1.0D;
            default -> 0.0D;
        };
        return material * 10.0D + slotBonus;
    }

    private EquipmentSlot armorSlot(ItemStack stack) {
        if (stack.isOf(Items.LEATHER_HELMET) || stack.isOf(Items.CHAINMAIL_HELMET) || stack.isOf(Items.IRON_HELMET) || stack.isOf(Items.GOLDEN_HELMET) || stack.isOf(Items.DIAMOND_HELMET) || stack.isOf(Items.NETHERITE_HELMET)) return EquipmentSlot.HEAD;
        if (stack.isOf(Items.LEATHER_CHESTPLATE) || stack.isOf(Items.CHAINMAIL_CHESTPLATE) || stack.isOf(Items.IRON_CHESTPLATE) || stack.isOf(Items.GOLDEN_CHESTPLATE) || stack.isOf(Items.DIAMOND_CHESTPLATE) || stack.isOf(Items.NETHERITE_CHESTPLATE)) return EquipmentSlot.CHEST;
        if (stack.isOf(Items.LEATHER_LEGGINGS) || stack.isOf(Items.CHAINMAIL_LEGGINGS) || stack.isOf(Items.IRON_LEGGINGS) || stack.isOf(Items.GOLDEN_LEGGINGS) || stack.isOf(Items.DIAMOND_LEGGINGS) || stack.isOf(Items.NETHERITE_LEGGINGS)) return EquipmentSlot.LEGS;
        if (stack.isOf(Items.LEATHER_BOOTS) || stack.isOf(Items.CHAINMAIL_BOOTS) || stack.isOf(Items.IRON_BOOTS) || stack.isOf(Items.GOLDEN_BOOTS) || stack.isOf(Items.DIAMOND_BOOTS) || stack.isOf(Items.NETHERITE_BOOTS)) return EquipmentSlot.FEET;
        return null;
    }

    private double materialScore(ItemStack stack) {
        if (stack.isOf(Items.NETHERITE_HELMET) || stack.isOf(Items.NETHERITE_CHESTPLATE) || stack.isOf(Items.NETHERITE_LEGGINGS) || stack.isOf(Items.NETHERITE_BOOTS)) return 6.0D;
        if (stack.isOf(Items.DIAMOND_HELMET) || stack.isOf(Items.DIAMOND_CHESTPLATE) || stack.isOf(Items.DIAMOND_LEGGINGS) || stack.isOf(Items.DIAMOND_BOOTS)) return 5.0D;
        if (stack.isOf(Items.IRON_HELMET) || stack.isOf(Items.IRON_CHESTPLATE) || stack.isOf(Items.IRON_LEGGINGS) || stack.isOf(Items.IRON_BOOTS)) return 4.0D;
        if (stack.isOf(Items.CHAINMAIL_HELMET) || stack.isOf(Items.CHAINMAIL_CHESTPLATE) || stack.isOf(Items.CHAINMAIL_LEGGINGS) || stack.isOf(Items.CHAINMAIL_BOOTS)) return 3.0D;
        if (stack.isOf(Items.GOLDEN_HELMET) || stack.isOf(Items.GOLDEN_CHESTPLATE) || stack.isOf(Items.GOLDEN_LEGGINGS) || stack.isOf(Items.GOLDEN_BOOTS)) return 2.0D;
        if (stack.isOf(Items.LEATHER_HELMET) || stack.isOf(Items.LEATHER_CHESTPLATE) || stack.isOf(Items.LEATHER_LEGGINGS) || stack.isOf(Items.LEATHER_BOOTS)) return 1.0D;
        return -1.0D;
    }

    private int armorSlotId(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            default -> 6;
        };
    }
}