package de.jakomi1.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.jakomi1.datapack.DatapackMcmeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Schreibt Biomes (und optional eine passende Dimension) in ein Datapack.
 *
 * <p>{@link #write(Path)} erzeugt zusaetzlich die {@code pack.mcmeta} fuer
 * ein eigenstaendiges Pack. Fuer ein gemeinsames Pack ueber
 * {@code de.jakomi1.datapack.DatapackBundle} wird stattdessen
 * {@link #writeContent(Path)} verwendet.</p>
 */
public final class BiomeDatapack {

    private final String namespace;
    private final String description;
    private final List<BiomeDefinition> biomes;
    private final String dimensionName;
    private final String dimensionType;

    public BiomeDatapack(String namespace, String packName, String description,
                         List<BiomeDefinition> biomes, String dimensionName, String dimensionType) {
        this.namespace = namespace;
        this.description = description;
        this.biomes = biomes;
        this.dimensionName = dimensionName;
        this.dimensionType = dimensionType;
    }

    public BiomeDatapack(String namespace, String description, List<BiomeDefinition> biomes) {
        this(namespace, null, description, biomes, null, null);
    }

    public void write(Path packRootPath) throws IOException {
        File packRoot = packRootPath.toFile();
        BiomeDatapackWriter.writeFile(packRoot, "pack.mcmeta", DatapackMcmeta.create(description));
        writeContent(packRootPath);
    }

    /**
     * Schreibt nur den Inhalt des Packs (Biomes und optionale Dimension),
     * ohne {@code pack.mcmeta}.
     */
    public void writeContent(Path packRootPath) throws IOException {
        File packRoot = packRootPath.toFile();

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
