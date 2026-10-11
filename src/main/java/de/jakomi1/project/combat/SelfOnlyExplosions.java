package de.jakomi1.project.combat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Explosionsquellen, die ausschliesslich den Ausloeser selbst treffen duerfen:
 * End Crystals und Respawn Anchors (sowie Betten, die im selben Blockpfad
 * explodieren).
 *
 * <p>Der Ausloeser wird bei beiden Quellen festgehalten: bei einem Respawn
 * Anchor ueber die Interaktion mit dem Block, bei einem End Crystal ueber das
 * Entzuenden mit Feuerstein und Eisen. Beim End Crystal wird der Besitzer im
 * PersistentDataContainer gespeichert und zusaetzlich in einer Map gehalten -
 * das Schutzfenster wird schon beim Entzuenden geoeffnet, weil ein Kristall
 * erst etwa zwei Sekunden spaeter explodiert.
 *
 * <p>Faellt der Ausloeser unbekannt aus, wird bewusst auf Null geschaetzt:
 * eine Schaetzung wuerde daneben stehende Spieler treffen, und genau das ist
 * nach den Regeln verboten.
 *
 * <p>Die Blast-Verarbeitung deckt zusaetzlich den indirekten Weg ueber
 * {@code EntityCombustByEntityEvent} ab, bei dem der Kristall nicht mehr als
 * Schadensquelle auftaucht.
 */
public final class SelfOnlyExplosions {

    /** Wie lange eine Interaktion als Ausloeser gilt. */
    private static final long TRIGGER_MEMORY_MS = 15_000L;

    /** Zuendverzoegerung eines Kristalls - Fenster ab dem Entzuenden offen halten. */
    private static final long CRYSTAL_FUSE_MS = 5_000L;

    /** Zeitfenster, in dem eine Explosion als "offen" gilt. */
    private static final long BLAST_WINDOW_MS = 2_000L;

    /** Explosionsradius plus Puffer - weiter weg kann niemand getroffen worden sein. */
    private static final double BLAST_RADIUS = 6.5D;

    private static volatile String namespace = "projectplugin";

    /**
     * @param trigger Spieler, der die Explosion ausgeloest hat, oder null wenn
     *                der Ausloeser nicht bekannt ist
     */
    public record Blast(UUID trigger) {

        public boolean belongsTo(Player player) {
            return trigger != null && player != null && trigger.equals(player.getUniqueId());
        }
    }

    private record BlockPos(String world, int x, int y, int z) {

        boolean matches(Location location) {
            if (location == null || location.getWorld() == null) return false;
            if (!location.getWorld().getName().equals(world)) return false;

            double dx = location.getX() - (x + 0.5D);
            double dy = location.getY() - (y + 0.5D);
            double dz = location.getZ() - (z + 0.5D);

            return dx * dx + dy * dy + dz * dz <= BLAST_RADIUS * BLAST_RADIUS;
        }
    }

    private record Trigger(UUID player, long until) {
    }

    private record Pending(Blast blast, long until) {
    }

    /**
     * Zuletzt von einem Spieler entzuendeter Kristall. Nach Spieler und nicht
     * nach Kristallposition verschluesselt, damit die Zuordnung auch dann
     * steht, wenn der Kristall zwischen Entzuenden und Explosion wandert.
     */
    private record Primed(BlockPos pos, long until) {
    }

    private static final Map<BlockPos, Trigger> blockOwners = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Trigger> crystalOwners = new ConcurrentHashMap<>();
    private static final Map<UUID, Primed> crystalPrimers = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Pending> openBlasts = new ConcurrentHashMap<>();

    private static volatile boolean warnedUnknownTrigger = false;

    private SelfOnlyExplosions() {
    }

    /** Namespace fuer den PersistentDataContainer der Kristalle. */
    public static void namespace(String id) {
        if (id != null && !id.isBlank()) {
            namespace = id;
        }
    }

    private static NamespacedKey crystalOwnerKey() {
        return new NamespacedKey(namespace, "crystal_owner");
    }

    /** Respawn Anchor oder Bett. */
    public static boolean isRespawnBlock(Block block) {
        if (block == null) return false;

        Material type = block.getType();

        return type == Material.RESPAWN_ANCHOR || type.name().endsWith("_BED");
    }

    /** Merkt sich, wer den Anker benutzt hat. */
    public static void trackAnchor(Player player, Block block) {
        if (player == null || block == null) return;

        blockOwners.put(key(block), new Trigger(player.getUniqueId(),
                System.currentTimeMillis() + TRIGGER_MEMORY_MS));
    }

    /**
     * Merkt sich, wer den Kristall entzuendet hat, und oeffnet direkt das
     * Schutzfenster. Bewusst schon hier und nicht erst bei der Explosion:
     * der Ausloeser steht damit fest, unabhaengig davon, ob Paper den Kristall
     * ueberhaupt als Schadensquelle mitliefert.
     */
    public static void trackCrystalOwner(Player player, EnderCrystal crystal) {
        if (player == null || crystal == null) return;

        long now = System.currentTimeMillis();
        UUID owner = player.getUniqueId();

        crystal.getPersistentDataContainer().set(crystalOwnerKey(), PersistentDataType.STRING,
                owner.toString());

        BlockPos pos = key(crystal.getLocation());
        crystalOwners.put(pos, new Trigger(owner, now + TRIGGER_MEMORY_MS));
        crystalPrimers.put(owner, new Primed(pos, now + CRYSTAL_FUSE_MS));
        openBlasts.put(pos, new Pending(new Blast(owner), now + CRYSTAL_FUSE_MS));
    }

