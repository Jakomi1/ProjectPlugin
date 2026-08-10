package de.jakomi1.dimension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.jakomi1.biome.BiomeDefinition;
import de.jakomi1.util.PackFormat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

final class DimensionDatapack {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final String namespace;
    private final String packName;
    private final String description;
    private final List<DimensionDefinition> dimensions;

    DimensionDatapack(String namespace, String packName, String description,
                      List<DimensionDefinition> dimensions) {
        this.namespace = namespace;
        this.packName = packName;
        this.description = description;
        this.dimensions = dimensions;
    }

    public void write(Path packRootPath) throws IOException {
        File packRoot = packRootPath.toFile();

        writeFile(packRoot, "pack.mcmeta", packMcmeta());

        for (DimensionDefinition dimension : dimensions) {
            if (dimension.dimensionType() == null) {
                File typeFile = new File(packRoot,
                        "data/" + namespace + "/dimension_type/" + dimension.path().replace('.', '/') + ".json");
                writeFile(typeFile.getParentFile(), typeFile.getName(), dimensionTypeJson(dimension));
            }

            File dimensionFile = new File(packRoot,
                    "data/" + namespace + "/dimension/" + dimension.path().replace('.', '/') + ".json");
            writeFile(dimensionFile.getParentFile(), dimensionFile.getName(), dimensionJson(dimension));

            for (String tag : dimension.tags()) {
                writeDimensionTag(packRoot, dimension, tag);
            }
        }
    }

    private JsonObject packMcmeta() {
        int format = PackFormat.current();

        JsonObject pack = new JsonObject();
        pack.addProperty("description", description);

        JsonArray minFormat = new JsonArray();
        minFormat.add(format);
        minFormat.add(1);
        pack.add("min_format", minFormat);

        JsonArray maxFormat = new JsonArray();
        maxFormat.add(format);
        maxFormat.add(1);
        pack.add("max_format", maxFormat);

        JsonObject root = new JsonObject();
        root.add("pack", pack);
        return root;
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
                    biomesArray.add(biomeEntry(biome));
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

    private JsonObject biomeEntry(BiomeDefinition biome) {
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

    private void writeDimensionTag(File packRoot, DimensionDefinition dimension, String tag) throws IOException {
        String tagNamespace = namespace;
        String tagPath = tag;
        int colon = tag.indexOf(':');
        if (colon > 0) {
            tagNamespace = tag.substring(0, colon);
            tagPath = tag.substring(colon + 1);
        }

        JsonObject root = new JsonObject();
        root.addProperty("replace", false);
        JsonArray values = new JsonArray();
        values.add(dimension.id());
        root.add("values", values);

        File tagFile = new File(packRoot,
                "data/" + tagNamespace + "/tags/dimension/" + tagPath.replace('.', '/') + ".json");
        writeFile(tagFile.getParentFile(), tagFile.getName(), root);
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
}
