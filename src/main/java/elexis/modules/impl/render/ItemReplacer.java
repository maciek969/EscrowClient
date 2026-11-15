package elexis.modules.impl.render;

import elexis.Elexis;
import elexis.modules.api.Category;
import elexis.modules.api.Function;
import elexis.modules.api.FunctionAdd;
import elexis.modules.api.settings.impl.ModeSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.List;

@FunctionAdd(name = "Item Replacer", alias = "Item Replacer", category = Category.Render, description = "Replaces sword models with resource-pack variants")
public class ItemReplacer extends Function {
    private static final String[] MODELS = {
            "Abominable Blade",
            "Abominable Great Saber",
            "Abominable Scythe",
            "Acidic Cleaver",
            "Amethyst Shuriken",
            "Ancient Royal Great Sword",
            "Aquatic Sacred Blade",
            "Arcanethyst",
            "Ashura's Blade",
            "Awakened Lichblade",
            "Blood Edge",
            "Bloody Death",
            "Bramblethorn",
            "Brimstone Claymore",
            "Carian Knight's Sword",
            "Chrono Blade",
            "Corrupted Mythic Blade",
            "Creation Splitter",
            "Crescent Rose",
            "Cyber Katana",
            "Cyber Mantis Blade",
            "Cyber Sword",
            "Cybernetic Chainsaw Blade",
            "Cybernetic Katana",
            "Cybernetic Knife",
            "Dainsleif",
            "Dark Blade",
            "Dark Cleaver",
            "Death Knight's Dagger",
            "Death Knight's Sword",
            "Demigod's Unholy Blade",
            "Demigod's Unholy Halberd",
            "Demon Lord's Great Axe",
            "Demon Lord's Sword",
            "Demonic Blade",
            "Demonic Cleaver",
            "Divine Axe Rhitta",
            "Divine Justice",
            "Divine Punisher",
            "Divine Reaper",
            "Dragon Slaying blade",
            "Edge Of The Astral Plane",
            "Emberblade",
            "Enigma",
            "Epic Sword",
            "Estoc",
            "Fallen God's Spear",
            "Fallen God's Sword",
            "Floral Longsword",
            "Floral Sabre",
            "Forest Guardian's Glaive",
            "Frost Axe",
            "Frost Blade",
            "Frost Scythe",
            "Hearthflame",
            "Hero Sword",
            "Holy Moonlight Sword",
            "Hornet's Needle",
            "Icewhisper",
            "Jade Halberd",
            "Katana",
            "Legendary Sword",
            "Longsword",
            "Magi Scythe",
            "Masamune",
            "Mjolnir",
            "Molten Blade",
            "Molten Sword",
            "Muramasa",
            "Mystical Spellblade",
            "Mythic Blade",
            "Ocean's Rage",
            "Partisan",
            "Pharaoh's Treasure",
            "Pheonix Grace",
            "Plague Longsword",
            "Power Fuse Hammer",
            "Power Fuse Sword",
            "Requiem of the Ninth Abyss",
            "Ribbon Cleaver",
            "Righteous Relic",
            "Rivers Of Blood",
            "Royal Chakram",
            "Royal Rapier",
            "Sabre",
            "Scissor Blade",
            "Sculk Cleaver",
            "Sculk Scythe",
            "Sculk Sword",
            "Sentinel's Will",
            "Silverine Blade",
            "Soul Claws",
            "Soul Collector",
            "Soul Devourer",
            "Soul Edge",
            "Soul Harvester",
            "Soul Stealer",
            "Soulrender",
            "Star's Edge",
            "Steel Sword",
            "Stop Sign",
            "Storm Bringer",
            "Storm's Edge",
            "Sunbreak",
            "Tengen's Blade",
            "Terra Blade",
            "Thousand Demon Daggers",
            "Thunder Bringer",
            "Thunderbrand",
            "True Excalibur",
            "Vampiric Needle",
            "Wakizashi",
            "Watcher Claymore",
            "Watching Warglaive",
            "Waxweaver",
            "Whisperwind",
            "Wickpiercer",
            "Wraith Scythe",
            "Yoru"
    };

    private final ModeSetting model = new ModeSetting("Model", "Katana", MODELS);

    public ItemReplacer() {
        addSettings(model);
    }

    public static ItemReplacer getInstance() {
        return Elexis.getFunctionManager() != null ? Elexis.getFunctionManager().getItemReplacer() : null;
    }

    public ItemStack getRenderStack(ItemStack stack) {
        if (!isState() || stack == null || stack.isEmpty() || !isSword(stack)) {
            return stack;
        }

        ItemStack copy = stack.copy();
        copy.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                List.of(),
                List.of(),
                List.of(model.get()),
                List.of()
        ));
        return copy;
    }

    public void onEvent() {
    }

    private boolean isSword(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        return Registries.ITEM.getId(stack.getItem()).getPath().contains("sword");
    }
}