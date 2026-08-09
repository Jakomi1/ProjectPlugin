package de.jakomi1.project.biome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BiomeDefinition {

    private final String namespace;
    private final String path;
    private final float temperature;
    private final float downfall;
    private final boolean hasPrecipitation;
    private final String skyColor;
    private final String fogColor;
    private final String waterColor;
    private final String waterFogColor;
    private final String grassColor;
    private final String foliageColor;
    private final List<String> carvers;
    private final List<List<String>> features;
    private final Map<SpawnCategory, List<SpawnEntry>> spawners;
    private final Set<String> tags;
    private final float dimensionTemperature;
    private final float dimensionHumidity;
    private final float dimensionContinentalness;
    private final float dimensionErosion;
    private final float dimensionDepth;
    private final float dimensionWeirdness;

    BiomeDefinition(BiomeBuilder builder) {
        this.namespace = builder.namespace();
        this.path = builder.path();
        this.temperature = builder.temperature();
        this.downfall = builder.downfall();
        this.hasPrecipitation = builder.hasPrecipitation();
        this.skyColor = builder.skyColor();
        this.fogColor = builder.fogColor() != null ? builder.fogColor() : builder.skyColor();
        this.waterColor = builder.waterColor();
        this.waterFogColor = builder.waterFogColor();
        this.grassColor = builder.grassColor();
        this.foliageColor = builder.foliageColor();
        this.carvers = Collections.unmodifiableList(new ArrayList<>(builder.carvers()));
        List<List<String>> featureCopy = new ArrayList<>();
        for (List<String> step : builder.features()) {
            featureCopy.add(Collections.unmodifiableList(new ArrayList<>(step)));
        }
        this.features = Collections.unmodifiableList(featureCopy);
        Map<SpawnCategory, List<SpawnEntry>> spawnerCopy = new LinkedHashMap<>();
        for (Map.Entry<SpawnCategory, List<SpawnEntry>> entry : builder.spawners().entrySet()) {
            spawnerCopy.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        this.spawners = Collections.unmodifiableMap(spawnerCopy);
        this.tags = Collections.unmodifiableSet(new LinkedHashSet<>(builder.tags()));
        this.dimensionTemperature = builder.dimensionTemperature();
        this.dimensionHumidity = builder.dimensionHumidity();
        this.dimensionContinentalness = builder.dimensionContinentalness();
        this.dimensionErosion = builder.dimensionErosion();
        this.dimensionDepth = builder.dimensionDepth();
        this.dimensionWeirdness = builder.dimensionWeirdness();
    }

    public String namespace() {
        return namespace;
    }

    public String path() {
        return path;
    }

    public String id() {
        return namespace + ":" + path;
    }

    public float temperature() {
        return temperature;
    }

    public float downfall() {
        return downfall;
    }

    public boolean hasPrecipitation() {
        return hasPrecipitation;
    }

    public String skyColor() {
        return skyColor;
    }

    public String fogColor() {
        return fogColor;
    }

    public String waterColor() {
        return waterColor;
    }

    public String waterFogColor() {
        return waterFogColor;
    }

    public String grassColor() {
        return grassColor;
    }

    public String foliageColor() {
        return foliageColor;
    }

    public List<String> carvers() {
        return carvers;
    }

    public List<List<String>> features() {
        return features;
    }

    public Map<SpawnCategory, List<SpawnEntry>> spawners() {
        return spawners;
    }

    public Set<String> tags() {
        return tags;
    }

    public float dimensionTemperature() {
        return dimensionTemperature;
    }

    public float dimensionHumidity() {
        return dimensionHumidity;
    }

    public float dimensionContinentalness() {
        return dimensionContinentalness;
    }

    public float dimensionErosion() {
        return dimensionErosion;
    }

    public float dimensionDepth() {
        return dimensionDepth;
    }

    public float dimensionWeirdness() {
        return dimensionWeirdness;
    }
}
