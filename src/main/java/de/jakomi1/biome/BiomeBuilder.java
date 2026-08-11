package de.jakomi1.biome;

import org.bukkit.Registry;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
    private String ambientSoundLoop;
    private String ambientSoundAdditions;
    private double ambientSoundAdditionsChance;
    private String ambientSoundMood;
    private int ambientSoundMoodTickDelay = 6000;
    private int ambientSoundMoodBlockSearchExtent = 8;
    private double ambientSoundMoodOffset = 2.0;
    private String backgroundMusic;
    private int backgroundMusicMinDelay = 12000;
    private int backgroundMusicMaxDelay = 24000;
    private final List<ParticleEntry> ambientParticles = new ArrayList<>();
    private boolean increasedFireBurnout;
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

    public BiomeBuilder waterColor(WaterColor color) {
        this.waterColor = color.hex();
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
        return spawn(category, type.key().asString(), weight, minCount, maxCount);
    }

    public BiomeBuilder tag(String tag) {
        this.tags.add(tag);
        return this;
    }

    public BiomeBuilder tag(io.papermc.paper.registry.tag.TagKey<org.bukkit.block.Biome> tag) {
        return tag(tag.key().asString());
    }

    public BiomeBuilder ambientSoundLoop(String sound) {
        this.ambientSoundLoop = sound;
        return this;
    }

    public BiomeBuilder ambientSoundLoop(Sound sound) {
        return ambientSoundLoop(soundKey(sound));
    }

    public BiomeBuilder ambientSoundAdditions(String sound, double tickChance) {
        this.ambientSoundAdditions = sound;
        this.ambientSoundAdditionsChance = tickChance;
        return this;
    }

    public BiomeBuilder ambientSoundAdditions(Sound sound, double tickChance) {
        return ambientSoundAdditions(soundKey(sound), tickChance);
    }

    public BiomeBuilder ambientSoundMood(String sound, int tickDelay, int blockSearchExtent, double offset) {
        this.ambientSoundMood = sound;
        this.ambientSoundMoodTickDelay = tickDelay;
        this.ambientSoundMoodBlockSearchExtent = blockSearchExtent;
        this.ambientSoundMoodOffset = offset;
        return this;
    }

    public BiomeBuilder ambientSoundMood(Sound sound, int tickDelay, int blockSearchExtent, double offset) {
        return ambientSoundMood(soundKey(sound), tickDelay, blockSearchExtent, offset);
    }

    public BiomeBuilder backgroundMusic(String sound, int minDelay, int maxDelay) {
        this.backgroundMusic = sound;
        this.backgroundMusicMinDelay = minDelay;
        this.backgroundMusicMaxDelay = maxDelay;
        return this;
    }

    public BiomeBuilder backgroundMusic(Sound sound, int minDelay, int maxDelay) {
        return backgroundMusic(soundKey(sound), minDelay, maxDelay);
    }

    private static String soundKey(Sound sound) {
        return Registry.SOUNDS.getKey(sound).asString();
    }

    public BiomeBuilder ambientParticle(String type, double probability) {
        this.ambientParticles.add(new ParticleEntry(type, probability));
        return this;
    }

    public BiomeBuilder ambientParticle(Particle particle, double probability) {
        return ambientParticle(particle.key().asString(), probability);
    }

    public BiomeBuilder increasedFireBurnout(boolean increasedFireBurnout) {
        this.increasedFireBurnout = increasedFireBurnout;
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

    String ambientSoundLoop() {
        return ambientSoundLoop;
    }

    String ambientSoundAdditions() {
        return ambientSoundAdditions;
    }

    double ambientSoundAdditionsChance() {
        return ambientSoundAdditionsChance;
    }

    String ambientSoundMood() {
        return ambientSoundMood;
    }

    int ambientSoundMoodTickDelay() {
        return ambientSoundMoodTickDelay;
    }

    int ambientSoundMoodBlockSearchExtent() {
        return ambientSoundMoodBlockSearchExtent;
    }

    double ambientSoundMoodOffset() {
        return ambientSoundMoodOffset;
    }

    String backgroundMusic() {
        return backgroundMusic;
    }

    int backgroundMusicMinDelay() {
        return backgroundMusicMinDelay;
    }

    int backgroundMusicMaxDelay() {
        return backgroundMusicMaxDelay;
    }

    List<ParticleEntry> ambientParticles() {
        return ambientParticles;
    }

    boolean increasedFireBurnout() {
        return increasedFireBurnout;
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

    static final class ParticleEntry {
        private final String type;
        private final double probability;

        ParticleEntry(String type, double probability) {
            this.type = type;
            this.probability = probability;
        }

        String type() {
            return type;
        }

        double probability() {
            return probability;
        }
    }
}
