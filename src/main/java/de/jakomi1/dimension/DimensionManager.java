package de.jakomi1.dimension;

import de.jakomi1.datapack.ManagedDatapack;
import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class DimensionManager implements Manager {

    private static final long DEPLOY_DELAY_TICKS = 200L;

    private static final String VANILLA_OVERWORLD = "minecraft:overworld";
    private static final String VANILLA_THE_NETHER = "minecraft:the_nether";
    private static final String VANILLA_THE_END = "minecraft:the_end";

    private final ProjectServer server;
    private final Map<String, DimensionDefinition> dimensions = new LinkedHashMap<>();

    private String namespace = "project";
    private String packName = "project_dimensions";
    private String description = "Generated dimensions";
    private boolean autoDeploy = true;

    private boolean registerCommand = true;
    private String permission;
    private final DimensionCommand command;

    private boolean enabled;

    public DimensionManager(ProjectServer server) {
        this.server = server;
        this.command = new DimensionCommand(this);
    }

    @Override
    public DimensionManager enable() {
        if (enabled) return this;
        enabled = true;

        if (registerCommand) {
            command.register(server.plugin());
        }

        if (autoDeploy && !dimensions.isEmpty()) {
            server.scheduler().runLater(this::deploy, DEPLOY_DELAY_TICKS);
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

    public DimensionManager namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String namespace() {
        return namespace;
    }

    public DimensionManager packName(String packName) {
        this.packName = packName;
        return this;
    }

    public String packName() {
        return packName;
    }

    public DimensionManager description(String description) {
        this.description = description;
        return this;
    }

    public DimensionManager autoDeploy(boolean autoDeploy) {
        this.autoDeploy = autoDeploy;
        return this;
    }

    public DimensionManager command(boolean registerCommand) {
        this.registerCommand = registerCommand;
        return this;
    }

    public boolean command() {
        return registerCommand;
    }

    public DimensionManager permission(String permission) {
        this.permission = permission == null || permission.isBlank() ? null : permission;
        return this;
    }

    public String permission() {
        return permission;
    }

    public ProjectServer server() {
        return server;
    }

    public DimensionBuilder create(String path) {
        return create(namespace, path);
    }

    public DimensionBuilder create(String namespace, String path) {
        return new DimensionBuilder(this, namespace, path);
    }

    public DimensionBuilder create(String path, Consumer<DimensionBuilder> consumer) {
        return create(namespace, path, consumer);
    }

    public DimensionBuilder create(String namespace, String path, Consumer<DimensionBuilder> consumer) {
        DimensionBuilder builder = create(namespace, path);
        consumer.accept(builder);
        return builder;
    }

    public DimensionManager register(DimensionDefinition dimension) {
        validate(dimension);
        dimensions.put(dimension.id(), dimension);
        return this;
    }

    public DimensionManager remove(String id) {
        dimensions.remove(id);
        return this;
    }

    public DimensionDefinition get(String id) {
        return dimensions.get(id);
    }

    public DimensionDefinition require(String id) {
        DimensionDefinition dimension = dimensions.get(id);
        if (dimension == null) {
            throw new IllegalStateException("Dimension '" + id + "' ist nicht registriert.");
        }
        return dimension;
    }

    public boolean contains(String id) {
        return dimensions.containsKey(id);
    }

    public boolean has(String id) {
        return dimensions.containsKey(id);
    }

    public Set<String> keys() {
        return dimensions.keySet();
    }

    public Collection<DimensionDefinition> all() {
        return new ArrayList<>(dimensions.values());
    }

    public boolean isEmpty() {
        return dimensions.isEmpty();
    }

    public int size() {
        return dimensions.size();
    }

    public World world(String id) {
        return resolveWorld(id);
    }

    public World world(DimensionDefinition dimension) {
        return dimension == null ? null : world(dimension.id());
    }

    public boolean isLoaded(String id) {
        return world(id) != null;
    }

    public Location spawnLocation(String id) {
        World world = world(id);
        return world == null ? null : world.getSpawnLocation();
    }

    public void load(String id) {
        require(id);
        if (isLoaded(id)) return;

        Player player = firstOnlinePlayer();
        if (player == null) {
            server.plugin().getLogger().warning("DimensionManager: '" + id + "' konnte nicht geladen werden, kein Spieler online.");
            return;
        }
        dispatch("execute in " + id + " run tp " + player.getName());
    }

    public void teleport(Player player, String id) {
        require(id);
        World world = world(id);
        if (world != null) {
            teleportAsync(player, world.getSpawnLocation());
            return;
        }

        dispatch("execute in " + id + " run tp " + player.getName());
        server.scheduler().runLater(() -> {
            World loaded = world(id);
            if (loaded != null && player.isOnline()) {
                teleportAsync(player, loaded.getSpawnLocation());
            }
        }, 10L);
    }

    public void teleport(Player player, DimensionDefinition dimension) {
        if (dimension == null) throw new IllegalArgumentException("Dimension darf nicht null sein.");
        teleport(player, dimension.id());
    }

    public void teleport(Player player, String id, double x, double y, double z) {
        World world = world(id);
        if (world != null) {
            teleportAsync(player, new Location(world, x, y, z));
            return;
        }

        if (!contains(id) && !isVanillaDimension(id)) {
            throw new IllegalStateException("Dimension '" + id + "' ist nicht registriert oder geladen.");
        }

        dispatch("execute in " + id + " run tp " + player.getName());
        server.scheduler().runLater(() -> {
            World loaded = world(id);
            if (loaded != null && player.isOnline()) {
                teleportAsync(player, new Location(loaded, x, y, z));
            }
        }, 10L);
    }

    public void teleport(Player player, String id, double x, double y, double z, float yaw, float pitch) {
        World world = world(id);
        if (world != null) {
            teleportAsync(player, new Location(world, x, y, z, yaw, pitch));
            return;
        }

        if (!contains(id) && !isVanillaDimension(id)) {
            throw new IllegalStateException("Dimension '" + id + "' ist nicht registriert oder geladen.");
        }

        dispatch("execute in " + id + " run tp " + player.getName());
        server.scheduler().runLater(() -> {
            World loaded = world(id);
            if (loaded != null && player.isOnline()) {
                teleportAsync(player, new Location(loaded, x, y, z, yaw, pitch));
            }
        }, 10L);
    }

    private World resolveWorld(String id) {
        if (id == null) return null;

        World world = Bukkit.getWorld(id);
        if (world != null) return world;

        for (World candidate : Bukkit.getWorlds()) {
            if (candidate.getKey().asString().equalsIgnoreCase(id)
                    || candidate.getName().equalsIgnoreCase(id)) {
                return candidate;
            }
        }

        String simple = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        switch (simple) {
            case "overworld" -> {
                for (World candidate : Bukkit.getWorlds()) {
                    if (candidate.getEnvironment() == Environment.NORMAL) {
                        return candidate;
                    }
                }
            }
            case "the_nether" -> {
                for (World candidate : Bukkit.getWorlds()) {
                    if (candidate.getEnvironment() == Environment.NETHER) {
                        return candidate;
                    }
                }
            }
            case "the_end" -> {
                for (World candidate : Bukkit.getWorlds()) {
                    if (candidate.getEnvironment() == Environment.THE_END) {
                        return candidate;
                    }
                }
            }
            default -> {
            }
        }

        return null;
    }

    private boolean isVanillaDimension(String id) {
        return VANILLA_OVERWORLD.equalsIgnoreCase(id)
                || VANILLA_THE_NETHER.equalsIgnoreCase(id)
                || VANILLA_THE_END.equalsIgnoreCase(id);
    }

    private void teleportAsync(Player player, Location location) {
        player.teleportAsync(location).whenComplete((result, throwable) -> {
            if (throwable != null) {
                server.plugin().getLogger().warning("DimensionManager: Teleport fehlgeschlagen: " + throwable.getMessage());
            }
        });
    }

    private Player firstOnlinePlayer() {
        return Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
    }

    private void dispatch(String command) {
        try {
            CommandSender console = Bukkit.getConsoleSender();
            if (console != null) {
                Bukkit.dispatchCommand(console, command);
            }
        } catch (Throwable t) {
            server.plugin().getLogger().warning("DimensionManager: Befehl konnte nicht ausgeführt werden: " + t.getMessage());
        }
    }

    private void validate(DimensionDefinition dimension) {
        if (dimension.minY() + dimension.height() > 2032) {
            throw new IllegalArgumentException(
                    "Dimension '" + dimension.id() + "': min_y (" + dimension.minY() + ") + height ("
                            + dimension.height() + ") überschreitet 2032.");
        }
        if (dimension.height() <= 0) {
            throw new IllegalArgumentException("Dimension '" + dimension.id() + "': height muss positiv sein.");
        }
        if (dimension.logicalHeight() > dimension.height()) {
            throw new IllegalArgumentException(
                    "Dimension '" + dimension.id() + "': logical_height darf height nicht überschreiten.");
        }
    }

    public void deploy() {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world == null) {
            server.plugin().getLogger().warning("DimensionManager: Keine Welt geladen, Datapack konnte nicht angewendet werden.");
            return;
        }

        try {
            DimensionDatapack datapack = new DimensionDatapack(
                    namespace,
                    packName,
                    description,
                    new ArrayList<>(dimensions.values())
            );

            ManagedDatapack managed = new ManagedDatapack(server.plugin(), packName);
            managed.update(datapack::write, world.getWorldFolder().toPath());
            managed.load();
            server.plugin().getLogger().info("DimensionManager: Datapack '" + packName + "' mit " + dimensions.size() + " Dimensionen aktualisiert.");
        } catch (IOException e) {
            server.plugin().getLogger().warning("DimensionManager: Datapack konnte nicht aktualisiert werden: " + e.getMessage());
        }
    }

    public void redeploy() {
        deploy();
    }

    public void clear() {
        dimensions.clear();
    }
}
