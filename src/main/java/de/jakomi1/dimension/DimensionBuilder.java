package de.jakomi1.dimension;

import de.jakomi1.biome.BiomeDefinition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DimensionBuilder {

    public static final String GENERATOR_NOISE = "minecraft:noise";
    public static final String GENERATOR_FLAT = "minecraft:flat";
    public static final String GENERATOR_DEBUG = "minecraft:debug";

    public static final String SOURCE_PRESET = "preset";
    public static final String SOURCE_MULTI_NOISE = "multi_noise";
    public static final String SOURCE_FIXED = "fixed";
    public static final String SOURCE_CHECKERBOARD = "checkerboard";
    public static final String SOURCE_THE_END = "the_end";

    private final DimensionManager manager;
    private final String namespace;
    private final String path;

    private String dimensionType;
    private boolean ultrawarm;
    private boolean natural = true;
    private boolean piglinSafe;
    private boolean respawnAnchorWorks;
    private boolean bedWorks = true;
    private boolean hasRaids = true;
    private boolean hasSkylight = true;
    private boolean hasCeiling;
    private double coordinateScale = 1.0;
    private double ambientLight = 0.0;
    private int logicalHeight = 384;
    private int minY = -64;
    private int height = 384;
    private int monsterSpawnLightMin = 0;
    private int monsterSpawnLightMax = 7;
    private int monsterSpawnLightMode = LIGHT_UNIFORM;
    private int monsterSpawnBlockLightLimit;
    private String effects = "minecraft:overworld";
    private String infiniburn = "#minecraft:infiniburn_overworld";
    private Long fixedTime;

    private String generatorType = GENERATOR_NOISE;
    private String noiseSettings = "minecraft:overworld";
    private boolean amplified;
    private Long seed;
    private final List<FlatLayer> flatLayers = new ArrayList<>();
    private String flatBiome;
    private boolean flatLakes;
    private boolean flatFeatures;
    private final List<String> flatStructureOverrides = new ArrayList<>();

    private String biomeSourceMode = SOURCE_PRESET;
    private String biomeSourcePreset = "minecraft:overworld";
    private String fixedBiome;
    private final List<BiomeDefinition> biomes = new ArrayList<>();
    private final List<String> checkerboardBiomes = new ArrayList<>();
    private int checkerboardScale = 2;

    private final Set<String> tags = new LinkedHashSet<>();

    private static final int LIGHT_CONSTANT = 1;
    private static final int LIGHT_UNIFORM = 2;

    DimensionBuilder(DimensionManager manager, String namespace, String path) {
        this.manager = manager;
        this.namespace = namespace;
        this.path = path;
    }

    public DimensionBuilder overworld() {
        ultrawarm(false);
        natural(true);
        piglinSafe(false);
        respawnAnchorWorks(false);
        bedWorks(true);
        hasRaids(true);
        hasSkylight(true);
        hasCeiling(false);
        coordinateScale(1.0);
        ambientLight(0.0);
        logicalHeight(384);
        minY(-64);
        height(384);
        effects("minecraft:overworld");
        infiniburn("#minecraft:infiniburn_overworld");
        monsterSpawnLightUniform(0, 7);
        monsterSpawnBlockLightLimit(0);
        generatorNoise("minecraft:overworld");
        preset("minecraft:overworld");
        return this;
    }

    public DimensionBuilder nether() {
        ultrawarm(true);
        natural(false);
        piglinSafe(true);
        respawnAnchorWorks(true);
        bedWorks(false);
        hasRaids(false);
        hasSkylight(false);
        hasCeiling(true);
        coordinateScale(8.0);
        ambientLight(0.1);
        logicalHeight(128);
        minY(0);
        height(256);
        effects("minecraft:the_nether");
        infiniburn("#minecraft:infiniburn_nether");
        monsterSpawnLightConstant(7);
        monsterSpawnBlockLightLimit(15);
        generatorNoise("minecraft:the_nether");
        preset("minecraft:the_nether");
        return this;
    }

    public DimensionBuilder theEnd() {
        ultrawarm(false);
        natural(false);
        piglinSafe(false);
        respawnAnchorWorks(false);
        bedWorks(false);
        hasRaids(true);
        hasSkylight(false);
        hasCeiling(false);
        coordinateScale(1.0);
        ambientLight(0.0);
        logicalHeight(256);
        minY(0);
        height(256);
        effects("minecraft:the_end");
        infiniburn("#minecraft:infiniburn_end");
        monsterSpawnLightConstant(0);
        monsterSpawnBlockLightLimit(0);
        generatorNoise("minecraft:the_end");
        theEndSource();
        return this;
    }

    public DimensionBuilder dimensionType(String dimensionType) {
        this.dimensionType = dimensionType;
        return this;
    }

    public DimensionBuilder ultrawarm(boolean ultrawarm) {
        this.ultrawarm = ultrawarm;
        return this;
    }

    public DimensionBuilder natural(boolean natural) {
        this.natural = natural;
        return this;
    }

    public DimensionBuilder piglinSafe(boolean piglinSafe) {
        this.piglinSafe = piglinSafe;
        return this;
    }

    public DimensionBuilder respawnAnchorWorks(boolean respawnAnchorWorks) {
        this.respawnAnchorWorks = respawnAnchorWorks;
        return this;
    }

    public DimensionBuilder bedWorks(boolean bedWorks) {
        this.bedWorks = bedWorks;
        return this;
    }

    public DimensionBuilder hasRaids(boolean hasRaids) {
        this.hasRaids = hasRaids;
        return this;
    }

    public DimensionBuilder hasSkylight(boolean hasSkylight) {
        this.hasSkylight = hasSkylight;
        return this;
    }

    public DimensionBuilder hasCeiling(boolean hasCeiling) {
        this.hasCeiling = hasCeiling;
        return this;
    }

    public DimensionBuilder coordinateScale(double coordinateScale) {
        this.coordinateScale = coordinateScale;
        return this;
    }

    public DimensionBuilder ambientLight(double ambientLight) {
        this.ambientLight = ambientLight;
        return this;
    }

    public DimensionBuilder logicalHeight(int logicalHeight) {
        this.logicalHeight = logicalHeight;
        return this;
    }

    public DimensionBuilder minY(int minY) {
        this.minY = minY;
        return this;
    }

    public DimensionBuilder height(int height) {
        this.height = height;
        return this;
    }

    public DimensionBuilder monsterSpawnLightConstant(int level) {
        this.monsterSpawnLightMode = LIGHT_CONSTANT;
        this.monsterSpawnLightMin = level;
        this.monsterSpawnLightMax = level;
        return this;
    }

    public DimensionBuilder monsterSpawnLightUniform(int min, int max) {
        this.monsterSpawnLightMode = LIGHT_UNIFORM;
        this.monsterSpawnLightMin = min;
        this.monsterSpawnLightMax = max;
        return this;
    }

    public DimensionBuilder monsterSpawnLight(int min, int max) {
        return monsterSpawnLightUniform(min, max);
    }

    public DimensionBuilder monsterSpawnBlockLightLimit(int limit) {
        this.monsterSpawnBlockLightLimit = limit;
        return this;
    }

    public DimensionBuilder effects(String effects) {
        this.effects = effects;
        return this;
    }

    public DimensionBuilder infiniburn(String infiniburn) {
        this.infiniburn = infiniburn;
        return this;
    }

    public DimensionBuilder fixedTime(long fixedTime) {
        this.fixedTime = fixedTime;
        return this;
    }

    public DimensionBuilder generatorNoise(String noiseSettings) {
        this.generatorType = GENERATOR_NOISE;
        this.noiseSettings = noiseSettings;
        return this;
    }

    public DimensionBuilder noiseSettings(String noiseSettings) {
        return generatorNoise(noiseSettings);
    }

    public DimensionBuilder amplified(boolean amplified) {
        this.amplified = amplified;
        return this;
    }

    public DimensionBuilder seed(long seed) {
        this.seed = seed;
        return this;
    }

    public DimensionBuilder flat() {
        this.generatorType = GENERATOR_FLAT;
        return this;
    }

    public DimensionBuilder flatLayer(String block, int height) {
        this.flatLayers.add(new FlatLayer(block, height));
        return this;
    }

    public DimensionBuilder flatBiome(String flatBiome) {
        this.flatBiome = flatBiome;
        return this;
    }

    public DimensionBuilder flatLakes(boolean flatLakes) {
        this.flatLakes = flatLakes;
        return this;
    }

    public DimensionBuilder flatFeatures(boolean flatFeatures) {
        this.flatFeatures = flatFeatures;
        return this;
    }

    public DimensionBuilder flatStructureOverride(String structure) {
        this.flatStructureOverrides.add(structure);
        return this;
    }

    public DimensionBuilder debug() {
        this.generatorType = GENERATOR_DEBUG;
        return this;
    }

    public DimensionBuilder preset(String biomeSourcePreset) {
        this.biomeSourceMode = SOURCE_PRESET;
        this.biomeSourcePreset = biomeSourcePreset;
        return this;
    }

    public DimensionBuilder multiNoise() {
        this.biomeSourceMode = SOURCE_MULTI_NOISE;
        return this;
    }

    public DimensionBuilder biome(BiomeDefinition biome) {
        this.biomeSourceMode = SOURCE_MULTI_NOISE;
        this.biomes.add(biome);
        return this;
    }

    public DimensionBuilder fixedBiome(String fixedBiome) {
        this.biomeSourceMode = SOURCE_FIXED;
        this.fixedBiome = fixedBiome;
        return this;
    }

    public DimensionBuilder fixedBiome(BiomeDefinition biome) {
        return fixedBiome(biome.id());
    }

    public DimensionBuilder checkerboard(int scale) {
        this.biomeSourceMode = SOURCE_CHECKERBOARD;
        this.checkerboardScale = scale;
        return this;
    }

    public DimensionBuilder checkerboardBiome(String biome) {
        this.biomeSourceMode = SOURCE_CHECKERBOARD;
        this.checkerboardBiomes.add(biome);
        return this;
    }

    public DimensionBuilder theEndSource() {
        this.biomeSourceMode = SOURCE_THE_END;
        return this;
    }

    public DimensionBuilder tag(String tag) {
        this.tags.add(tag);
        return this;
    }

    public DimensionBuilder tag(String namespace, String tag) {
        return tag(namespace + ":" + tag);
    }

    public DimensionManager register() {
        manager.register(new DimensionDefinition(this));
        return manager;
    }

    public DimensionDefinition build() {
        return new DimensionDefinition(this);
    }

    String namespace() {
        return namespace;
    }

    String path() {
        return path;
    }

    String dimensionType() {
        return dimensionType;
    }

    boolean ultrawarm() {
        return ultrawarm;
    }

    boolean natural() {
        return natural;
    }

    boolean piglinSafe() {
        return piglinSafe;
    }

    boolean respawnAnchorWorks() {
        return respawnAnchorWorks;
    }

    boolean bedWorks() {
        return bedWorks;
    }

    boolean hasRaids() {
        return hasRaids;
    }

    boolean hasSkylight() {
        return hasSkylight;
    }

    boolean hasCeiling() {
        return hasCeiling;
    }

    double coordinateScale() {
        return coordinateScale;
    }

    double ambientLight() {
        return ambientLight;
    }

    int logicalHeight() {
        return logicalHeight;
    }

    int minY() {
        return minY;
    }

    int height() {
        return height;
    }

    int monsterSpawnLightMin() {
        return monsterSpawnLightMin;
    }

    int monsterSpawnLightMax() {
        return monsterSpawnLightMax;
    }

    boolean monsterSpawnLightConstantMode() {
        return monsterSpawnLightMode == LIGHT_CONSTANT;
    }

    int monsterSpawnBlockLightLimit() {
        return monsterSpawnBlockLightLimit;
    }

    String effects() {
        return effects;
    }

    String infiniburn() {
        return infiniburn;
    }

    Long fixedTime() {
        return fixedTime;
    }

    String generatorType() {
        return generatorType;
    }

    String noiseSettings() {
        return noiseSettings;
    }

    boolean amplified() {
        return amplified;
    }

    Long seed() {
        return seed;
    }

    List<FlatLayer> flatLayers() {
        return flatLayers;
    }

    String flatBiome() {
        return flatBiome;
    }

    boolean flatLakes() {
        return flatLakes;
    }

    boolean flatFeatures() {
        return flatFeatures;
    }

    List<String> flatStructureOverrides() {
        return flatStructureOverrides;
    }

    String biomeSourceMode() {
        return biomeSourceMode;
    }

    String biomeSourcePreset() {
        return biomeSourcePreset;
    }

    String fixedBiome() {
        return fixedBiome;
    }

    List<BiomeDefinition> biomes() {
        return biomes;
    }

    List<String> checkerboardBiomes() {
        return checkerboardBiomes;
    }

    int checkerboardScale() {
        return checkerboardScale;
    }

    Set<String> tags() {
        return tags;
    }

    static final class FlatLayer {
        private final String block;
        private final int height;

        FlatLayer(String block, int height) {
            this.block = block;
            this.height = height;
        }

        String block() {
            return block;
        }

        int height() {
            return height;
        }
    }
}
