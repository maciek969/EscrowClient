package elexis.modules.impl.player;

import elexis.client.events.impl.client.EventKey;
import elexis.client.events.impl.entity.EventUpdate;
import elexis.client.utils.eventbus.EventHandler;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.BindSetting;
import elexis.modules.api.settings.impl.ModeSetting;
import elexis.modules.impl.combat.TargetPearl;
import elexis.modules.impl.combat.javelin.JavelinCombat;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.util.ArrayDeque;
import java.util.Deque;

@FunctionAdd(name = "ClickPearl", alias = "Click Pearl", category = Category.Player,
        description = "Throws an ender pearl on key press")
public final class ClickPearl extends Function {
    private static final String NORMAL = "Default";
    private static final String LEGIT = "Legit";
    private static final int MIDDLE_MOUSE_BIND = -3;

    private final ModeSetting mode = new ModeSetting("Mode", NORMAL, NORMAL, LEGIT);
    private final BindSetting pearlBind = new BindSetting("Pearl Bind", MIDDLE_MOUSE_BIND);
    private final Deque<ScheduledTask> tasks = new ArrayDeque<>();
    private ThrowFlow flow;
    private ClientPlayerEntity lastThrower;
    private int lastThrowAge;

    public ClickPearl() {
        addSettings(mode, pearlBind);
    }

    @EventHandler
    public void onKey(EventKey event) {
        if (!event.isKeyDown(pearlBind.get()) || !canStart()) return;
        if (flow != null && !isCurrentSession(flow)) finish();
        if (isPearlUseBusy() || TargetPearl.getMovementRotation() != null) return;

        int source = findPearl(0, 9);
        if (source == -1) source = findPearl(9, 36);
        if (source == -1 || mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(source))) return;
        if (source >= 9 && !canSwapFromInventory()) return;

        flow = new ThrowFlow(mc.player, mc.world, source, mc.player.getInventory().getSelectedSlot(), mode.is(LEGIT));
        int age = mc.player.age;
        if (flow.legit) {
            int preparation = source >= 9 ? 2 : 0;
            tasks.add(new ScheduledTask(age + preparation, this::prepare));
            tasks.add(new ScheduledTask(age + preparation + 2, this::throwPrepared));
            tasks.add(new ScheduledTask(age + preparation + 4, this::finish));
        } else {
            tasks.add(new ScheduledTask(age, this::throwImmediately));
        }
    }

    @EventHandler
    public void onTick(EventUpdate.Fast event) {
        if (flow == null) return;
        if (!isCurrentSession(flow) || !canStart() || mc.player.getInventory().getSelectedSlot() != flow.expectedSlot
                || (flow.source >= 9 && !canSwapFromInventory())) {
            finish();
            return;
        }

        ScheduledTask task = tasks.peekFirst();
        if (task != null && task.tick() <= mc.player.age) {
            tasks.removeFirst();
            task.action().run();
        }
    }

    public boolean isPearlUseBusy() {
        return flow != null && isCurrentSession(flow)
                || lastThrower != null && lastThrower == mc.player && lastThrowAge == lastThrower.age;
    }

    private boolean canStart() {
        return JavelinCombat.ready() && mc.currentScreen == null && !mc.player.isUsingItem();
    }

    private boolean canSwapFromInventory() {
        return mc.player.currentScreenHandler == mc.player.playerScreenHandler
                && mc.player.playerScreenHandler.getCursorStack().isEmpty();
    }

    private static boolean isCurrentSession(ThrowFlow use) {
        return mc.player == use.player && mc.world == use.world;
    }

    private static int findPearl(int start, int end) {
        int ordinary = JavelinCombat.find(stack -> stack.isOf(Items.ENDER_PEARL) && !stack.hasEnchantments(), start, end);
        return ordinary >= 0 ? ordinary : JavelinCombat.find(Items.ENDER_PEARL, start, end);
    }

    private void throwImmediately() {
        try {
            prepare();
            if (flow != null) throwPrepared();
        } finally {
            finish();
        }
    }

    private void prepare() {
        ThrowFlow use = flow;
        var inventory = use.player.getInventory();
        ItemStack pearl = inventory.getStack(use.source);
        if (!pearl.isOf(Items.ENDER_PEARL) || use.player.getItemCooldownManager().isCoolingDown(pearl)) {
            finish();
            return;
        }

        if (use.source < 9) {
            inventory.setSelectedSlot(use.source);
            use.expectedSlot = use.source;
            return;
        }

        Slot source = JavelinCombat.inventorySlot(use.source);
        if (source == null) {
            finish();
            return;
        }
        use.displaced = inventory.getStack(use.originalSlot).copy();
        use.pearlStack = pearl.copy();
        swap(use, source);
        if (!hasSwapContents(use)) finish();
    }

    private void throwPrepared() {
        ThrowFlow use = flow;
        ItemStack held = use.player.getMainHandStack();
        if (!held.isOf(Items.ENDER_PEARL) || use.player.getItemCooldownManager().isCoolingDown(held)) {
            finish();
            return;
        }
        lastThrower = use.player;
        lastThrowAge = use.player.age;
        var result = mc.interactionManager.interactItem(use.player, Hand.MAIN_HAND);
        if (use.legit && result.isAccepted()) use.player.swingHand(Hand.MAIN_HAND);
    }

    private void swap(ThrowFlow use, Slot source) {
        mc.interactionManager.clickSlot(use.player.currentScreenHandler.syncId, source.id,
                use.originalSlot, SlotActionType.SWAP, use.player);
    }

    private static boolean hasSwapContents(ThrowFlow use) {
        var inventory = use.player.getInventory();
        ItemStack hotbar = inventory.getStack(use.originalSlot);
        return use.displaced != null && ItemStack.areEqual(inventory.getStack(use.source), use.displaced)
                && (hotbar.isEmpty() || (ItemStack.areItemsAndComponentsEqual(hotbar, use.pearlStack)
                && hotbar.getCount() <= use.pearlStack.getCount()));
    }

    private void finish() {
        ThrowFlow use = flow;
        flow = null;
        tasks.clear();
        if (use == null || !isCurrentSession(use)) return;

        var inventory = use.player.getInventory();
        if (use.expectedSlot != use.originalSlot && inventory.getSelectedSlot() == use.expectedSlot) {
            inventory.setSelectedSlot(use.originalSlot);
        }
        if (use.source >= 9 && mc.interactionManager != null
                && use.player.currentScreenHandler.getCursorStack().isEmpty() && hasSwapContents(use)) {
            Slot source = JavelinCombat.inventorySlot(use.source);
            if (source != null) swap(use, source);
        }
    }

    @Override
    public void onDisable() {
        finish();
        lastThrower = null;
        super.onDisable();
    }

    private record ScheduledTask(int tick, Runnable action) {}

    private static final class ThrowFlow {
        final ClientPlayerEntity player;
        final ClientWorld world;
        final int source;
        final int originalSlot;
        final boolean legit;
        int expectedSlot;
        ItemStack displaced;
        ItemStack pearlStack;

        ThrowFlow(ClientPlayerEntity player, ClientWorld world, int source, int originalSlot, boolean legit) {
            this.player = player;
            this.world = world;
            this.source = source;
            this.originalSlot = originalSlot;
            this.expectedSlot = originalSlot;
            this.legit = legit;
        }
    }
}
