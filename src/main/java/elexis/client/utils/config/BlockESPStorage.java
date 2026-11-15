package elexis.client.utils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BlockESPStorage {
    private final File file;
    private final List<Block> blocks = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public BlockESPStorage() {
        this.file = new File(new File(MinecraftClient.getInstance().runDirectory, "Elexis/files"), "blockesp.json");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        load();
    }

    public void add(Block block) {
        if (block != null && !blocks.contains(block) && block != Registries.BLOCK.get(Registries.BLOCK.getDefaultId())) {
            blocks.add(block);
            save();
        }
    }

    public void remove(Block block) {
        if (block != null) {
            blocks.remove(block);
            save();
        }
    }

    public void clear() {
        blocks.clear();
        save();
    }

    public List<Block> getBlocks() {
        return new ArrayList<>(blocks);
    }

    private void save() {
        try (Writer writer = new FileWriter(file)) {
            List<String> blockIds = blocks.stream()
                    .map(block -> Registries.BLOCK.getId(block).toString())
                    .collect(Collectors.toList());
            GSON.toJson(blockIds, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            List<String> blockIds = GSON.fromJson(reader, new TypeToken<List<String>>(){}.getType());
            if (blockIds != null) {
                blocks.clear();
                blocks.addAll(blockIds.stream()
                        .map(id -> {
                            try {
                                return Identifier.of(id);
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(id -> id != null)
                        .map(id -> Registries.BLOCK.get(id))
                        .filter(block -> block != Registries.BLOCK.get(Registries.BLOCK.getDefaultId()))
                        .collect(Collectors.toList()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}