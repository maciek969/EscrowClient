package elexis.modules.impl.misc;

import elexis.Elexis;
import elexis.client.events.impl.client.EventKey;
import elexis.client.events.impl.client.EventReceivePacket;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.events.impl.render.EventRender;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BindSetting;
import elexis.modules.api.settings.impl.BooleanSetting;
import elexis.modules.api.settings.impl.MultiBooleanSetting;
import elexis.modules.impl.combat.javelin.JavelinCombat;
import elexis.modules.impl.render.hud.HudRenderUtil;
import elexis.modules.impl.render.visual.VisualGeometry;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@FunctionAdd(name = "RWHelper", alias = "RWHelper", category = Category.Utilities, description = "ReallyWorld items, orbs and GPS")
public final class RWHelper extends Function {
    private static final Pattern COORDINATES = Pattern.compile("(?i)x[:\\s]*(-?\\d+).*?z[:\\s]*(-?\\d+)");
    public final BindSetting antiFlyKey = new BindSetting("Anti Fly bind", 0);
    public final BindSetting scrollKey = new BindSetting("Scroll bind", 0);
    public final BindSetting trapKey = new BindSetting("Trap bind", 0);
    private final MultiBooleanSetting options = new MultiBooleanSetting("Actions",
            new BooleanSetting("Auto-GPS to events", true), new BooleanSetting("Ignore friends in /pvp", true));
    private final BooleanSetting autoOrb = new BooleanSetting("Auto-swap orb", false);
    private final MultiBooleanSetting conditions = new MultiBooleanSetting("Swap conditions",
            new BooleanSetting("Below health", false), new BooleanSetting("Enemy eating", false),
            new BooleanSetting("Enemy retreating", false), new BooleanSetting("Enemy lost totem", false));
    private ClientWorld world;
    private BlockPos gps;
    private int swapSlot = -1;
    private ItemStack originalOrb = ItemStack.EMPTY, equippedOrb = ItemStack.EMPTY;
    private LivingEntity orbTarget, poppedTarget;
    private int lastHurt;
    private long nextOrbCheck, poppedAt;
    private boolean scrollHeld;

    public RWHelper() {
        conditions.setVisible(autoOrb::get);
        addSettings(antiFlyKey, scrollKey, trapKey, options, autoOrb, conditions);
    }

    @EventHandler public void input(EventKey event) {
        if (nullCheck() || event.screen() != null) return;
        if (event.isKeyReleased(scrollKey.get())) scrollHeld = false;
        else if (event.isKeyDown(antiFlyKey.get())) useAntiFlight();
        else if (event.isKeyDown(scrollKey.get())) scrollHeld = true;
        else if (event.isKeyDown(trapKey.get())) HelperItems.use(HelperItems.find(Items.HEART_OF_THE_SEA, "Trap", true));
    }

    @EventHandler public void tick(EventUpdate.Fast event) {
        if (world != mc.world) {
            world = mc.world;
            gps = null;
            clearSwap();
            scrollHeld = false;
            poppedTarget = null;
            nextOrbCheck = 0;
        }
        if (nullCheck()) return;
        if (mc.currentScreen != null) scrollHeld = false;
        if (scrollHeld) {
            int slot = HelperItems.find(Items.FLOWER_BANNER_PATTERN, "XP scroll", false);
            if (slot < 0) scrollHeld = false;
            else HelperItems.use(slot);
        }
        var target = HudRenderUtil.currentHudTarget();
        if (swapSlot >= 0) {
            boolean hit = mc.player.hurtTime > lastHurt && target != null && (mc.player.getRecentDamageSource() == null
                    ? mc.player.squaredDistanceTo(target) <= 36
                    : mc.player.getRecentDamageSource().getAttacker() == target);
            if (!autoOrb.get() || target != orbTarget || hit) restoreOrb();
        } else if (autoOrb.get() && target != null && System.nanoTime() >= nextOrbCheck) {
            nextOrbCheck = System.nanoTime() + 1_000_000_000L;
            if (shouldSwap(target)) equipBetterOrb(target);
        }
        lastHurt = mc.player.hurtTime;
    }

    private boolean shouldSwap(LivingEntity target) {
        return conditions.get().stream().noneMatch(BooleanSetting::get)
                || conditions.get("Below health") && target.getHealth() + target.getAbsorptionAmount()
                        < mc.player.getHealth() + mc.player.getAbsorptionAmount()
                || conditions.get("Enemy eating") && target.isUsingItem() && target.getActiveItem().contains(DataComponentTypes.FOOD)
                || conditions.get("Enemy retreating") && target.getEntityPos().subtract(mc.player.getEntityPos()).multiply(1,0,1)
                        .dotProduct(target.getVelocity().subtract(mc.player.getVelocity())) > .01
                || conditions.get("Enemy lost totem") && target == poppedTarget && System.nanoTime() - poppedAt < 3_000_000_000L;
    }

    private void equipBetterOrb(LivingEntity target) {
        ItemStack offhand = mc.player.getOffHandStack();
        double best = damage(offhand);
        if (best <= 0) return;
        int index = -1;
        for (int slot = 0; slot < 36; slot++) {
            double damage = damage(mc.player.getInventory().getStack(slot));
            if (damage > best) { best = damage; index = slot; }
        }
        if (index < 0) return;
        ItemStack old = offhand.copy();
        ItemStack replacement = mc.player.getInventory().getStack(index).copy();
        if (JavelinCombat.swapInventory(index, 40)) {
            swapSlot = index;
            originalOrb = old;
            equippedOrb = replacement;
            orbTarget = target;
        }
    }

