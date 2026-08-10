package de.jakomi1.biome;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class BiomeDatapackWriter {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private BiomeDatapackWriter() {
    }

    public static void writeBiomes(File packRoot, String namespace, Collection<BiomeDefinition> biomes) throws IOException {
        if (biomes.isEmpty()) {
            return;
        }

        for (BiomeDefinition biome : biomes) {
            File biomeFile = new File(packRoot,
                    "data/" + namespace + "/worldgen/biome/" + biome.path().replace('.', '/') + ".json");
            writeFile(biomeFile.getParentFile(), biomeFile.getName(), biomeJson(biome));
        }

        for (Map.Entry<String, Set<String>> tag : collectTags(biomes).entrySet()) {
            String tagNamespace;
            String tagPath;
            String tagId = tag.getKey();
            int colon = tagId.indexOf(':');
            if (colon > 0) {
                tagNamespace = tagId.substring(0, colon);
                tagPath = tagId.substring(colon + 1);
            } else {
                tagNamespace = namespace;
                tagPath = tagId;
            }
            File tagFile = new File(packRoot, "data/" + tagNamespace + "/tags/worldgen/biome/" + tagPath.replace('.', '/') + ".json");
            writeFile(tagFile.getParentFile(), tagFile.getName(), tagJson(tag.getValue()));
        }
    }

    public static JsonObject multiNoiseBiomeEntry(BiomeDefinition biome) {
        JsonObject entry = new JsonObject();
        entry.addProperty("biome", biome.id());

        JsonObject parameters = new JsonObject();
        parameters.addProperty("temperature", biome.dimensionTemperature());
        parameters.addProperty("humidity", biome.dimensionHumidity());
        parameters.addProperty("continentalness", biome.dimensionContinentalness());
        parameters.addProperty("erosion", biome.dimensionErosion());
        parameters.addProperty("depth", biome.dimensionDepth());
        parameters.addProperty("weirdness", biome.dimensionWeirdness());
        parameters.addProperty("offset", 0.0F);
        entry.add("parameters", parameters);

        return entry;
    }

    private static Map<String, Set<String>> collectTags(Collection<BiomeDefinition> biomes) {
        Map<String, Set<String>> tags = new TreeMap<>();
        for (BiomeDefinition biome : biomes) {
            for (String tag : biome.tags()) {
                String tagId = tag.startsWith("#") ? tag.substring(1) : tag;
                tags.computeIfAbsent(tagId, k -> new LinkedHashSet<>()).add(biome.id());
            }
        }
        return tags;
    }

    private static JsonObject tagJson(Set<String> biomeIds) {
        JsonObject root = new JsonObject();
        root.addProperty("replace", false);

        JsonArray values = new JsonArray();
        for (String id : biomeIds) {
            values.add(id);
        }
        root.add("values", values);
        return root;
    }

    private static JsonObject biomeJson(BiomeDefinition biome) {
        JsonObject root = new JsonObject();

        JsonObject attributes = new JsonObject();
        if (biome.skyColor() != null) {
            attributes.addProperty("minecraft:visual/sky_color", biome.skyColor());
        }
        if (biome.fogColor() != null) {
            attributes.addProperty("minecraft:visual/fog_color", biome.fogColor());
        }
        if (biome.waterFogColor() != null) {
            attributes.addProperty("minecraft:visual/water_fog_color", biome.waterFogColor());
        }
        if (biome.increasedFireBurnout()) {
            attributes.addProperty("minecraft:gameplay/increased_fire_burnout", true);
        }
        addAmbientSounds(attributes, biome);
        addBackgroundMusic(attributes, biome);
        addAmbientParticles(attributes, biome);
        if (!attributes.isEmpty()) {
            root.add("attributes", attributes);
        }

        if (biome.carvers().size() == 1) {
            root.addProperty("carvers", biome.carvers().get(0));
        } else {
            JsonArray carvers = new JsonArray();
            for (String carver : biome.carvers()) {
                carvers.add(carver);
            }
            root.add("carvers", carvers);
        }

        root.addProperty("downfall", biome.downfall());

        JsonObject effects = new JsonObject();
        if (biome.waterColor() != null) {
            effects.addProperty("water_color", biome.waterColor());
        }
        if (biome.grassColor() != null) {
            effects.addProperty("grass_color", biome.grassColor());
        }
        if (biome.foliageColor() != null) {
            effects.addProperty("foliage_color", biome.foliageColor());
        }
        if (!effects.isEmpty()) {
            root.add("effects", effects);
        }

        JsonArray features = new JsonArray();
        for (List<String> step : biome.features()) {
            JsonArray stepArray = new JsonArray();
            for (String feature : step) {
                stepArray.add(feature);
            }
            features.add(stepArray);
        }
        root.add("features", features);

        root.addProperty("has_precipitation", biome.hasPrecipitation());

        root.add("spawn_costs", new JsonObject());

        JsonObject spawners = new JsonObject();
        for (SpawnCategory category : SpawnCategory.values()) {
            JsonArray entries = new JsonArray();
            for (SpawnEntry entry : biome.spawners().getOrDefault(category, List.of())) {
                JsonObject spawnObject = new JsonObject();
                spawnObject.addProperty("type", entry.type());
                spawnObject.addProperty("weight", entry.weight());
                spawnObject.addProperty("minCount", entry.minCount());
                spawnObject.addProperty("maxCount", entry.maxCount());
                entries.add(spawnObject);
            }
            spawners.add(category.key(), entries);
        }
        root.add("spawners", spawners);

        root.addProperty("temperature", biome.temperature());

        return root;
    }

    private static void addAmbientSounds(JsonObject attributes, BiomeDefinition biome) {
        String loop = biome.ambientSoundLoop();
        String additions = biome.ambientSoundAdditions();
        String mood = biome.ambientSoundMood();
        if (loop == null && additions == null && mood == null) return;

        JsonObject sounds = new JsonObject();
        if (additions != null) {
            JsonObject additionsObject = new JsonObject();
            additionsObject.addProperty("sound", additions);
            additionsObject.addProperty("tick_chance", biome.ambientSoundAdditionsChance());
            sounds.add("additions", additionsObject);
        }
        if (loop != null) {
            sounds.addProperty("loop", loop);
        }
        if (mood != null) {
            JsonObject moodObject = new JsonObject();
            moodObject.addProperty("block_search_extent", biome.ambientSoundMoodBlockSearchExtent());
            moodObject.addProperty("offset", biome.ambientSoundMoodOffset());
            moodObject.addProperty("sound", mood);
            moodObject.addProperty("tick_delay", biome.ambientSoundMoodTickDelay());
            sounds.add("mood", moodObject);
        }
        attributes.add("minecraft:audio/ambient_sounds", sounds);
    }

    private static void addBackgroundMusic(JsonObject attributes, BiomeDefinition biome) {
        String music = biome.backgroundMusic();
        if (music == null) return;

        JsonObject musicObject = new JsonObject();
        musicObject.addProperty("max_delay", biome.backgroundMusicMaxDelay());
        musicObject.addProperty("min_delay", biome.backgroundMusicMinDelay());
        musicObject.addProperty("sound", music);

        JsonObject defaultMusic = new JsonObject();
        defaultMusic.add("default", musicObject);
        attributes.add("minecraft:audio/background_music", defaultMusic);
    }

    private static void addAmbientParticles(JsonObject attributes, BiomeDefinition biome) {
        if (biome.ambientParticles().isEmpty()) return;

        JsonArray particles = new JsonArray();
        for (BiomeBuilder.ParticleEntry entry : biome.ambientParticles()) {
            JsonObject particleObject = new JsonObject();
            JsonObject particle = new JsonObject();
            particle.addProperty("type", entry.type());
            particleObject.add("particle", particle);
            particleObject.addProperty("probability", entry.probability());
            particles.add(particleObject);
        }
        attributes.add("minecraft:visual/ambient_particles", particles);
    }

    public static void writeFile(File parent, String name, JsonObject content) throws IOException {
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File file = new File(parent, name);
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(content, writer);
        }
    }
}
