package de.jakomi1.project.biome;

import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BiomeBuilder {

    private final BiomeManager manager;
    private final String namespace;
    private final String path;
    private float temperature = 0.5F;
    private float downfall = 0.4F;
    private boolean hasPrecipitation = true;
    private String skyColor = "#78a7ff";
    private String fogColor;
    private String waterColor = "#3f76e4";
    private String waterFogColor;
    private String grassColor;
    private String foliageColor;
    private final List<String> carvers = new ArrayList<>();
    private final List<List<String>> features = new ArrayList<>();
    private final Map<SpawnCategory, List<SpawnEntry>> spawners = new LinkedHashMap<>();
    private final Set<String> tags = new LinkedHashSet<>();
    private float dimensionTemperature = 0.5F;
    private float dimensionHumidity = 0.0F;
    private float dimensionContinentalness = 0.0F;
    private float dimensionErosion = 0.0F;
    private float dimensionDepth = 0.0F;
    private float dimensionWeirdness = 0.0F;

    BiomeBuilder(BiomeManager manager, String namespace, String path) {
        this.manager = manager;
        this.namespace = namespace;
        this.path = path;
    }

    public BiomeBuilder temperature(float temperature) {
        this.temperature = temperature;
        return this;
    }

    public BiomeBuilder downfall(float downfall) {
        this.downfall = downfall;
        return this;
    }

    public BiomeBuilder precipitation(boolean hasPrecipitation) {
        this.hasPrecipitation = hasPrecipitation;
        return this;
    }

    public BiomeBuilder skyColor(int rgb) {
        this.skyColor = toHex(rgb);
        return this;
    }

    public BiomeBuilder skyColor(String hex) {
        this.skyColor = hex;
        return this;
    }

    public BiomeBuilder fogColor(int rgb) {
        this.fogColor = toHex(rgb);
        return this;
    }

    public BiomeBuilder fogColor(String hex) {
        this.fogColor = hex;
        return this;
    }

    public BiomeBuilder waterColor(int rgb) {
        this.waterColor = toHex(rgb);
        return this;
    }

    public BiomeBuilder waterColor(String hex) {
        this.waterColor = hex;
        return this;
    }

    public BiomeBuilder waterFogColor(int rgb) {
        this.waterFogColor = toHex(rgb);
        return this;
    }

    public BiomeBuilder waterFogColor(String hex) {
        this.waterFogColor = hex;
        return this;
    }

    public BiomeBuilder grassColor(int rgb) {
        this.grassColor = toHex(rgb);
        return this;
    }

    public BiomeBuilder grassColor(String hex) {
        this.grassColor = hex;
        return this;
    }

    public BiomeBuilder foliageColor(int rgb) {
        this.foliageColor = toHex(rgb);
        return this;
    }

    public BiomeBuilder foliageColor(String hex) {
        this.foliageColor = hex;
        return this;
    }

    public BiomeBuilder carver(String carver) {
        this.carvers.add(carver);
        return this;
    }

    public BiomeBuilder feature(String feature) {
        if (this.features.isEmpty()) {
            this.features.add(new ArrayList<>());
        }
        this.features.get(this.features.size() - 1).add(feature);
        return this;
    }

    public BiomeBuilder featureStep(String... features) {
        List<String> step = new ArrayList<>();
        java.util.Collections.addAll(step, features);
        this.features.add(step);
        return this;
    }

    public BiomeBuilder spawn(SpawnCategory category, String type, int weight, int minCount, int maxCount) {
        this.spawners.computeIfAbsent(category, k -> new ArrayList<>())
                .add(new SpawnEntry(type, weight, minCount, maxCount));
        return this;
    }

    public BiomeBuilder spawn(SpawnCategory category, EntityType type, int weight, int minCount, int maxCount) {
        return spawn(category, type.getKey().asString(), weight, minCount, maxCount);
    }

    public BiomeBuilder tag(String tag) {
        this.tags.add(tag);
        return this;
    }

    public BiomeBuilder dimensionParameters(float temperature, float humidity, float continentalness, float erosion, float depth, float weirdness) {
        this.dimensionTemperature = temperature;
        this.dimensionHumidity = humidity;
        this.dimensionContinentalness = continentalness;
        this.dimensionErosion = erosion;
        this.dimensionDepth = depth;
        this.dimensionWeirdness = weirdness;
        return this;
    }

    public BiomeManager register() {
        manager.register(new BiomeDefinition(this));
        return manager;
    }

    public BiomeDefinition build() {
        return new BiomeDefinition(this);
    }

    String namespace() {
        return namespace;
    }

    String path() {
        return path;
    }

    float temperature() {
        return temperature;
    }

    float downfall() {
        return downfall;
    }

    boolean hasPrecipitation() {
        return hasPrecipitation;
    }

    String skyColor() {
        return skyColor;
    }

    String fogColor() {
        return fogColor;
    }

    String waterColor() {
        return waterColor;
    }

    String waterFogColor() {
        return waterFogColor;
    }

    String grassColor() {
        return grassColor;
    }

    String foliageColor() {
        return foliageColor;
    }

    List<String> carvers() {
        return carvers;
    }

    List<List<String>> features() {
        return features;
    }

    Map<SpawnCategory, List<SpawnEntry>> spawners() {
        return spawners;
    }

    Set<String> tags() {
        return tags;
    }

    float dimensionTemperature() {
        return dimensionTemperature;
    }

    float dimensionHumidity() {
        return dimensionHumidity;
    }

    float dimensionContinentalness() {
        return dimensionContinentalness;
    }

    float dimensionErosion() {
        return dimensionErosion;
    }

    float dimensionDepth() {
        return dimensionDepth;
    }

    float dimensionWeirdness() {
        return dimensionWeirdness;
    }

    private static String toHex(int rgb) {
        return String.format("#%02x%02x%02x", (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }
}
