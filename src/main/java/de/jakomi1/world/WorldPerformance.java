package de.jakomi1.world;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpawnCategory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class WorldPerformance implements Manager {

    private static final String[] WORLDS = {"world", "world_nether", "world_the_end"};
    private static final int[] PLAYER_COUNTS = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};

    private static final Map<String, Integer> DEFAULT_SPAWN_LIMITS = Map.of(
            "ambient", 15,
            "animal", 10,
            "water_animal", 5,
            "water_ambient", 20,
            "axolotl", 5,
            "monster", 50
    );

    private static final Map<String, Integer> DEFAULT_SPAWN_TICKS = Map.of(
            "animal", 400,
            "monster", 5,
            "water_animal", 5,
            "water_ambient", 5,
            "axolotl", 5,
            "ambient", 5
    );

    private static final Map<String, List<String>> ALLOWED_SPAWN_CATEGORIES = Map.of(
            "world", List.of("ambient", "animal", "water_animal", "water_ambient", "axolotl", "monster"),
            "world_nether", List.of("ambient", "animal", "monster"),
            "world_the_end", List.of("monster")
    );

    private final ProjectServer server;

    private boolean enabled;

    private File configFile;
    private FileConfiguration config;
    private final TreeMap<Integer, Map<String, int[]>> distanceMap = new TreeMap<>();
    private int lastAppliedPlayerKey = -1;

    private Component chatPrefix = Component.empty();
    private WorldPerformanceListener performanceListener;

    public WorldPerformance(ProjectServer server) {
        this.server = server;
    }

    @Override
    public WorldPerformance enable() {
        if (enabled) return this;
        enabled = true;

        this.chatPrefix = server.plugin().getPrefix() != null ? server.plugin().getPrefix() : Component.empty();

        load();
        updateDistances(true);
        this.performanceListener = new WorldPerformanceListener(this);
        performanceListener.register(server.plugin());
        return this;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;

        if (performanceListener != null) {
            performanceListener.unregister();
        }
    }

    public void load() {
        configFile = new File(server.plugin().getDataFolder(), "world.yml");
        copyDefaultIfMissing();
        loadConfig();
    }

    private void copyDefaultIfMissing() {
        if (configFile.exists()) return;

        try {
            server.plugin().getDataFolder().mkdirs();
            if (server.plugin().getResource("world.yml") != null) {
                server.plugin().saveResource("world.yml", false);
            } else {
                configFile.createNewFile();
            }
        } catch (IOException e) {
            server.plugin().getLogger().warning("world.yml konnte nicht erstellt werden: " + e.getMessage());
        }
    }

    public synchronized void updateDistances(boolean force) {
        int online = Bukkit.getOnlinePlayers().size();

        Map.Entry<Integer, Map<String, int[]>> selectedEntry = null;
        for (Map.Entry<Integer, Map<String, int[]>> entry : distanceMap.entrySet()) {
            if (online <= entry.getKey()) {
                selectedEntry = entry;
                break;
            }
        }

        if (selectedEntry == null && !distanceMap.isEmpty()) {
            selectedEntry = distanceMap.lastEntry();
        }

        if (selectedEntry == null) return;

        int currentPlayerKey = selectedEntry.getKey();
        if (!force && currentPlayerKey == lastAppliedPlayerKey) return;

        Map<String, int[]> selectedWorldMap = selectedEntry.getValue();

        List<Runnable> worldActions = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            String name = world.getName();
            int[] values = selectedWorldMap.getOrDefault(name, new int[]{5, 8});

            int sim = values[0];
            int view = values[1];

            if (sim == -1) {
                sim = resolveInheritedSimOrView(currentPlayerKey, name, true, 5);
            }
            if (view == -1) {
                view = resolveInheritedSimOrView(currentPlayerKey, name, false, 8);
            }

            sim = Math.max(2, Math.min(sim, 32));
            view = Math.max(2, Math.min(view, 32));

            final int appliedSim = sim;
            final int appliedView = view;
            worldActions.add(() -> {
                world.setSimulationDistance(appliedSim);
                world.setViewDistance(appliedView);
            });

            List<String> allowed = ALLOWED_SPAWN_CATEGORIES.getOrDefault(name, Collections.emptyList());

            for (String cat : allowed) {
                SpawnCategory sc = parseSpawnCategory(cat);
                if (sc == null) continue;

                String limitPath = currentPlayerKey + "." + name + ".spawnLimits." + cat;
                int limitDefault = (currentPlayerKey == 0 ? DEFAULT_SPAWN_LIMITS.get(cat) : -1);
                int limit = config.getInt(limitPath, limitDefault);

                if (limit == -1) {
                    limit = resolveInheritedInt(currentPlayerKey, name, "spawnLimits." + cat, DEFAULT_SPAWN_LIMITS.get(cat));
                }

                String tickPath = currentPlayerKey + "." + name + ".ticksPerSpawn." + cat;
                int tickDefault = (currentPlayerKey == 0 ? DEFAULT_SPAWN_TICKS.get(cat) : -1);
                int ticks = config.getInt(tickPath, tickDefault);

                if (ticks == -1) {
                    ticks = resolveInheritedInt(currentPlayerKey, name, "ticksPerSpawn." + cat, DEFAULT_SPAWN_TICKS.get(cat));
                }

                final int appliedLimit = Math.max(0, limit);
                final int appliedTicks = Math.max(1, ticks);
                worldActions.add(() -> {
                    world.setSpawnLimit(sc, appliedLimit);
                    world.setTicksPerSpawns(sc, appliedTicks);
                });
            }
        }

        Component info = buildConfigInfo(currentPlayerKey);

        server.scheduler().runGlobal(() -> {
            for (Runnable action : worldActions) {
                try {
                    action.run();
                } catch (Exception e) {
                    server.plugin().getLogger().warning("Fehler beim Anwenden der Distanz-/Spawn-Werte: " + e.getMessage());
                }
            }
        });

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (server.permissions().roleOf(player.getUniqueId()).isOwner()) {
                server.scheduler().runEntity(player, () -> player.sendMessage(info));
            }
        }

        lastAppliedPlayerKey = currentPlayerKey;
        server.plugin().getLogger().info("Render Distances, Spawn-Limits & Ticks aktualisiert (Key " + currentPlayerKey + ", Spieler: " + online + ").");
    }

    public void updateDistances() {
        updateDistances(false);
    }

    public void scheduleUpdate() {
        server.scheduler().runGlobal(this::updateDistances);
    }

    public void reload() {
        load();
        updateDistances(true);
    }

    public Component configInfo() {
        int online = Bukkit.getOnlinePlayers().size();

        Integer selectedKey = null;
        for (Integer key : distanceMap.keySet()) {
            if (online <= key) {
                selectedKey = key;
                break;
            }
        }
        if (selectedKey == null && !distanceMap.isEmpty()) {
            selectedKey = distanceMap.lastKey();
        }
        if (selectedKey == null) {
            return chatPrefix.append(
                    Component.text("world.yml wurde noch nicht geladen.", NamedTextColor.RED)
            );
        }

        return buildConfigInfo(selectedKey);
    }

    public Component configInfo(int playerKey) {
        if (!distanceMap.containsKey(playerKey)) {
            return chatPrefix.append(
                    Component.text("Stufe " + playerKey + " existiert nicht in world.yml.", NamedTextColor.RED)
            );
        }
        return buildConfigInfo(playerKey);
    }

    private Component buildConfigInfo(int currentPlayerKey) {
        Map<String, int[]> worldMap = distanceMap.get(currentPlayerKey);

        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("                  ", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH)
                .append(Component.text(" Spieleranzahl: " + currentPlayerKey + " ", NamedTextColor.AQUA)
                        .decoration(TextDecoration.STRIKETHROUGH, false))
                .append(Component.text("                  ", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH)));

        if (worldMap != null) {
            for (Map.Entry<String, int[]> entry : worldMap.entrySet()) {
                String worldName = switch (entry.getKey()) {
                    case "world" -> "Oberwelt";
                    case "world_nether" -> "Nether";
                    case "world_the_end" -> "Ende";
                    default -> entry.getKey();
                };
                int[] values = entry.getValue();
                int sim = values[0] == -1 ? resolveInheritedSimOrView(currentPlayerKey, entry.getKey(), true, 5) : values[0];
                int view = values[1] == -1 ? resolveInheritedSimOrView(currentPlayerKey, entry.getKey(), false, 8) : values[1];

                Component hoverText = Component.text("SpawnLimits & Ticks:", NamedTextColor.GRAY);
                List<String> allowed = ALLOWED_SPAWN_CATEGORIES.getOrDefault(entry.getKey(), Collections.emptyList());
                for (String cat : allowed) {
                    int limit = resolveInheritedInt(currentPlayerKey, entry.getKey(), "spawnLimits." + cat, DEFAULT_SPAWN_LIMITS.get(cat));
                    int tick = resolveInheritedInt(currentPlayerKey, entry.getKey(), "ticksPerSpawn." + cat, DEFAULT_SPAWN_TICKS.get(cat));
                    hoverText = hoverText.append(
                            Component.text("\n" + cat + ": ", NamedTextColor.GRAY)
                                    .append(Component.text("L=" + limit + ", T=" + tick, NamedTextColor.AQUA))
                    );
                }

                lines.add(chatPrefix.append(
                        Component.text(worldName, NamedTextColor.AQUA)
                                .append(Component.text(": [", NamedTextColor.GRAY))
                                .append(Component.text("SimD: ", NamedTextColor.GRAY))
                                .append(Component.text(sim, NamedTextColor.AQUA))
                                .append(Component.text(", ViewD: ", NamedTextColor.GRAY))
                                .append(Component.text(view, NamedTextColor.AQUA))
                                .append(Component.text(", Details ", NamedTextColor.GRAY)
                                        .hoverEvent(HoverEvent.showText(hoverText))
                                        .append(Component.text("]", NamedTextColor.GRAY)))
                ));
            }
        }

        lines.add(Component.text("                                                             ",
                NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH));

        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            result = result.append(i == 0 ? Component.empty() : Component.newline()).append(lines.get(i));
        }
        return result;
    }

    private void loadConfig() {
        if (configFile == null || !configFile.exists()) return;

        config = YamlConfiguration.loadConfiguration(configFile);
        boolean modified = false;

        for (int playerCount : PLAYER_COUNTS) {
            String base = String.valueOf(playerCount);

            for (String worldName : WORLDS) {
                String simPath = base + "." + worldName + ".simulationDistance";
                String viewPath = base + "." + worldName + ".viewDistance";

                if (playerCount == 0) {
                    if (!config.contains(simPath)) {
                        config.set(simPath, 5);
                        modified = true;
                    }
                    if (!config.contains(viewPath)) {
                        config.set(viewPath, 8);
                        modified = true;
                    }
                } else {
                    if (!config.contains(simPath)) {
                        config.set(simPath, -1);
                        modified = true;
                    }
                    if (!config.contains(viewPath)) {
                        config.set(viewPath, -1);
                        modified = true;
                    }
                }

                List<String> allowed = ALLOWED_SPAWN_CATEGORIES.getOrDefault(worldName, Collections.emptyList());

                String spawnLimitsSection = base + "." + worldName + ".spawnLimits";
                if (!config.contains(spawnLimitsSection)) {
                    config.createSection(spawnLimitsSection);
                    modified = true;
                }
                if (config.getConfigurationSection(spawnLimitsSection) != null) {
                    Set<String> keysToRemove = new HashSet<>();
                    for (String key : config.getConfigurationSection(spawnLimitsSection).getKeys(false)) {
                        if (!allowed.contains(key)) keysToRemove.add(key);
                    }
                    for (String removeKey : keysToRemove) {
                        config.set(spawnLimitsSection + "." + removeKey, null);
                        modified = true;
                    }
                }

                for (String cat : allowed) {
                    String path = spawnLimitsSection + "." + cat;
                    if (playerCount == 0) {
                        if (!config.contains(path)) {
                            config.set(path, DEFAULT_SPAWN_LIMITS.get(cat));
                            modified = true;
                        }
                    } else {
                        if (!config.contains(path)) {
                            config.set(path, -1);
                            modified = true;
                        }
                    }
                }

                String ticksSection = base + "." + worldName + ".ticksPerSpawn";
                if (!config.contains(ticksSection)) {
                    config.createSection(ticksSection);
                    modified = true;
                }
                if (config.getConfigurationSection(ticksSection) != null) {
                    Set<String> keysToRemoveTicks = new HashSet<>();
                    for (String key : config.getConfigurationSection(ticksSection).getKeys(false)) {
                        if (!allowed.contains(key)) keysToRemoveTicks.add(key);
                    }
                    for (String removeKey : keysToRemoveTicks) {
                        config.set(ticksSection + "." + removeKey, null);
                        modified = true;
                    }
                }

                for (String cat : allowed) {
                    String path = ticksSection + "." + cat;
                    if (playerCount == 0) {
                        if (!config.contains(path)) {
                            config.set(path, DEFAULT_SPAWN_TICKS.get(cat));
                            modified = true;
                        }
                    } else {
                        if (!config.contains(path)) {
                            config.set(path, -1);
                            modified = true;
                        }
                    }
                }
            }
        }

        for (String key : new HashSet<>(config.getKeys(false))) {
            if (key.equalsIgnoreCase("lastAppliedPlayerKey")) continue;
            try {
                Integer.parseInt(key);
            } catch (NumberFormatException e) {
                config.set(key, null);
                modified = true;
                server.plugin().getLogger().warning("Ungültiger Key '" + key + "' entfernt!");
            }
        }

        if (modified) {
            saveSortedConfig();
        } else {
            sortAndResaveIfUnsorted();
        }

        reloadIntoMap();
    }

    private void saveSortedConfig() {
        try {
            TreeMap<Integer, Object> sorted = new TreeMap<>();
            for (String key : config.getKeys(false)) {
                if (key.equalsIgnoreCase("lastAppliedPlayerKey")) continue;
                try {
                    ConfigurationSection section = config.getConfigurationSection(key);
                    if (section != null) {
                        sorted.put(Integer.parseInt(key), section.getValues(true));
                    }
                } catch (Exception ignored) {
                }
            }

            YamlConfiguration newConfig = new YamlConfiguration();
            for (Map.Entry<Integer, Object> entry : sorted.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Map<?, ?> values = (Map<?, ?>) entry.getValue();
                for (Map.Entry<?, ?> sub : values.entrySet()) {
                    newConfig.set(key + "." + sub.getKey(), sub.getValue());
                }
            }

            newConfig.save(configFile);
            server.plugin().getLogger().info("world.yml sortiert und gespeichert.");
        } catch (Exception e) {
            server.plugin().getLogger().warning("world.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    private void sortAndResaveIfUnsorted() {
        List<Integer> keys = new ArrayList<>();
        for (String key : config.getKeys(false)) {
            if (key.equalsIgnoreCase("lastAppliedPlayerKey")) continue;
            try {
                keys.add(Integer.parseInt(key));
            } catch (NumberFormatException ignored) {
            }
        }
        List<Integer> sorted = new ArrayList<>(keys);
        Collections.sort(sorted);
        if (!keys.equals(sorted)) {
            server.plugin().getLogger().info("world.yml war unsortiert – wird neu gespeichert.");
            saveSortedConfig();
        }
    }

    private void reloadIntoMap() {
        config = YamlConfiguration.loadConfiguration(configFile);
        distanceMap.clear();

        for (String key : config.getKeys(false)) {
            if (key.equalsIgnoreCase("lastAppliedPlayerKey")) continue;
            try {
                int playerCount = Integer.parseInt(key);
                Map<String, int[]> worldMap = new HashMap<>();
                for (String worldName : WORLDS) {
                    int simDefault = (playerCount == 0 ? 5 : -1);
                    int viewDefault = (playerCount == 0 ? 8 : -1);

                    int sim = config.getInt(key + "." + worldName + ".simulationDistance", simDefault);
                    int view = config.getInt(key + "." + worldName + ".viewDistance", viewDefault);

                    if (sim != -1) sim = Math.max(2, Math.min(sim, 32));
                    if (view != -1) view = Math.max(2, Math.min(view, 32));

                    worldMap.put(worldName, new int[]{sim, view});
                }
                distanceMap.put(playerCount, worldMap);
            } catch (NumberFormatException ignored) {
            }
        }

        server.plugin().getLogger().info("world.yml geladen und sortiert: " + distanceMap.keySet());
    }

    private int resolveInheritedInt(int currentKey, String worldName, String suffixPath, int defaultValue) {
        Map.Entry<Integer, Map<String, int[]>> previousEntry = distanceMap.lowerEntry(currentKey);
        while (previousEntry != null) {
            int prevKey = previousEntry.getKey();
            int val = config.getInt(prevKey + "." + worldName + "." + suffixPath, Integer.MIN_VALUE);
            if (val != Integer.MIN_VALUE && val != -1) return val;
            previousEntry = distanceMap.lowerEntry(prevKey);
        }
        return defaultValue;
    }

    private int resolveInheritedSimOrView(int currentKey, String worldName, boolean isSim, int defaultValue) {
        Map.Entry<Integer, Map<String, int[]>> previousEntry = distanceMap.lowerEntry(currentKey);
        while (previousEntry != null) {
            Map<String, int[]> prevWorldMap = previousEntry.getValue();
            if (prevWorldMap != null) {
                int[] prevVals = prevWorldMap.get(worldName);
                if (prevVals != null) {
                    int candidate = isSim ? prevVals[0] : prevVals[1];
                    if (candidate != -1) return candidate;
                }
            }
            previousEntry = distanceMap.lowerEntry(previousEntry.getKey());
        }
        return defaultValue;
    }

    private static SpawnCategory parseSpawnCategory(String string) {
        return switch (string) {
            case "ambient" -> SpawnCategory.AMBIENT;
            case "animal" -> SpawnCategory.ANIMAL;
            case "water_animal" -> SpawnCategory.WATER_ANIMAL;
            case "water_ambient" -> SpawnCategory.WATER_AMBIENT;
            case "axolotl" -> SpawnCategory.AXOLOTL;
            case "monster" -> SpawnCategory.MONSTER;
            default -> null;
        };
    }
}
