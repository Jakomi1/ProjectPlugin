package de.jakomi1.biome;

import de.jakomi1.datapack.ManagedDatapack;
import de.jakomi1.project.AutoManager;
import de.jakomi1.project.ProjectServer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class BiomeManager implements AutoManager {

    private final ProjectServer server;
    private final Map<String, BiomeDefinition> biomes = new LinkedHashMap<>();

    private String namespace = "project";
    private String packName = "project_biomes";
    private String description = "Generated biomes";
    private String dimensionName;
    private String dimensionType = "minecraft:overworld";
    private boolean autoDeploy = true;
    private boolean auto = true;

    private boolean enabled;

    public BiomeManager(ProjectServer server) {
        this.server = server;
    }

    @Override
    public BiomeManager enable() {
        if (enabled) return this;
        enabled = true;

        if (autoDeploy && !biomes.isEmpty()) {
            deploy();
        }
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
    }

    @Override
    public boolean auto() {
        return auto;
    }

    @Override
    public BiomeManager auto(boolean auto) {
        this.auto = auto;
        return this;
    }

    public BiomeManager namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String namespace() {
        return namespace;
    }

    public BiomeManager packName(String packName) {
        this.packName = packName;
        return this;
    }

    public String packName() {
        return packName;
    }

    public BiomeManager description(String description) {
        this.description = description;
        return this;
    }

    public BiomeManager dimension(String name) {
        this.dimensionName = name;
        return this;
    }

    public BiomeManager dimension(String name, String dimensionType) {
        this.dimensionName = name;
        this.dimensionType = dimensionType;
        return this;
    }

    public BiomeManager autoDeploy(boolean autoDeploy) {
        this.autoDeploy = autoDeploy;
        return this;
    }

    public BiomeBuilder create(String path) {
        return create(namespace, path);
    }

    public BiomeBuilder create(String namespace, String path) {
        return new BiomeBuilder(this, namespace, path);
    }

    public BiomeBuilder create(String path, Consumer<BiomeBuilder> consumer) {
        return create(namespace, path, consumer);
    }

    public BiomeBuilder create(String namespace, String path, Consumer<BiomeBuilder> consumer) {
        BiomeBuilder builder = create(namespace, path);
        consumer.accept(builder);
        return builder;
    }

    public BiomeManager register(BiomeDefinition biome) {
        biomes.put(biome.id(), biome);
        return this;
    }

    public BiomeManager remove(String id) {
        biomes.remove(id);
        return this;
    }

    public BiomeDefinition get(String id) {
        return biomes.get(id);
    }

    public Collection<BiomeDefinition> all() {
        return new ArrayList<>(biomes.values());
    }

    public boolean isEmpty() {
        return biomes.isEmpty();
    }

    public int size() {
        return biomes.size();
    }

    public void deploy() {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world == null) {
            server.plugin().getLogger().warning("BiomeManager: Keine Welt geladen, Datapack konnte nicht angewendet werden.");
            return;
        }

        ManagedDatapack.Result result;
        try {
            BiomeDatapack datapack = new BiomeDatapack(
                    namespace,
                    packName,
                    description,
                    new ArrayList<>(biomes.values()),
                    dimensionName,
                    dimensionType
            );

            result = new ManagedDatapack(server.plugin(), packName)
                    .update(datapack::write, world.getWorldFolder().toPath());
        } catch (IOException e) {
            server.plugin().getLogger().warning("BiomeManager: Datapack konnte nicht aktualisiert werden: " + e.getMessage());
            return;
        }

        server.scheduler().runGlobal(() -> applyDatapack(result));
    }

    public void redeploy() {
        deploy();
    }

    public void clear() {
        biomes.clear();
    }

    private void applyDatapack(ManagedDatapack.Result result) {
        Plugin plugin = server.plugin();
        try {
            Bukkit.getServer().getDatapackManager().refreshPacks();
        } catch (Throwable t) {
            plugin.getLogger().warning("BiomeManager: DatapackManager nicht verfügbar: " + t.getMessage());
            return;
        }

        boolean needsReload = result.changed();
        try {
            var pack = Bukkit.getServer().getDatapackManager().getPack(packName);
            if (pack != null) {
                if (!pack.isEnabled()) {
                    needsReload = true;
                }
                pack.setEnabled(true);
            } else {
                plugin.getLogger().warning("BiomeManager: Datapack '" + packName + "' wurde nach refreshPacks() nicht gefunden.");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("BiomeManager: Datapack '" + packName + "' konnte nicht aktiviert werden: " + t.getMessage());
        }

        if (needsReload) {
            Bukkit.reloadData();
            plugin.getLogger().info("BiomeManager: Datapack '" + packName + "' mit " + biomes.size() + " Biomen aktualisiert.");
        } else {
            plugin.getLogger().info("BiomeManager: Datapack '" + packName + "' ist aktuell.");
        }
    }
}