    private static double damage(ItemStack stack) {
        if (!stack.isOf(Items.PLAYER_HEAD) || !stack.getName().getString().toLowerCase(Locale.ROOT).contains("шар")) return 0;
        double[] total = {0};
        stack.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT)
                .applyModifiers(EquipmentSlot.OFFHAND, (attribute, modifier) -> {
                    if (attribute == EntityAttributes.ATTACK_DAMAGE && modifier.operation() == EntityAttributeModifier.Operation.ADD_VALUE)
                        total[0] += modifier.value();
                });
        return total[0];
    }

    private void useAntiFlight() {
        int slot = HelperItems.find(Items.FIREWORK_STAR, "Anti Fly", false);
        if (slot < 0) return;
        if (mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(slot))) {
            mc.player.sendMessage(Text.literal("Anti Fly on cooldown"), true);
            return;
        }
        if (!JavelinCombat.swapInventory(slot, 40)) return;
        try {
            PlayerInput input = mc.player.input.playerInput;
            if (input.sneak()) sendPacket(new PlayerInputC2SPacket(withSneak(input, false)));
            sendPacket(new PlayerInputC2SPacket(withSneak(input, true)));
            if (!input.sneak()) sendPacket(new PlayerInputC2SPacket(input));
        } finally { JavelinCombat.swapInventory(slot, 40); }
    }

    private static PlayerInput withSneak(PlayerInput input, boolean sneak) {
        return new PlayerInput(input.forward(), input.backward(), input.left(), input.right(), input.jump(), sneak, input.sprint());
    }

    public boolean protectFriend(int syncId, int slot) {
        if (!options.get("Ignore friends in /pvp") || !(mc.currentScreen instanceof GenericContainerScreen screen)
                || !screen.getTitle().getString().equals("PVP » Ожидание...")) return false;
        var handler = screen.getScreenHandler();
        return handler.syncId == syncId && slot >= 0 && slot < handler.slots.size()
                && handler.getSlot(slot).hasStack()
                && Elexis.getInstance().getFriendStorage().isFriend(handler.getSlot(slot).getStack().getName().getString());
    }

    @EventHandler public void packet(EventReceivePacket event) {
        if (!(event.getPacket() instanceof BossBarS2CPacket || event.getPacket() instanceof EntityStatusS2CPacket)) return;
        var connection = mc.getNetworkHandler();
        mc.execute(() -> {
            if (!isState() || connection != mc.getNetworkHandler() || nullCheck()) return;
            if (event.getPacket() instanceof EntityStatusS2CPacket status && status.getStatus() == 35
                    && status.getEntity(mc.world) instanceof LivingEntity living && living == HudRenderUtil.currentHudTarget()) {
                poppedTarget = living;
                poppedAt = System.nanoTime();
            } else if (options.get("Auto-GPS to events") && event.getPacket() instanceof BossBarS2CPacket boss) {
                boss.accept(new BossBarS2CPacket.Consumer() {
                    @Override public void add(UUID id, Text name, float progress, BossBar.Color color, BossBar.Style style,
                                              boolean darkenSky, boolean dragonMusic, boolean thickenFog) { updateName(id, name); }
                    @Override public void updateName(UUID id, Text name) {
                        var matcher = COORDINATES.matcher(name.getString());
                        if (!matcher.find()) return;
                        try {
                            var pos = new BlockPos(Integer.parseInt(matcher.group(1)), 0, Integer.parseInt(matcher.group(2)));
                            if (pos.equals(gps)) return;
                            gps = pos;
                            mc.player.sendMessage(Text.literal("RWHelper: GPS set to X: " + pos.getX() + " Z: " + pos.getZ()), false);
                        } catch (NumberFormatException ignored) {}
                    }
                });
            }
        });
    }

    @EventHandler public void render(EventRender.Screen.Hud event) {
        if (nullCheck() || gps == null || world != mc.world || !options.get("Auto-GPS to events") || mc.options.hudHidden) return;
        double dx = gps.getX() + .5 - mc.player.getX(), dz = gps.getZ() + .5 - mc.player.getZ();
        double distance = Math.hypot(dx, dz);
        double angle = Math.atan2(dz, dx) - Math.toRadians(mc.gameRenderer.getCamera().getYaw() + 90);
        float x = event.getViewportWidth() / 2f + (float)Math.sin(angle) * 65;
        float y = event.getViewportHeight() / 2f - (float)Math.cos(angle) * 65;
        String label = "◆ " + Math.round(distance) + " m";
        float size = 12;
        var renderer = event.getRenderer();
        var font = event.getDefaultFont();
        renderer.text(font, x - renderer.measureText(font, label, size).width / 2, y, size, label, 0xFFE4B4FF);
    }

    private void restoreOrb() {
        if (swapSlot < 0) return;
        if (world != mc.world || mc.player == null || !ItemStack.areEqual(mc.player.getOffHandStack(), equippedOrb)
                || !ItemStack.areEqual(mc.player.getInventory().getStack(swapSlot), originalOrb)) {
            clearSwap();
        } else if (JavelinCombat.swapInventory(swapSlot, 40)) clearSwap();
    }

    private void clearSwap() {
        swapSlot = -1;
        equippedOrb = originalOrb = ItemStack.EMPTY;
        orbTarget = null;
    }

    @Override public void onDisable() {
        restoreOrb();
        clearSwap();
        gps = null;
        scrollHeld = false;
        super.onDisable();
    }
}
