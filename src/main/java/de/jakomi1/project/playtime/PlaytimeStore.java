package de.jakomi1.project.playtime;

import de.jakomi1.project.ProjectServer;
import de.jakomi1.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Spielzeit-Zaehler.
 *
 * <p>Quelle der Wahrheit ist die Vanilla-Statistik
 * {@link Statistic#TOTAL_WORLD_TIME}, weil sie nur die tatsaechlich
 * verbrachte Spielzeit zaehlt. Der Wert wird regelmaessig in die Tabelle
 * uebernommen, sodass er auch einen Absturz uebersteht.
 *
 * <p>Damit {@code /topten} nie sortieren muss, existiert ein fertiger
 * Snapshot. Er wird im Hintergrund erneuert; der Command liest danach nur
 * noch eine unveraenderliche Liste. Damit laeuft weder eine Sortierung noch
 * ein Dateizugriff auf dem Server-Thread.
 */
public final class PlaytimeStore {

    /** Wie oft die laufenden Werte in die Tabelle geschrieben werden. */
    private static final long SAVE_PERIOD_TICKS = 20L;

    /** Wie oft die Top-Liste erneuert wird. */
    private static final long SNAPSHOT_PERIOD_TICKS = 40L;

    /** Eintraege pro Snapshot sind begrenzt, damit nichts unbegrenzt waechst. */
    private static final int SNAPSHOT_LIMIT = 500;

    private static final AtomicReference<List<PlaytimeTable.PlaytimeEntry>> topSnapshot =
            new AtomicReference<>(List.of());

    private static final Map<UUID, String> names = new ConcurrentHashMap<>();

    private static PlaytimeTable table;
    private static Scheduler.Task saveTask;
    private static Scheduler.Task snapshotTask;

    private PlaytimeStore() {
    }

    public static void enable(ProjectServer server, PlaytimeTable playtimeTable) {
        table = playtimeTable;

        seedKnownPlayers(server.scheduler());

        saveTask = server.scheduler().runTimer(PlaytimeStore::save, SAVE_PERIOD_TICKS, SAVE_PERIOD_TICKS);
        snapshotTask = server.scheduler().runTimer(
                PlaytimeStore::refreshSnapshot, SNAPSHOT_PERIOD_TICKS, SNAPSHOT_PERIOD_TICKS
        );
    }

    public static void disable() {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }

        if (snapshotTask != null) {
            snapshotTask.cancel();
            snapshotTask = null;
        }

        table = null;
    }

    /** Merkt sich den Namen, damit die Anzeige auch offline stimmt. */
    public static void track(Player player) {
        if (player == null || table == null) return;

        names.put(player.getUniqueId(), player.getName());

        table.put(player.getUniqueId(), new PlaytimeTable.PlaytimeEntry(
                player.getName(),
                secondsOf(player),
                System.currentTimeMillis()
        ));
    }

    /** Wird beim Quit gerufen, solange der Spieler noch geladen ist. */
    public static void saveNow(Player player) {
        if (player == null || table == null) return;

        names.put(player.getUniqueId(), player.getName());

        table.put(player.getUniqueId(), new PlaytimeTable.PlaytimeEntry(
                player.getName(),
                secondsOf(player),
                System.currentTimeMillis()
        ));
    }

    /**
     * Spielzeit in Sekunden. Fuer Online-Spieler kommt der aktuelle
     * Statistikwert direkt, sonst der zuletzt gespeicherte Wert aus der
     * Tabelle.
     */
    public static long seconds(String name) {
        if (name == null || name.isBlank() || table == null) return 0L;

        UUID online = onlineId(name);
        if (online != null) return secondsOf(Bukkit.getPlayer(online));

        PlaytimeTable.PlaytimeEntry entry = table.entry(name);
        if (entry == null) return 0L;

        Player onlinePlayer = Bukkit.getPlayer(entry.name());
        if (onlinePlayer != null) return secondsOf(onlinePlayer);

        return entry.seconds();
    }

    public static String nameOf(String name) {
        if (name == null || name.isBlank() || table == null) return name;

        PlaytimeTable.PlaytimeEntry entry = table.entry(name);
        if (entry == null) return name;

        return entry.name();
    }

    /** Ist der Name auf diesem Server schon einmal aufgetaucht? */
    public static boolean isKnown(String name) {
        return table != null && table.hasName(name);
    }

    /** Alle jemals gesehenen Namen, sortiert - Basis der Tab-Vorschlaege. */
    public static List<String> knownNames() {
        if (table == null) return List.of();

        List<String> result = new ArrayList<>(table.size());

        for (PlaytimeTable.PlaytimeEntry entry : table.values()) {
            if (entry.name() != null && !entry.name().isBlank()) {
                result.add(entry.name());
            }
        }

        result.sort(String.CASE_INSENSITIVE_ORDER);

        return List.copyOf(result);
    }

    /**
     * Fertige Top-Liste. Kein Sortieren, kein Dateizugriff - nur ein Blick in
     * den bereits erzeugten Snapshot.
     */
    public static List<PlaytimeTable.PlaytimeEntry> top(int limit) {
        List<PlaytimeTable.PlaytimeEntry> snapshot = topSnapshot.get();
        if (limit <= 0 || snapshot.isEmpty()) return List.of();

        return snapshot.subList(0, Math.min(limit, snapshot.size()));
    }

    private static void save() {
        if (table == null) return;

        for (Player player : List.copyOf(Bukkit.getOnlinePlayers())) {
            saveNow(player);
        }
    }

    private static void refreshSnapshot() {
        if (table == null) return;

        List<PlaytimeTable.PlaytimeEntry> entries = new ArrayList<>(table.values());

        // Online-Spieler koennen juenger sein als der letzte Snapshot und
        // werden deshalb mit dem Live-Wert ueberschrieben.
        for (Player player : List.copyOf(Bukkit.getOnlinePlayers())) {
            String name = names.getOrDefault(player.getUniqueId(), player.getName());

            entries.removeIf(entry -> entry.name() != null && entry.name().equalsIgnoreCase(name));
            entries.add(new PlaytimeTable.PlaytimeEntry(name, secondsOf(player),
                    System.currentTimeMillis()));
        }

        entries.sort(Comparator.comparingLong(PlaytimeTable.PlaytimeEntry::seconds).reversed());

        if (entries.size() > SNAPSHOT_LIMIT) {
            entries = new ArrayList<>(entries.subList(0, SNAPSHOT_LIMIT));
        }

        topSnapshot.set(List.copyOf(entries));
    }

    /**
     * Namen und Spielzeiten von Spielern uebernehmen, die schon vor dem
     * Start dieser Tabelle auf dem Server waren. Das passiert einmalig und
     * asynchron, damit die Vorschlaege vollstaendig sind, ohne dass der Start
     * laenger dauert. Der Snapshot wird danach vom normalen Task erneuert -
     * von hier aus darf nicht auf Bukkit-Entitaeten zugegriffen werden.
     */
    private static void seedKnownPlayers(Scheduler scheduler) {
        scheduler.runAsync(() -> {
            if (table == null) return;

            for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
                String name = offline.getName();
                if (name == null || name.isBlank()) continue;

                UUID uuid = offline.getUniqueId();
                PlaytimeTable.PlaytimeEntry existing = table.get(uuid);

                if (existing != null) {
                    if (existing.name() == null || !existing.name().equalsIgnoreCase(name)) {
                        table.put(uuid, new PlaytimeTable.PlaytimeEntry(
                                name, existing.seconds(), existing.lastSeen()
                        ));
                    }
                    continue;
                }

                table.put(uuid, new PlaytimeTable.PlaytimeEntry(name, statisticsSeconds(offline), 0L));
            }
        });
    }

    private static long secondsOf(Player player) {
        return player == null ? 0L : statisticsSeconds(player);
    }

    private static long statisticsSeconds(OfflinePlayer player) {
        try {
            return Math.max(0, player.getStatistic(Statistic.TOTAL_WORLD_TIME)) / 20L;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static UUID onlineId(String name) {
        Player player = Bukkit.getPlayerExact(name);
        return player == null ? null : player.getUniqueId();
    }

    /** Formatiert Sekunden als {@code 3d 4h 05min}. */
    public static String format(long seconds) {
        long total = Math.max(0L, seconds);

        long days = total / 86400L;
        long hours = (total % 86400L) / 3600L;
        long minutes = (total % 3600L) / 60L;

        if (days > 0L) {
            return "%dd %dh %02dmin".formatted(days, hours, minutes);
        }

        return "%dh %02dmin".formatted(hours, minutes);
    }
}
