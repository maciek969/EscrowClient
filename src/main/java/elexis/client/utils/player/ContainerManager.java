package elexis.client.utils.player;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.*;

public class ContainerManager {
    public static boolean isContainerScreenWithoutInventory(Screen screen) {
        if (screen == null) {
            return false;
        }

        return screen instanceof CraftingScreen ||
                screen instanceof GenericContainerScreen ||
                screen instanceof FurnaceScreen ||
                screen instanceof BlastFurnaceScreen ||
                screen instanceof SmokerScreen ||
                screen instanceof HopperScreen ||
                screen instanceof ShulkerBoxScreen ||
                screen instanceof BrewingStandScreen ||
                screen instanceof BeaconScreen ||
                screen instanceof AnvilScreen ||
                screen instanceof EnchantmentScreen ||
                screen instanceof CartographyTableScreen ||
                screen instanceof GrindstoneScreen ||
                screen instanceof LoomScreen ||
                screen instanceof StonecutterScreen ||
                screen instanceof SmithingScreen ||
                screen instanceof HorseScreen ||
                screen instanceof MerchantScreen;
    }

    public static boolean isContainerScreen(Screen screen) {
        if (screen == null) {
            return false;
        }

        return screen instanceof InventoryScreen ||
                screen instanceof CraftingScreen ||
                screen instanceof GenericContainerScreen ||
                screen instanceof FurnaceScreen ||
                screen instanceof BlastFurnaceScreen ||
                screen instanceof SmokerScreen ||
                screen instanceof HopperScreen ||
                screen instanceof ShulkerBoxScreen ||
                screen instanceof BrewingStandScreen ||
                screen instanceof BeaconScreen ||
                screen instanceof AnvilScreen ||
                screen instanceof EnchantmentScreen ||
                screen instanceof CartographyTableScreen ||
                screen instanceof GrindstoneScreen ||
                screen instanceof LoomScreen ||
                screen instanceof StonecutterScreen ||
                screen instanceof SmithingScreen ||
                screen instanceof HorseScreen ||
                screen instanceof MerchantScreen ||
                screen instanceof CreativeInventoryScreen;
    }

    public static boolean isMyInventory(Screen screen) {
        if (screen == null) {
            return false;
        }

        return screen instanceof InventoryScreen ||
                screen instanceof CreativeInventoryScreen;
    }
}