    /**
     * Hat dieser Spieler gerade einen Kristall in Reichweite entzuendet?
     *
     * <p>Dient als Rueckfall, falls die Zuordnung an der Kristallposition
     * scheitert.
     */
    public static boolean primedNearby(Player player) {
        if (player == null) return false;

        Primed primed = crystalPrimers.get(player.getUniqueId());
        if (primed == null) return false;

        if (primed.until() <= System.currentTimeMillis()) {
            crystalPrimers.remove(player.getUniqueId());
            return false;
        }

        return primed.pos().matches(player.getLocation());
    }

    /**
     * Wer den Kristall entzuendet hat, oder null wenn unbekannt.
     */
    public static UUID crystalOwner(EnderCrystal crystal) {
        if (crystal == null) return null;

        String stored = crystal.getPersistentDataContainer().get(crystalOwnerKey(), PersistentDataType.STRING);
        if (stored != null && !stored.isBlank()) {
            try {
                return UUID.fromString(stored);
            } catch (IllegalArgumentException ignored) {
                // faellt auf die Map zurueck
            }
        }

        Trigger trigger = crystalOwners.get(key(crystal.getLocation()));
        if (trigger == null || trigger.until() <= System.currentTimeMillis()) return null;

        return trigger.player();
    }

    /**
     * Ausloeser einer Kristall-Explosion, wie er im Schadensereignis
     * auftaucht.
     *
     * @return null wenn die Schadensquelle kein Kristall ist
     */
    public static Blast crystalBlast(EntityDamageEvent event) {
        if (event == null) return null;

        var source = event.getDamageSource();

        EnderCrystal crystal = asCrystal(source.getDirectEntity());
        if (crystal == null) crystal = asCrystal(source.getCausingEntity());
        if (crystal == null) return null;

        return new Blast(crystalOwner(crystal));
    }

    /** Kommt der Schaden von einem End Crystal? */
    public static boolean isCrystalDamage(EntityDamageEvent event) {
        if (event == null) return false;

        var source = event.getDamageSource();

        return asCrystal(source.getDirectEntity()) != null || asCrystal(source.getCausingEntity()) != null;
    }

    /** Markiert die Explosion eines Anker- oder Bettblocks als "nur Ausloeser". */
    public static void openBlast(Block block) {
        if (block == null) return;

        BlockPos pos = key(block);
        Trigger trigger = blockOwners.get(pos);

        UUID owner = trigger != null && trigger.until() > System.currentTimeMillis()
                ? trigger.player()
                : null;

        if (owner == null) warnUnknownTrigger("Respawn Anchor/Bett");

        openBlasts.put(pos, new Pending(new Blast(owner), System.currentTimeMillis() + BLAST_WINDOW_MS));
    }

    /**
     * Markiert eine Explosion an einer Stelle als "nur Ausloeser".
     */
    public static void openBlast(Location at, UUID trigger) {
        openBlast(at, trigger, BLAST_WINDOW_MS);
    }

    /**
     * Verlaengert das Schutzfenster eines Kristalls bis zum Ende seiner
     * Zuendung.
     */
    public static void openCrystalBlast(EnderCrystal crystal) {
        if (crystal == null) return;

        UUID owner = crystalOwner(crystal);
        if (owner == null) warnUnknownTrigger("End Crystal");

        openBlast(crystal.getLocation(), owner, CRYSTAL_FUSE_MS);
    }

    /**
     * Laeuft eine geschuetzte Explosion ohne bekannten Ausloeser durch, wird
     * auch derjenige geschuetzt, der sie ausgeloest hat. Einmal laut melden.
     */
    private static void warnUnknownTrigger(String what) {
        if (warnedUnknownTrigger) return;

        warnedUnknownTrigger = true;

        Bukkit.getLogger().warning(
                what + ": Ausloeser unbekannt - alle Spieler wurden geblockt. "
                        + "Pruefen, ob PlayerInteractEntityEvent/PlayerInteractAtEntityEvent ankommen.");
    }

    private static void openBlast(Location at, UUID trigger, long windowMs) {
        if (at == null) return;

        openBlasts.put(key(at), new Pending(new Blast(trigger),
                System.currentTimeMillis() + windowMs));
    }

    /**
     * Offene Explosion in der Naehe des Ortes. Fenster mit bekanntem
     * Ausloeser gewinnen, damit nicht ein fremdes Fenster den Ausloeser
     * verdeckt.
     *
     * @return null wenn dort gerade keine geschuetzte Explosion lief
     */
    public static Blast nearBlast(Location location) {
        if (location == null) return null;

        long now = System.currentTimeMillis();
        Blast unknown = null;

        for (Map.Entry<BlockPos, Pending> entry : openBlasts.entrySet()) {
            Pending pending = entry.getValue();

            if (pending.until() <= now) {
                openBlasts.remove(entry.getKey());
                continue;
            }

            if (!entry.getKey().matches(location)) continue;

            if (pending.blast().trigger() != null) return pending.blast();
            if (unknown == null) unknown = pending.blast();
        }

        if (unknown == null) sweep(now);

        return unknown;
    }

    /** Raeumt abgelaufene Eintraege der Besitzer-Maps ab. */
    private static void sweep(long now) {
        blockOwners.entrySet().removeIf(entry -> entry.getValue().until() <= now);
        crystalOwners.entrySet().removeIf(entry -> entry.getValue().until() <= now);
        crystalPrimers.entrySet().removeIf(entry -> entry.getValue().until() <= now);
    }

    private static EnderCrystal asCrystal(Entity entity) {
        return entity instanceof EnderCrystal crystal ? crystal : null;
    }

    private static BlockPos key(Block block) {
        return key(block.getLocation());
    }

    private static BlockPos key(Location location) {
        return new BlockPos(
                location.getWorld() == null ? "" : location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }
}
