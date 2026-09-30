package de.jakomi1.dimension;

import de.jakomi1.listener.EventListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verhindert den Zugriff auf deaktivierte Dimensionen.
 *
 * <p>Alle relevanten Wege in eine deaktivierte Dimension werden abgefangen:
 * normale Teleports, Portal-Teleports, Nether-Portale, End-Portale,
 * End-Gateways, Respawns und das nachtraegliche Erkennen eines Spielers
 * in einer deaktivierten Welt.
 *
 * <p>Beim Betreten eines Portals wird das Event direkt abgebrochen, statt zu
 * teleportieren. Dadurch kommt der Spieler nie kurz in die gesperrte
 * Dimension und es gibt keinen Teleport mitten im Entity-Tick.
 */
public final class DimensionAccessListener extends EventListener {

    private final Set<UUID> releasing = ConcurrentHashMap.newKeySet();
    private final Set<UUID> releaseQueued = ConcurrentHashMap.newKeySet();

    private final DimensionManager manager;

    public DimensionAccessListener(DimensionManager manager) {
        this.manager = manager;
    }

    private boolean isLocked(World world) {
        return world != null && manager.isDisabled(world);
    }

    private boolean isLocked(Location location) {
        return location != null && isLocked(location.getWorld());
    }

    private World findWorld(World.Environment environment) {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == environment) {
                return world;
            }
        }

        return null;
    }

    /**
     * Ermittelt bei PortalCreateEvent die relevante Zielwelt.
     *
     * <p>PortalCreateEvent besitzt keinen PortalType. Deshalb wird
     * CreateReason verwendet.
     */
    private World getWorldForPortalCreate(PortalCreateEvent event) {
        World sourceWorld = event.getWorld();

        return switch (event.getReason()) {
            case NETHER_PAIR -> {
                if (sourceWorld.getEnvironment() == World.Environment.NETHER) {
                    yield findWorld(World.Environment.NORMAL);
                }

                if (sourceWorld.getEnvironment() == World.Environment.NORMAL) {
                    yield findWorld(World.Environment.NETHER);
                }

                yield null;
            }

            case END_PLATFORM -> findWorld(World.Environment.THE_END);

            case FIRE -> null;
        };
    }

    public void release(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        Location target = manager.fallbackSpawn();

        if (target == null || target.getWorld() == null) {
            return;
        }

        if (isLocked(target.getWorld())) {
            return;
        }

        UUID uuid = player.getUniqueId();

        releasing.add(uuid);

        player.teleportAsync(target).whenComplete((result, throwable) ->
                releasing.remove(uuid)
        );
    }

    public void releaseLater(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        UUID uuid = player.getUniqueId();

        if (!releaseQueued.add(uuid)) {
            return;
        }

        manager.server().scheduler().runEntity(player, () -> {
            releaseQueued.remove(uuid);

            if (!player.isOnline()) {
                return;
            }

            if (!isLocked(player.getWorld())) {
                return;
            }

            release(player);
        });
    }

    public boolean isReleasing(Player player) {
        return player != null
                && releasing.contains(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        releaseQueued.remove(uuid);
        releasing.remove(uuid);
    }

    /**
     * Normale Spieler-Teleports.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (isReleasing(player)) {
            return;
        }

        if (isLocked(event.getTo())) {
            event.setCancelled(true);
        }
    }

    /**
     * Normale Entity-Teleports.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }

        if (isLocked(event.getTo())) {
            event.setCancelled(true);
        }
    }

    /**
     * Portal-Teleports von Spielern.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();

        if (isReleasing(player)) {
            return;
        }

        if (isLocked(event.getFrom()) || isLocked(event.getTo())) {
            event.setCancelled(true);
        }
    }

    /**
     * Portal-Teleports von Nicht-Spieler-Entities.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }

        if (isLocked(event.getFrom()) || isLocked(event.getTo())) {
            event.setCancelled(true);
        }
    }

    /**
     * Moderne Portal-Erstellung.
     *
     * <p>PortalCreateEvent besitzt keinen PortalType. NETHER_PAIR und
     * END_PLATFORM können aber anhand von CreateReason und World.Environment
     * eindeutig behandelt werden.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        World sourceWorld = event.getWorld();
        World targetWorld = getWorldForPortalCreate(event);

        if (isLocked(sourceWorld) || isLocked(targetWorld)) {
            event.setCancelled(true);
        }
    }

    /**
     * Legacy-Event für Entity-generierte Portale.
     *
     * <p>Hier steht der PortalType zur Verfuegung und wird direkt in die Art
     * der Zieldimension umgewandelt - ohne ueber die Ausgangswelt zu raten.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    @SuppressWarnings("deprecation")
    public void onEntityCreatePortal(EntityCreatePortalEvent event) {
        Environment target = DimensionManager.targetEnvironment(event.getPortalType());

        if (isLocked(event.getEntity().getWorld()) || manager.isDisabled(target)) {
            event.setCancelled(true);
        }
    }

    /**
     * Spieler betritt einen Portalblock, der in eine gesperrte Dimension
     * fuehren wuerde.
     *
     * <p>Hier wird das Event direkt abgebrochen. EntityPortalEnterEvent ist im
     * Canvas-Build Cancellable, deshalb wird gar nicht erst teleportiert. Damit
     * entfallen drei Probleme auf einmal: es gibt keinen Teleport mitten im
     * laufenden Entity-Tick (Folia), der Spieler ist nie kurz in der
     * gesperrten Dimension (kein unerwuenschtes Achievement) und das Portal
     * laeuft ins Leere.
     *
     * <p>LOWEST, damit der Abbruch vor allen anderen Plugins greift.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPortalEnter(EntityPortalEnterEvent event) {
        Environment target = DimensionManager.targetEnvironment(event.getPortalType());

        if (!manager.isDisabled(target)) {
            return;
        }

        event.setCancelled(true);
    }

    /**
     * Respawn in deaktivierter Dimension verhindern.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!isLocked(event.getRespawnLocation())) {
            return;
        }

        Location fallback = manager.fallbackSpawn();

        if (fallback != null
                && fallback.getWorld() != null
                && !isLocked(fallback.getWorld())) {
            event.setRespawnLocation(fallback);
        }
    }

    /**
     * Sicherheitsnetz für Spieler, die bereits in einer deaktivierten
     * Dimension angekommen sind.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        if (!isLocked(player.getWorld())) {
            return;
        }

        if (!isReleasing(player)) {
            releaseLater(player);
        }
    }
}