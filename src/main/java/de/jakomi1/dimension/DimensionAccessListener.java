package de.jakomi1.dimension;

import de.jakomi1.listener.EventListener;
import org.bukkit.Location;
import org.bukkit.World;
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
 * Verhindert, dass eine deaktivierte Dimension betreten wird.
 *
 * <p>Es reicht nicht, nur das Teleportieren zu blocken: Nether-Portale,
 * End-Portale, Respawn-Anker, Betten und End-Gateways umgehen einen
 * Teleport-Befehl vollstaendig. Deshalb werden hier alle Wege abgedeckt, die
 * zu einer Weltwechsel fuehren koennen - und zwar bevor der Wechsel
 * stattfindet. Wer bereits drin steht, wird ueber
 * {@link PlayerChangedWorldEvent} zurueckgeholt.
 *
 * <p>Wichtig fuer Folia: der Handler laedt keine Chunks, erzeugt keine
 * Welt und ruft kein synchrones {@code teleport()} auf. Fuer das
 * Zurueckholen wird {@code teleportAsync()} benutzt - aber nie mitten im
 * Tick des Spielers. Genau das waere namens der Grund, warum das Portal-Event
 * hier gefährlich ist: {@code EntityPortalEnterEvent} feuert aus
 * {@code entityInside()} heraus, also noch innerhalb von
 * {@code applyEffectsFromBlocks()}. Ein Teleport an dieser Stelle setzt den
 * Spieler sofort auf {@code removed=CHANGED_DIMENSION}, der Tick laeuft aber
 * weiter bis {@code ServerPlayer.onInsideBlock()} und dort feuert
 * {@code EnterBlockTrigger} - ein Main-Thread-Criterion. Ergebnis auf Folia:
 * {@code ThreadViolationException} und der Spieler fliegt mit "Internal server
 * error" raus. Deshalb wird das Herausholen immer ueber den
 * Entity-Scheduler um einen Tick verschoben, wo der Tick sauber zu Ende
 * gelaufen ist.
 */
public final class DimensionAccessListener extends EventListener {

    /**
     * Spieler, die gerade herausgeholt werden. Ohne diese Ausnahme wuerde
     * der eigene Rettungsversuch am Teleport-Event scheitern und der Spieler
     * bliebe in der gesperrten Dimension.
     */
    private final Set<UUID> releasing = ConcurrentHashMap.newKeySet();

    /**
     * Spieler, fuer die das Herausholen bereits fuer den naechsten Tick
     * eingereiht ist. {@code EntityPortalEnterEvent} feuert jeden Tick neu,
     * solange der Spieler im Portalblock steht - ohne diese Sperre wuerde
     * fuer jeden Tick ein Task eingeplant.
     *
     * <p>Bewusst getrennt von {@link #releasing}: waehrend der Wartezeit
     * soll der Spieler weiterhin als normaler Besucher gelten, damit
     * {@link #onPlayerPortal(PlayerPortalEvent)} sein Portal abweist und die
     * Rettung der einzige Ausgang bleibt.
     */
    private final Set<UUID> releaseQueued = ConcurrentHashMap.newKeySet();

    private final DimensionManager manager;

    public DimensionAccessListener(DimensionManager manager) {
        this.manager = manager;
    }

    /**
     * Befreit einen Spieler aus einer gesperrten Dimension. Wird benutzt,
     * sobald eine Dimension deaktiviert wird, waehrend jemand noch drin ist.
     */
    public void release(Player player) {
        if (player == null || !player.isOnline()) return;

        Location target = manager.fallbackSpawn();
        if (target == null) return;

        releasing.add(player.getUniqueId());

        player.teleportAsync(target).whenComplete((result, throwable) ->
                releasing.remove(player.getUniqueId()));
    }

    /**
     * Holt einen Spieler aus einer gesperrten Dimension, aber erst im
     * naechsten Tick. Muss fuer alle Events benutzt werden, die mitten im
     * Entity-Tick feuern - vor allem {@link #onPortalEnter(EntityPortalEnterEvent)}.
     *
     * <p>Der Task laeuft ueber den Entity-Scheduler und damit auf dem
     * Region-Thread des Spielers. Ein dort aufgerufenes {@code teleportAsync()}
     * ist auf Folia ein regulaerer, erlaubter Vorgang.
     */
    public void releaseLater(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!releaseQueued.add(player.getUniqueId())) return;

        manager.server().scheduler().runEntity(player, () -> {
            releaseQueued.remove(player.getUniqueId());
            release(player);
        });
    }

    /**
     * Raeumt die Merkmale auf, sonst bleiben UUIDs von Spielern zurueck, die
     * sich mitten im Portalblock ausloggen.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        releaseQueued.remove(uuid);
        releasing.remove(uuid);
    }

    /** Darf dieser Spieler gerade geholt werden? */
    public boolean isReleasing(Player player) {
        return player != null && releasing.contains(player.getUniqueId());
    }

    private boolean isLocked(World world) {
        return world != null && manager.isDisabled(world);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (isReleasing(player)) return;

        if (isLocked(event.getTo().getWorld())) {
            event.setCancelled(true);
        }
    }

    /**
     * Nicht-Spieler. EntityTeleportEvent und PlayerTeleportEvent sind
     * getrennte Klassen, deshalb laeuft Spieler hier bewusst nicht mit
     * durch - die Ausnahme wuerde sonst nur den eigenen Rettungsversuch
     * aufhalten.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTeleport(EntityTeleportEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) return;

        Location to = event.getTo();
        if (to != null && isLocked(to.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        if (isReleasing(player)) return;

        if (isLocked(event.getFrom().getWorld()) || isLocked(event.getTo().getWorld())) {
            event.setCancelled(true);
        }
    }

    /** Portale fuer Nicht-Spieler. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) return;

        if (isLocked(event.getFrom().getWorld()) || isLocked(event.getTo().getWorld())) {
            event.setCancelled(true);
        }
    }

    /**
     * Betreten eines Portalblocks. Das PortalEvent feuert erst beim
     * tatsaechlichen Wechsel, hier ist also noch Zeit zu reagieren.
     * EntityPortalEnterEvent ist nicht cancellable - der Weg wird deshalb
     * ueber den naechsten Teleport abgeschnitten.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPortalEnter(EntityPortalEnterEvent event) {
        if (!isLocked(event.getLocation().getWorld())) return;

        if (event.getEntity() instanceof Player player && !isReleasing(player)) {
            releaseLater(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        if (isLocked(event.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityCreatePortal(EntityCreatePortalEvent event) {
        if (isLocked(event.getEntity().getWorld())) {
            event.setCancelled(true);
        }
    }

    /**
     * Bett oder Respawn-Anker in einer gesperrten Dimension.
     * PlayerRespawnEvent ist nicht cancellable, deshalb wird einfach das
     * Ziel umgeschrieben.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!isLocked(event.getRespawnLocation().getWorld())) return;

        Location fallback = manager.fallbackSpawn();
        if (fallback != null) {
            event.setRespawnLocation(fallback);
        }
    }

    /**
     * Letztes Sicherheitsnetz: Wer dennoch in einer gesperrten Dimension
     * landet - etwa weil die Dimension erst danach gesperrt wurde - wird
     * hier zurueckgeholt.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        if (!isLocked(event.getPlayer().getWorld())) return;

        releaseLater(event.getPlayer());
    }
}
