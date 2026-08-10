package de.jakomi1.biome;

public enum WaterColor {

    WARM("#43d5ee"),
    NORMAL("#3f76e4"),
    SWAMP("#617b64");

    private final String hex;

    WaterColor(String hex) {
        this.hex = hex;
    }

    public String hex() {
        return hex;
    }

    @Override
    public String toString() {
        return hex;
    }
}
