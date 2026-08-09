package de.jakomi1.dimension;

import de.jakomi1.biome.BiomeDefinition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DimensionDefinition {

    private final String id;
    private final String namespace;
    private final String path;

    private final String dimensionType;
    private final boolean ultrawarm;
    private final boolean natural;
    private final boolean piglinSafe;
    private final boolean respawnAnchorWorks;
    private final boolean bedWorks;
    private final boolean hasRaids;
    private final boolean hasSkylight;
    private final boolean hasCeiling;
    private final double coordinateScale;
    private final double ambientLight;
    private final int logicalHeight;
    private final int minY;
    private final int height;
    private final int monsterSpawnLightMin;
    private final int monsterSpawnLightMax;
    private final boolean monsterSpawnLightConstantMode;
    private final int monsterSpawnBlockLightLimit;
    private final String effects;
    private final String infiniburn;
    private final Long fixedTime;

    private final String generatorType;
    private final String noiseSettings;
    private final boolean amplified;
    private final Long seed;
    private final List<DimensionBuilder.FlatLayer> flatLayers;
    private final String flatBiome;
    private final boolean flatLakes;
    private final boolean flatFeatures;
    private final List<String> flatStructureOverrides;

    private final String biomeSourceMode;
    private final String biomeSourcePreset;
    private final String fixedBiome;
    private final List<BiomeDefinition> biomes;
    private final List<String> checkerboardBiomes;
    private final int checkerboardScale;

    private final Set<String> tags;

    DimensionDefinition(DimensionBuilder builder) {
        this.namespace = builder.namespace();
        this.path = builder.path();
        this.id = namespace + ":" + path;

        this.dimensionType = builder.dimensionType();
        this.ultrawarm = builder.ultrawarm();
        this.natural = builder.natural();
        this.piglinSafe = builder.piglinSafe();
        this.respawnAnchorWorks = builder.respawnAnchorWorks();
        this.bedWorks = builder.bedWorks();
        this.hasRaids = builder.hasRaids();
        this.hasSkylight = builder.hasSkylight();
        this.hasCeiling = builder.hasCeiling();
        this.coordinateScale = builder.coordinateScale();
        this.ambientLight = builder.ambientLight();
        this.logicalHeight = builder.logicalHeight();
        this.minY = builder.minY();
        this.height = builder.height();
        this.monsterSpawnLightMin = builder.monsterSpawnLightMin();
        this.monsterSpawnLightMax = builder.monsterSpawnLightMax();
        this.monsterSpawnLightConstantMode = builder.monsterSpawnLightConstantMode();
        this.monsterSpawnBlockLightLimit = builder.monsterSpawnBlockLightLimit();
        this.effects = builder.effects();
        this.infiniburn = builder.infiniburn();
        this.fixedTime = builder.fixedTime();

        this.generatorType = builder.generatorType();
        this.noiseSettings = builder.noiseSettings();
        this.amplified = builder.amplified();
        this.seed = builder.seed();
        this.flatLayers = new ArrayList<>(builder.flatLayers());
        this.flatBiome = builder.flatBiome();
        this.flatLakes = builder.flatLakes();
        this.flatFeatures = builder.flatFeatures();
        this.flatStructureOverrides = new ArrayList<>(builder.flatStructureOverrides());

        this.biomeSourceMode = builder.biomeSourceMode();
        this.biomeSourcePreset = builder.biomeSourcePreset();
        this.fixedBiome = builder.fixedBiome();
        this.biomes = new ArrayList<>(builder.biomes());
        this.checkerboardBiomes = new ArrayList<>(builder.checkerboardBiomes());
        this.checkerboardScale = builder.checkerboardScale();

        this.tags = new LinkedHashSet<>(builder.tags());
    }

    public String id() {
        return id;
    }

    public String namespace() {
        return namespace;
    }

    public String path() {
        return path;
    }

    public String dimensionType() {
        return dimensionType;
    }

    public boolean ultrawarm() {
        return ultrawarm;
    }

    public boolean natural() {
        return natural;
    }

    public boolean piglinSafe() {
        return piglinSafe;
    }

    public boolean respawnAnchorWorks() {
        return respawnAnchorWorks;
    }

    public boolean bedWorks() {
        return bedWorks;
    }

    public boolean hasRaids() {
        return hasRaids;
    }

    public boolean hasSkylight() {
        return hasSkylight;
    }

    public boolean hasCeiling() {
        return hasCeiling;
    }

    public double coordinateScale() {
        return coordinateScale;
    }

    public double ambientLight() {
        return ambientLight;
    }

    public int logicalHeight() {
        return logicalHeight;
    }

    public int minY() {
        return minY;
    }

    public int height() {
        return height;
    }

    public int monsterSpawnLightMin() {
        return monsterSpawnLightMin;
    }

    public int monsterSpawnLightMax() {
        return monsterSpawnLightMax;
    }

    public boolean monsterSpawnLightConstantMode() {
        return monsterSpawnLightConstantMode;
    }

    public int monsterSpawnBlockLightLimit() {
        return monsterSpawnBlockLightLimit;
    }

    public String effects() {
        return effects;
    }

    public String infiniburn() {
        return infiniburn;
    }

    public Long fixedTime() {
        return fixedTime;
    }

    public String generatorType() {
        return generatorType;
    }

    public String noiseSettings() {
        return noiseSettings;
    }

    public boolean amplified() {
        return amplified;
    }

    public Long seed() {
        return seed;
    }

    public List<DimensionBuilder.FlatLayer> flatLayers() {
        return flatLayers;
    }

    public String flatBiome() {
        return flatBiome;
    }

    public boolean flatLakes() {
        return flatLakes;
    }

    public boolean flatFeatures() {
        return flatFeatures;
    }

    public List<String> flatStructureOverrides() {
        return flatStructureOverrides;
    }

    public String biomeSourceMode() {
        return biomeSourceMode;
    }

    public String biomeSourcePreset() {
        return biomeSourcePreset;
    }

    public String fixedBiome() {
        return fixedBiome;
    }

    public List<BiomeDefinition> biomes() {
        return biomes;
    }

    public List<String> checkerboardBiomes() {
        return checkerboardBiomes;
    }

    public int checkerboardScale() {
        return checkerboardScale;
    }

    public Set<String> tags() {
        return tags;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DimensionDefinition that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
