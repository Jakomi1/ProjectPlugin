package de.jakomi1.project.biome;

public enum SpawnCategory {

    AMBIENT("ambient"),
    AXOLOTLS("axolotls"),
    CREATURE("creature"),
    MISC("misc"),
    MONSTER("monster"),
    UNDERGROUND_WATER_CREATURE("underground_water_creature"),
    WATER_AMBIENT("water_ambient"),
    WATER_CREATURE("water_creature");

    private final String key;

    SpawnCategory(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
