package de.jakomi1.project.pearl;

import de.jakomi1.database.KeyValueTable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PearlFixTable extends KeyValueTable<String, String> {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public PearlFixTable() {
        super("pearl_fix_table", String.class, String.class);
    }

    public void savePearls(UUID playerId, Set<SavedPearlState> pearls) {
        if (playerId == null) return;

        String key = playerId.toString();
        if (pearls == null || pearls.isEmpty()) {
            remove(key);
        } else {
            put(key, serialize(pearls));
        }
        flushNow();
    }

    public Set<SavedPearlState> loadPearls(UUID playerId) {
        if (playerId == null) return Collections.emptySet();

        String json = get(playerId.toString());
        if (json == null || json.isBlank()) return Collections.emptySet();

        return deserialize(json);
    }

    public void deletePearls(UUID playerId) {
        if (playerId == null) return;

        remove(playerId.toString());
        flushNow();
    }

    private static String serialize(Set<SavedPearlState> pearls) {
        JsonArray array = new JsonArray();

        for (SavedPearlState state : pearls) {
            JsonObject obj = new JsonObject();
            obj.addProperty("pearlId", state.pearlId().toString());
            obj.addProperty("ownerId", state.ownerId().toString());
            obj.addProperty("worldId", state.worldId().toString());
            obj.addProperty("x", state.x());
            obj.addProperty("y", state.y());
            obj.addProperty("z", state.z());
            obj.addProperty("vx", state.vx());
            obj.addProperty("vy", state.vy());
            obj.addProperty("vz", state.vz());
            array.add(obj);
        }

        return GSON.toJson(array);
    }

    private static Set<SavedPearlState> deserialize(String json) {
        Set<SavedPearlState> result = new HashSet<>();

        try {
            for (JsonElement element : JsonParser.parseString(json).getAsJsonArray()) {
                JsonObject obj = element.getAsJsonObject();
                result.add(new SavedPearlState(
                        UUID.fromString(obj.get("pearlId").getAsString()),
                        UUID.fromString(obj.get("ownerId").getAsString()),
                        UUID.fromString(obj.get("worldId").getAsString()),
                        obj.get("x").getAsDouble(),
                        obj.get("y").getAsDouble(),
                        obj.get("z").getAsDouble(),
                        obj.get("vx").getAsDouble(),
                        obj.get("vy").getAsDouble(),
                        obj.get("vz").getAsDouble()
                ));
            }
        } catch (RuntimeException ignored) {
        }

        return result;
    }
}
