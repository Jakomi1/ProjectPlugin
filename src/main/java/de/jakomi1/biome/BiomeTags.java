package de.jakomi1.biome;

public enum BiomeTags {

    IS_OVERWORLD("is_overworld"),
    IS_NETHER("is_nether"),
    IS_END("is_end"),
    IS_OCEAN("is_ocean"),
    IS_DEEP_OCEAN("is_deep_ocean"),
    IS_RIVER("is_river"),
    IS_BEACH("is_beach"),
    IS_FOREST("is_forest"),
    IS_JUNGLE("is_jungle"),
    IS_TAIGA("is_taiga"),
    IS_SAVANNA("is_savanna"),
    IS_MOUNTAIN("is_mountain"),
    IS_HILL("is_hill"),
    IS_BADLANDS("is_badlands"),
    IS_CAVE("is_cave"),
    SPAWNS_COLD_VARIANT_FARM_ANIMALS("spawns_cold_variant_farm_animals"),
    SPAWNS_WARM_VARIANT_FARM_ANIMALS("spawns_warm_variant_farm_animals"),
    SPAWNS_SNOW_FOXES("spawns_snow_foxes"),
    SPAWNS_WHITE_RABBITS("spawns_white_rabbits"),
    POLAR_BEARS_SPAWN_ON_ALTERNATE_BLOCKS("polar_bears_spawn_on_alternate_blocks"),
    SNOW_GOLEM_MELTS("snow_golem_melts"),
    PLAYS_UNDERWATER_MUSIC("plays_underwater_music"),
    INCREASED_FIRE_BURNOUT("increased_fire_burnout"),
    WITHOUT_PATROL_SPAWNS("without_patrol_spawns"),
    WITHOUT_WANDERING_TRADER_SPAWNS("without_wandering_trader_spawns"),
    WITHOUT_ZOMBIE_SIEGES("without_zombie_sieges"),
    WATER_ON_MAP_OUTLINES("water_on_map_outlines"),
    REDUCE_WATER_AMBIENT_SPAWNS("reduce_water_ambient_spawns"),
    REQUIRED_OCEAN_MONUMENT_SURROUNDING("required_ocean_monument_surrounding"),
    PRODUCES_CORALS_AROUND_BORDERS("produces_corals_around_borders"),
    STRONGHOLD_BIASED_TO("stronghold_biased_to");

    private final String key;

    BiomeTags(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
