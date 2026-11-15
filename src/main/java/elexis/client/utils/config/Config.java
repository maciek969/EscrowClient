package elexis.client.utils.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elexis.Elexis;
import elexis.client.utils.client.IMinecraft;
import elexis.modules.api.settings.api.Setting;
import elexis.modules.api.settings.impl.*;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Getter
public class Config implements IMinecraft {
    private final File file;
    private final String name;
    private final List<Setting<?>> settings = new ArrayList<>();
    public static final Map<Item, Integer> itemBinds = new HashMap<>();

    public Config(String name) {
        this.name = name;
        this.file = new File(new File(MinecraftClient.getInstance().runDirectory, "\\Elexis\\configs"), name + ".json");
    }

    public void addSetting(Setting<?> setting) {
        settings.add(setting);
    }

    public void loadConfig(JsonObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        Elexis.noNeedSounds = true;

        if (jsonObject.has("modules")) {
            loadFunctionSettings(jsonObject.getAsJsonObject("modules"));
        }

        if (jsonObject.has("settings")) {
            JsonObject settingsObject = jsonObject.getAsJsonObject("settings");
            for (Setting<?> setting : settings) {
                if (settingsObject.has(setting.getName())) {
                    JsonElement settingElement = settingsObject.get(setting.getName());
                    if (settingElement != null && !settingElement.isJsonNull()) {
                        if (setting instanceof ColorSetting) {
                            ((ColorSetting) setting).set(settingElement.getAsInt());
                            ((ColorSetting) setting).updateHSB();
                        } else if (setting instanceof SliderSetting) {
                            ((SliderSetting) setting).set(settingElement.getAsFloat());
                        } else if (setting instanceof BooleanSetting) {
                            ((BooleanSetting) setting).set(settingElement.getAsBoolean());
                        } else if (setting instanceof ModeSetting) {
                            ((ModeSetting) setting).set(settingElement.getAsString());
                        } else if (setting instanceof BindSetting) {
                            ((BindSetting) setting).set(settingElement.getAsInt());
                        } else if (setting instanceof StringSetting) {
                            ((StringSetting) setting).set(settingElement.getAsString());
                        }
                    }
                }
            }
        }

        if (jsonObject.has("item_binds")) {
            JsonObject bindsObject = jsonObject.getAsJsonObject("item_binds");
            itemBinds.clear();
            for (String itemName : bindsObject.keySet()) {
                Item item = getItemFromString(itemName);
                if (item != null) {
                    int bind = bindsObject.get(itemName).getAsInt();
                    itemBinds.put(item, bind);
                }
            }
        }

        /*
        if (jsonObject.has("theme")) {
            String themeName = jsonObject.get("theme").getAsString();
            try {
                ThemeList theme = ThemeList.valueOf(themeName);
                Elexis.getInstance().getTheme().setTheme(theme);
            } catch (IllegalArgumentException e) {
            }
        }

         */

        // Loading Overlay element positions

        /*
        if (jsonObject.has("overlay_positions")) {
            JsonObject positionsObject = jsonObject.getAsJsonObject("overlay_positions");
            Interface.positions.clear();
            for (String element : positionsObject.keySet()) {
                JsonObject posObject = positionsObject.getAsJsonObject(element);
                float relativeX = posObject.get("relativeX").getAsFloat();
                float relativeY = posObject.get("relativeY").getAsFloat();
                Interface.positions.put(element, new Interface.ElementPosition(relativeX, relativeY));
            }
            Interface.positionsInitialized = true;
        }

         */

        Elexis.noNeedSounds = false;
    }

    private void loadFunctionSettings(JsonObject functionsObject) {
        Elexis.getFunctionManager().getFunctions().forEach(module -> {
            String key = module.getName().toLowerCase(java.util.Locale.ROOT);
            if (key.equals("customswing") && !functionsObject.has(key)) {
                key = "swinganimation";
            }
            JsonObject moduleObject = functionsObject.getAsJsonObject(key);
            if (moduleObject == null) {
                return;
            }

            module.setState(false);
            loadSettingFromJson(moduleObject, "bind", value -> module.setBind(value.getAsInt()));
            loadSettingFromJson(moduleObject, "state", value -> module.setState(value.getAsBoolean()));
            module.getSettings().forEach(setting -> loadIndividualSetting(moduleObject, setting));
        });
    }

    private void loadIndividualSetting(JsonObject moduleObject, Setting<?> setting) {
        JsonElement settingElement = moduleObject.get(setting.getName());
        if (settingElement == null || settingElement.isJsonNull()) {
            return;
        }

        if (setting instanceof SliderSetting) {
            ((SliderSetting) setting).set(settingElement.getAsFloat());
        } else if (setting instanceof BooleanSetting) {
            ((BooleanSetting) setting).set(settingElement.getAsBoolean());
        } else if (setting instanceof ColorSetting) {
            ((ColorSetting) setting).set(settingElement.getAsInt());
            ((ColorSetting) setting).updateHSB();
        } else if (setting instanceof ModeSetting) {
            ((ModeSetting) setting).set(settingElement.getAsString());
        } else if (setting instanceof ModeListSetting) {
            loadModeListSetting((ModeListSetting) setting, moduleObject);
        } else if (setting instanceof BindSetting) {
            ((BindSetting) setting).set(settingElement.getAsInt());
        } else if (setting instanceof StringSetting string) {
            string.set(settingElement.getAsString());
        }
    }

