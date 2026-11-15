package elexis.client.utils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import elexis.client.utils.client.IMinecraft;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FriendStorage implements IMinecraft {
    private final File file;
    private final List<Friend> friendNicks = new ArrayList<>();
    private final java.util.Set<String> friendIndex=new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STAFF_LIST_TYPE = new TypeToken<List<Friend>>() {}.getType();

    @Getter
    public static class Friend {
        private final String name;

        public Friend(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Friend friend)) return false;
            return name.equalsIgnoreCase(friend.name);
        }

        @Override
        public int hashCode() {
            return name.toLowerCase().hashCode();
        }
    }

    public FriendStorage() {
        this.file = new File(new File(MinecraftClient.getInstance().runDirectory, "Elexis/files"), "friends.json");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        load();
    }

    public void add(String nick) {
        if (nick == null || nick.trim().isEmpty()) return;
        String trimmed = nick.trim();
        if (friendIndex.add(trimmed)) {
            friendNicks.add(new Friend(trimmed));
            save();
        }
    }

    public void remove(String nick) {
        if (nick != null) {
            friendIndex.remove(nick.trim());
            friendNicks.removeIf(friend -> friend.getName().equalsIgnoreCase(nick.trim()));
            save();
        }
    }

    public void clear() {
        friendNicks.clear();
        friendIndex.clear();
        save();
    }

    public List<Friend> getFriends() {
        return new ArrayList<>(friendNicks);
    }

    public boolean isFriend(String name) {
        if (name == null) return false;
        return friendIndex.contains(name);
    }

    private void save() {
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(friendNicks, STAFF_LIST_TYPE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean exists(String name) {
        if (name == null || name.isEmpty()) return false;
        return friendIndex.contains(name.trim());
    }

    private void load() {
        if (!file.exists()) return;

        try (Reader reader = new FileReader(file)) {
            List<Friend> loaded = GSON.fromJson(reader, STAFF_LIST_TYPE);
            if (loaded != null) {
                friendNicks.clear();
                friendNicks.addAll(loaded);
                friendIndex.clear();
                for(Friend friend:loaded) friendIndex.add(friend.getName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
