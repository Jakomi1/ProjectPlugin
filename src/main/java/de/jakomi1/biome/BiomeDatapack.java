package de.jakomi1.biome;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class BiomeDatapack {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final String namespace;
    private final String packName;
    private final String description;
    private final List<BiomeDefinition> biomes;
    private final String dimensionName;
    private final String dimensionType;

    BiomeDatapack(String namespace, String packName, String description,
                  List<BiomeDefinition> biomes, String dimensionName, String dimensionType) {
        this.namespace = namespace;
        this.packName = packName;
        this.description = description;
        this.biomes = biomes;
        this.dimensionName = dimensionName;
        this.dimensionType = dimensionType;
    }

    public void write(File worldFolder) throws IOException {
        File packRoot = new File(new File(worldFolder, "datapacks"), packName);
        if (packRoot.exists()) {
            deleteRecursively(packRoot);
        }
        packRoot.mkdirs();

        writeFile(packRoot, "pack.mcmeta", packMcmeta());

        for (BiomeDefinition biome : biomes) {
            File biomeFile = new File(packRoot,
                    "data/" + namespace + "/worldgen/biome/" + biome.path().replace('.', '/') + ".json");
            writeFile(biomeFile.getParentFile(), biomeFile.getName(), biomeJson(biome));
        }

        for (Map.Entry<String, Set<String>> tag : collectTags().entrySet()) {
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

        if (dimensionName != null && !biomes.isEmpty()) {
            File dimensionFile = new File(packRoot,
                    "data/" + namespace + "/dimension/" + dimensionName + ".json");
            writeFile(dimensionFile.getParentFile(), dimensionFile.getName(), dimensionJson());
        }
    }

    private JsonObject packMcmeta() {
        JsonObject pack = new JsonObject();
        pack.addProperty("description", description);

        JsonArray minFormat = new JsonArray();
        minFormat.add(107);
        minFormat.add(1);
        pack.add("min_format", minFormat);

        JsonArray maxFormat = new JsonArray();
        maxFormat.add(107);
        maxFormat.add(1);
        pack.add("max_format", maxFormat);

        JsonObject root = new JsonObject();
        root.add("pack", pack);
        return root;
    }

    private JsonObject biomeJson(BiomeDefinition biome) {
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
        if (attributes.size() > 0) {
            root.add("attributes", attributes);
        }

        JsonArray carvers = new JsonArray();
        for (String carver : biome.carvers()) {
            carvers.add(carver);
        }
        root.add("carvers", carvers);

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
        if (effects.size() > 0) {
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

    private Map<String, Set<String>> collectTags() {
        Map<String, Set<String>> tags = new TreeMap<>();
        for (BiomeDefinition biome : biomes) {
            for (String tag : biome.tags()) {
                String tagId = tag.startsWith("#") ? tag.substring(1) : tag;
                tags.computeIfAbsent(tagId, k -> new java.util.LinkedHashSet<>()).add(biome.id());
            }
        }
        return tags;
    }

    private JsonObject tagJson(Set<String> biomeIds) {
        JsonObject root = new JsonObject();
        root.addProperty("replace", false);

        JsonArray values = new JsonArray();
        for (String id : biomeIds) {
            values.add(id);
        }
        root.add("values", values);
        return root;
    }

    private JsonObject dimensionJson() {
        JsonObject root = new JsonObject();
        root.addProperty("type", dimensionType);

        JsonObject generator = new JsonObject();
        generator.addProperty("type", "minecraft:noise");
        generator.addProperty("settings", "minecraft:overworld");

        JsonObject biomeSource = new JsonObject();
        biomeSource.addProperty("type", "minecraft:multi_noise");

        JsonArray biomesArray = new JsonArray();
        for (BiomeDefinition biome : biomes) {
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

            biomesArray.add(entry);
        }
        biomeSource.add("biomes", biomesArray);

        generator.add("biome_source", biomeSource);
        root.add("generator", generator);
        return root;
    }

    private static void writeFile(File parent, String name, JsonObject content) throws IOException {
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        File file = new File(parent, name);
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(content, writer);
        }
    }

    private static void deleteRecursively(File file) throws IOException {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        Files.delete(file.toPath());
    }
}