    private void loadModeListSetting(ModeListSetting setting, JsonObject moduleObject) {
        JsonObject elements = moduleObject.getAsJsonObject(setting.getName());
        setting.get().forEach(option -> {
            JsonElement optionElement = elements.get(option.getName());
            if (optionElement != null && !optionElement.isJsonNull()) {
                option.set(optionElement.getAsBoolean());
            }
        });
    }

    private void loadSettingFromJson(JsonObject jsonObject, String key, Consumer<JsonElement> consumer) {
        JsonElement element = jsonObject.get(key);
        if (element != null && !element.isJsonNull()) {
            consumer.accept(element);
        }
    }

    public JsonElement saveConfig() {
        JsonObject functionsObject = new JsonObject();
        saveFunctionSettings(functionsObject);

        JsonObject settingsObject = new JsonObject();
        for (Setting<?> setting : settings) {
            saveIndividualSetting(settingsObject, setting);
        }

        JsonObject bindsObject = new JsonObject();
        for (Map.Entry<Item, Integer> entry : itemBinds.entrySet()) {
            String itemName = getStringFromItem(entry.getKey());
            if (itemName != null) {
                bindsObject.addProperty(itemName, entry.getValue());
            }
        }

        /*
        String themeName = Elexis.getInstance().getTheme().getTheme().name();
        JsonObject positionsObject = new JsonObject();
        for (var entry : Interface.positions.entrySet()) {
            JsonObject posObject = new JsonObject();
            posObject.addProperty("relativeX", entry.getValue().relativeX);
            posObject.addProperty("relativeY", entry.getValue().relativeY);
            positionsObject.add(entry.getKey(), posObject);
        }

         */

        JsonObject newObject = new JsonObject();
        newObject.add("modules", functionsObject);
        if (!settings.isEmpty()) {
            newObject.add("settings", settingsObject);
        }
        newObject.add("item_binds", bindsObject);

        // newObject.addProperty("theme", themeName);
        // newObject.add("overlay_positions", positionsObject);

        return newObject;
    }

    private void saveFunctionSettings(JsonObject functionsObject) {
        Elexis.getFunctionManager().getFunctions().forEach(module -> {
            JsonObject moduleObject = new JsonObject();
            moduleObject.addProperty("bind", module.getBind());
            moduleObject.addProperty("state", module.isState());

            module.getSettings().forEach(setting -> saveIndividualSetting(moduleObject, setting));

            functionsObject.add(module.getName().toLowerCase(), moduleObject);
        });
    }

    private void saveIndividualSetting(JsonObject moduleObject, Setting<?> setting) {
        if (setting instanceof BooleanSetting) {
            moduleObject.addProperty(setting.getName(), ((BooleanSetting) setting).get());
        } else if (setting instanceof SliderSetting) {
            moduleObject.addProperty(setting.getName(), ((SliderSetting) setting).get());
        } else if (setting instanceof ModeSetting) {
            moduleObject.addProperty(setting.getName(), ((ModeSetting) setting).get());
        } else if (setting instanceof ColorSetting) {
            moduleObject.addProperty(setting.getName(), ((ColorSetting) setting).get());
        } else if (setting instanceof ModeListSetting) {
            saveModeListSetting(moduleObject, (ModeListSetting) setting);
        } else if (setting instanceof BindSetting) {
            moduleObject.addProperty(setting.getName(), ((BindSetting) setting).get());
        } else if (setting instanceof StringSetting string) {
            moduleObject.addProperty(setting.getName(), string.get());
        }
    }

    private void saveModeListSetting(JsonObject moduleObject, ModeListSetting setting) {
        JsonObject elements = new JsonObject();
        setting.get().forEach(option -> elements.addProperty(option.getName(), option.get()));
        moduleObject.add(setting.getName(), elements);
    }

    private static Item getItemFromString(String itemName) {
        switch (itemName) {
            case "ENDER_EYE": return Items.ENDER_EYE;
            case "NETHERITE_SCRAP": return Items.NETHERITE_SCRAP;
            case "PHANTOM_MEMBRANE": return Items.PHANTOM_MEMBRANE;
            case "SUGAR": return Items.SUGAR;
            case "DRIED_KELP": return Items.DRIED_KELP;
            case "SNOWBALL": return Items.SNOWBALL;
            case "COMPASS": return Items.COMPASS;
            default: return null;
        }
    }

    private static String getStringFromItem(Item item) {
        if (item == Items.ENDER_EYE) return "ENDER_EYE";
        if (item == Items.NETHERITE_SCRAP) return "NETHERITE_SCRAP";
        if (item == Items.PHANTOM_MEMBRANE) return "PHANTOM_MEMBRANE";
        if (item == Items.SUGAR) return "SUGAR";
        if (item == Items.DRIED_KELP) return "DRIED_KELP";
        if (item == Items.SNOWBALL) return "SNOWBALL";
        if (item == Items.COMPASS) return "COMPASS";
        return null;
    }
}
