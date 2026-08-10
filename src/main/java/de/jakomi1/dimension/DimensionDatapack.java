package de.jakomi1.dimension;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.jakomi1.biome.BiomeDatapackWriter;
import de.jakomi1.biome.BiomeDefinition;
import de.jakomi1.datapack.DatapackMcmeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class DimensionDatapack {

    private final String namespace;
    private final String description;
    private final List<DimensionDefinition> dimensions;

    DimensionDatapack(String namespace, String packName, String description,
                      List<DimensionDefinition> dimensions) {
        this.namespace = namespace;
        this.description = description;
        this.dimensions = dimensions;
    }

    public void write(Path packRootPath) throws IOException {
        File packRoot = packRootPath.toFile();

        BiomeDatapackWriter.writeFile(packRoot, "pack.mcmeta", DatapackMcmeta.create(description));
        BiomeDatapackWriter.writeBiomes(packRoot, namespace, embeddedBiomes());

        for (DimensionDefinition dimension : dimensions) {
            if (dimension.dimensionType() == null) {
                File typeFile = new File(packRoot,
                        "data/" + namespace + "/dimension_type/" + dimension.path().replace('.', '/') + ".json");
                BiomeDatapackWriter.writeFile(typeFile.getParentFile(), typeFile.getName(), dimensionTypeJson(dimension));
            }

            File dimensionFile = new File(packRoot,
                    "data/" + namespace + "/dimension/" + dimension.path().replace('.', '/') + ".json");
            BiomeDatapackWriter.writeFile(dimensionFile.getParentFile(), dimensionFile.getName(), dimensionJson(dimension));
        }

        for (Map.Entry<String, Set<String>> tag : collectDimensionTags().entrySet()) {
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

            JsonObject root = new JsonObject();
            root.addProperty("replace", false);
            JsonArray values = new JsonArray();
            for (String id : tag.getValue()) {
                values.add(id);
            }
            root.add("values", values);

            File tagFile = new File(packRoot,
                    "data/" + tagNamespace + "/tags/dimension/" + tagPath.replace('.', '/') + ".json");
            BiomeDatapackWriter.writeFile(tagFile.getParentFile(), tagFile.getName(), root);
        }
    }

    private List<BiomeDefinition> embeddedBiomes() {
        Map<String, BiomeDefinition> biomes = new LinkedHashMap<>();
        for (DimensionDefinition dimension : dimensions) {
            if (!DimensionBuilder.SOURCE_MULTI_NOISE.equals(dimension.biomeSourceMode())) {
                continue;
            }
            for (BiomeDefinition biome : dimension.biomes()) {
                biomes.putIfAbsent(biome.id(), biome);
            }
        }
        return new ArrayList<>(biomes.values());
    }

    private Map<String, Set<String>> collectDimensionTags() {
        Map<String, Set<String>> tags = new TreeMap<>();
        for (DimensionDefinition dimension : dimensions) {
            for (String tag : dimension.tags()) {
                String tagId = tag.startsWith("#") ? tag.substring(1) : tag;
                tags.computeIfAbsent(tagId, k -> new LinkedHashSet<>()).add(dimension.id());
            }
        }
        return tags;
    }

    private JsonObject dimensionTypeJson(DimensionDefinition dimension) {
        JsonObject root = new JsonObject();
        root.addProperty("ultrawarm", dimension.ultrawarm());
        root.addProperty("natural", dimension.natural());
        root.addProperty("piglin_safe", dimension.piglinSafe());
        root.addProperty("respawn_anchor_works", dimension.respawnAnchorWorks());
        root.addProperty("bed_works", dimension.bedWorks());
        root.addProperty("has_raids", dimension.hasRaids());
        root.addProperty("has_skylight", dimension.hasSkylight());
        root.addProperty("has_ceiling", dimension.hasCeiling());
        root.addProperty("coordinate_scale", dimension.coordinateScale());
        root.addProperty("ambient_light", dimension.ambientLight());
        root.addProperty("logical_height", dimension.logicalHeight());
        root.addProperty("min_y", dimension.minY());
        root.addProperty("height", dimension.height());
        root.add("monster_spawn_light_level", monsterSpawnLight(dimension));
        root.addProperty("monster_spawn_block_light_limit", dimension.monsterSpawnBlockLightLimit());

        if (dimension.effects() != null) {
            root.addProperty("effects", dimension.effects());
        }
        if (dimension.infiniburn() != null) {
            root.addProperty("infiniburn", dimension.infiniburn());
        }
        if (dimension.fixedTime() != null) {
            root.addProperty("fixed_time", dimension.fixedTime());
        }

        return root;
    }

    private JsonObject monsterSpawnLight(DimensionDefinition dimension) {
        JsonObject root = new JsonObject();
        if (dimension.monsterSpawnLightConstantMode()) {
            root.addProperty("type", "minecraft:constant");
            root.addProperty("value", dimension.monsterSpawnLightMin());
        } else {
            root.addProperty("type", "minecraft:uniform");
            JsonObject value = new JsonObject();
            value.addProperty("min_inclusive", dimension.monsterSpawnLightMin());
            value.addProperty("max_inclusive", dimension.monsterSpawnLightMax());
            root.add("value", value);
        }
        return root;
    }

    private JsonObject dimensionJson(DimensionDefinition dimension) {
        JsonObject root = new JsonObject();
        String type = dimension.dimensionType() != null ? dimension.dimensionType() : dimension.id();
        root.addProperty("type", type);
        root.add("generator", generator(dimension));
        return root;
    }

    private JsonObject generator(DimensionDefinition dimension) {
        JsonObject generator = new JsonObject();

        switch (dimension.generatorType()) {
            case DimensionBuilder.GENERATOR_DEBUG -> {
                generator.addProperty("type", DimensionBuilder.GENERATOR_DEBUG);
            }
            case DimensionBuilder.GENERATOR_FLAT -> {
                generator.addProperty("type", DimensionBuilder.GENERATOR_FLAT);
                generator.add("settings", flatSettings(dimension));
            }
            default -> {
                generator.addProperty("type", DimensionBuilder.GENERATOR_NOISE);
                generator.addProperty("settings", dimension.noiseSettings());
                if (dimension.seed() != null) {
                    generator.addProperty("seed", dimension.seed());
                }
                if (dimension.amplified()) {
                    generator.addProperty("amplified", true);
                }
                generator.add("biome_source", biomeSource(dimension));
            }
        }

        return generator;
    }

    private JsonObject flatSettings(DimensionDefinition dimension) {
        JsonObject settings = new JsonObject();

        if (dimension.flatBiome() != null) {
            settings.addProperty("biome", dimension.flatBiome());
        }

        JsonArray layers = new JsonArray();
        for (DimensionBuilder.FlatLayer layer : dimension.flatLayers()) {
            JsonObject layerObject = new JsonObject();
            layerObject.addProperty("height", layer.height());
            layerObject.addProperty("block", layer.block());
            layers.add(layerObject);
        }
        settings.add("layers", layers);

        settings.addProperty("lakes", dimension.flatLakes());
        settings.addProperty("features", dimension.flatFeatures());

        if (!dimension.flatStructureOverrides().isEmpty()) {
            JsonArray overrides = new JsonArray();
            for (String structure : dimension.flatStructureOverrides()) {
                overrides.add(structure);
            }
            settings.add("structure_overrides", overrides);
        }

        return settings;
    }

    private JsonObject biomeSource(DimensionDefinition dimension) {
        JsonObject biomeSource = new JsonObject();

        switch (dimension.biomeSourceMode()) {
            case DimensionBuilder.SOURCE_FIXED -> {
                biomeSource.addProperty("type", "minecraft:fixed");
                if (dimension.fixedBiome() != null) {
                    biomeSource.addProperty("biome", dimension.fixedBiome());
                }
            }
            case DimensionBuilder.SOURCE_MULTI_NOISE -> {
                biomeSource.addProperty("type", "minecraft:multi_noise");
                JsonArray biomesArray = new JsonArray();
                for (BiomeDefinition biome : dimension.biomes()) {
                    biomesArray.add(BiomeDatapackWriter.multiNoiseBiomeEntry(biome));
                }
                biomeSource.add("biomes", biomesArray);
            }
            case DimensionBuilder.SOURCE_CHECKERBOARD -> {
                biomeSource.addProperty("type", "minecraft:checkerboard");
                JsonArray biomesArray = new JsonArray();
                for (String biome : dimension.checkerboardBiomes()) {
                    biomesArray.add(biome);
                }
                biomeSource.add("biomes", biomesArray);
                biomeSource.addProperty("scale", dimension.checkerboardScale());
            }
            case DimensionBuilder.SOURCE_THE_END -> {
                biomeSource.addProperty("type", "minecraft:the_end");
            }
            default -> {
                biomeSource.addProperty("type", "minecraft:multi_noise");
                biomeSource.addProperty("preset", dimension.biomeSourcePreset());
            }
        }

        return biomeSource;
    }
}
