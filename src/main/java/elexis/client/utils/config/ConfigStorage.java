package elexis.client.utils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elexis.client.utils.client.IMinecraft;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigStorage implements IMinecraft {
    public final File CONFIG_DIR = new File(MinecraftClient.getInstance().runDirectory, "Elexis" + File.separator + "configs");
    public final File AUTOCFG_DIR = new File(CONFIG_DIR, "backup.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public void init() throws IOException {
      // new MeowInit().boostrap();
        setupFolder();
    }

    private void setupFolder() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }
        if (!AUTOCFG_DIR.exists()) {
            try {
                AUTOCFG_DIR.createNewFile();
                try (FileWriter writer = new FileWriter(AUTOCFG_DIR)) {
                    writer.write("{}");
                }
            } catch (IOException ignored) {
            }
        }
    }

    public boolean isEmpty() {
        return getConfigs().isEmpty();
    }

    public List<Config> getConfigs() {
        List<Config> configs = new ArrayList<>();
        File[] configFiles = CONFIG_DIR.listFiles();

        if (configFiles != null) {
            for (File configFile : configFiles) {
                if (configFile.isFile() && configFile.getName().endsWith(".json")) {
                    String configName = configFile.getName().replace(".json", "");
                    Config config = findConfig(configName);
                    if (config != null) {
                        configs.add(config);
                    }
                }
            }
        }
        return configs;
    }

    public void loadConfiguration(String configuration) {
        Config config = findConfig(configuration);
        if (config == null) {
            return;
        }

        try (FileReader reader = new FileReader(config.getFile())) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            config.loadConfig(object);
        } catch (IOException ignored) {
        }
    }

    public static void saveConfiguration(String configuration) {
        Config config = new Config(configuration);

        File configDir = config.getFile().getParentFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        try (FileWriter writer = new FileWriter(config.getFile())) {
            writer.write(GSON.toJson(config.saveConfig()));
        } catch (IOException ignored) {
        }
    }

    public void deleteConfiguration(String configuration) {
        File file = new File(CONFIG_DIR, configuration + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    public Config findConfig(String configName) {
        if (configName == null) return null;
        File file = new File(CONFIG_DIR, configName + ".json");
        return file.exists() ? new Config(configName) : null;
    }
}