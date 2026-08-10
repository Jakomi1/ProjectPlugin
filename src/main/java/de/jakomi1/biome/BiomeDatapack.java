package de.jakomi1.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.jakomi1.datapack.DatapackMcmeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

final class BiomeDatapack {

    private final String namespace;
    private final String description;
    private final List<BiomeDefinition> biomes;
    private final String dimensionName;
    private final String dimensionType;

    BiomeDatapack(String namespace, String packName, String description,
                  List<BiomeDefinition> biomes, String dimensionName, String dimensionType) {
        this.namespace = namespace;
        this.description = description;
        this.biomes = biomes;
        this.dimensionName = dimensionName;
        this.dimensionType = dimensionType;
    }

    public void write(Path packRootPath) throws IOException {
        File packRoot = packRootPath.toFile();

        BiomeDatapackWriter.writeFile(packRoot, "pack.mcmeta", DatapackMcmeta.create(description));
        BiomeDatapackWriter.writeBiomes(packRoot, namespace, biomes);

        if (dimensionName != null && !biomes.isEmpty()) {
            File dimensionFile = new File(packRoot,
                    "data/" + namespace + "/dimension/" + dimensionName + ".json");
            BiomeDatapackWriter.writeFile(dimensionFile.getParentFile(), dimensionFile.getName(), dimensionJson());
        }
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
            biomesArray.add(BiomeDatapackWriter.multiNoiseBiomeEntry(biome));
        }
        biomeSource.add("biomes", biomesArray);

        generator.add("biome_source", biomeSource);
        root.add("generator", generator);
        return root;
    }
}
