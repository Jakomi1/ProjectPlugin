package de.jakomi1.project.respawn;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.function.BiFunction;

/**
 * Merkt sich fuer jeden Spieler, ob und wo er einen gueltigen Respawn-Punkt
 * besitzt.
 *
 * <p>Gespeichert wird das im PersistentDataContainer des Spielers und damit
 * ueber Neustarts hinweg. Ohne diese Information wuerde ein Spieler, dessen
 * Bett oder Anker nach einem Relog fehlt, beim Join wie ein Neuspieler
 * behandelt und faelschlich zum Spawn teleportiert.
 *
 * <p>Der Manager ist opt-in ({@link #defaultState()} false). Er kuemmert sich
 * um Tabelle, Listener und den Respawn-Fallback. Wo ein Spieler ohne
 * Bett/Anker landen soll, bleibt dem Aufrufer ueberlassen
 * ({@link #respawnResolver(BiFunction)}).
 */
public final class RespawnManager implements Manager {

    /** Blocktypen, die als gueltiger Respawn-Punkt zaehlen. */
    private static final String BED_SUFFIX = "_bed";
    private static final String ANCHOR = "respawn_anchor";

    private final ProjectServer server;

    private boolean enabled;
    private RespawnListener listener;

    private BiFunction<Player, Location, Location> respawnResolver = (player, location) -> null;

    public RespawnManager(ProjectServer server) {
        this.server = server;
    }

    @Override
    public RespawnManager enable() {
        if (enabled) return this;
        enabled = true;

        listener = new RespawnListener(this);
        listener.register(server.plugin());

        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;

        if (listener != null) {
            listener.unregister();
            listener = null;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean defaultState() {
        return false;
    }

    /**
     * Bestimmt, wo ein Spieler ohne nutzbares Bett oder Anker landet.
     *
     * @param resolver bekommt den Spieler und die Vanilla-Respawn-Position und
     *                 liefert die endgueltige Position; {@code null} laesst die
     *                 Vanilla-Position unveraendert
     */
    public RespawnManager respawnResolver(BiFunction<Player, Location, Location> resolver) {
        this.respawnResolver = resolver == null ? (player, location) -> null : resolver;
        return this;
    }

    /** Endgueltige Respawn-Position, oder {@code null} um Vanilla zu belassen. */
    public Location applyRespawn(Player player, Location vanilla) {
        return respawnResolver.apply(player, vanilla);
    }

    /** Ist der Block ueberhaupt ein Bett oder Respawn Anchor? */
    public boolean isRespawnBlock(Block block) {
        if (block == null) return false;

        String name = block.getType().name().toLowerCase(Locale.ROOT);

        return name.endsWith(BED_SUFFIX) || name.equals(ANCHOR);
    }

    /**
     * Speichert den Respawn-Punkt, falls der Block ein Bett oder Anker ist.
     *
     * @return true wenn ein Punkt gespeichert wurde
     */
    public boolean remember(Player player, Block block) {
        if (player == null || !isRespawnBlock(block)) return false;

        return remember(player, block.getLocation());
    }

    /**
     * Speichert die Position als Respawn-Punkt.
     *
     * @return true wenn ein Punkt gespeichert wurde
     */
    public boolean remember(Player player, Location location) {
        if (player == null || location == null || location.getWorld() == null) return false;

        player.getPersistentDataContainer().set(key(), PersistentDataType.STRING, encode(location));

        return true;
    }

    /** Entfernt einen gespeicherten Respawn-Punkt wieder. */
    public void forget(Player player) {
        if (player == null) return;

        player.getPersistentDataContainer().remove(key());
    }

    /** Besitzt der Spieler einen gespeicherten Respawn-Punkt? */
    public boolean has(Player player) {
        return player != null && player.getPersistentDataContainer().has(key(), PersistentDataType.STRING);
    }

    /**
     * Ein Punkt zaehlt nur, wenn der Spieler ihn auch benutzen kann. Ein
     * Spieler, der sein Bett entfernt hat, wird also wieder zum Spawn
     * geschickt - genau wie es Vanilla tut.
     */
    public boolean hasUsable(Player player) {
        if (!has(player)) return false;

        Location location = decode(player);
        if (location == null) return false;

        return isRespawnBlock(location.getBlock());
    }

    /**
     * Position, auf die der Spieler zurueckgeschickt wird. Der y-Wert wird
     * auf den obersten freien Block angehoben, damit niemand in einem
     * Einschlaf-Block stecken bleibt.
     */
    public Location location(Player player) {
        Location location = decode(player);
        if (location == null) return null;

        return highestStandable(location);
    }

    private Location decode(Player player) {
        String stored = player.getPersistentDataContainer().get(key(), PersistentDataType.STRING);
        if (stored == null || stored.isBlank()) return null;

        int firstSeparator = stored.indexOf(';');
        if (firstSeparator < 0) return null;

        String worldName = stored.substring(0, firstSeparator);

        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        String[] parts = stored.substring(firstSeparator + 1).split(":", 4);
        if (parts.length != 4) return null;

        try {
            return new Location(
                    world,
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Float.parseFloat(parts[3]),
                    0.0F
            );
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String encode(Location location) {
        return "%s;%s:%s:%s:%s".formatted(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw()
        );
    }

    /** Obersten Block suchen, auf dem der Spieler sicher stehen kann. */
    private Location highestStandable(Location location) {
        var world = location.getWorld();
        if (world == null) return location;

        int start = Math.min(location.getBlockY() + 2, world.getMaxHeight() - 2);

        for (int y = start; y > location.getBlockY() - 4 && y > world.getMinHeight(); y--) {
            var feet = location.getBlock().getRelative(0, y - location.getBlockY(), 0);

            if (!feet.isPassable()) continue;
            if (!feet.getRelative(0, 1, 0).isPassable()) continue;

            return feet.getLocation().add(0.5D, 0.0D, 0.5D);
        }

        return location;
    }

    private NamespacedKey key() {
        String id = server.plugin().getId();

        return new NamespacedKey(id != null && !id.isBlank() ? id : "projectplugin", "respawn_point");
    }

    public ProjectServer server() {
        return server;
    }
}
