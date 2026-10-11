package de.jakomi1.player;

import de.jakomi1.permission.RoleTable;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.project.playtime.PlaytimeTable;
import de.jakomi1.project.whitelist.WhitelistTable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Findet einen Spieler auch dann, wenn er gerade offline ist.
 *
 * <p>{@code Bukkit.getOfflinePlayer(name)} hilft hier nicht weiter: Ist die
 * UUID lokal nicht bekannt, fragt der Aufruf beim Mojang-Server nach und
 * blockiert dabei den Server-Tick. Gesucht wird deshalb ausschliesslich
 * lokal - erst die laufenden Spieler, dann die bekannten Tabellen des
 * Projekts und erst zuletzt der Mojang-Cache, den der Server bereits
 * mitbringt.
 */
public final class PlayerLookup {

    /**
     * Ein aufgeloester Spieler.
     *
     * @param uuid   UUID des Spielers
     * @param name   Name, unter dem der Spieler dem Server bekannt ist
     * @param online true, wenn der Spieler gerade verbunden ist
     */
    public record ResolvedPlayer(UUID uuid, String name, boolean online) {
    }

    private PlayerLookup() {
    }

    /**
     * @return null, wenn der Name dem Server vollstaendig unbekannt ist
     */
    public static ResolvedPlayer resolve(ProjectServer server, String name) {
        if (server == null || name == null || name.isBlank()) return null;

        String query = name.trim();

        Player online = Bukkit.getPlayerExact(query);
        if (online != null) {
            return new ResolvedPlayer(online.getUniqueId(), online.getName(), true);
        }

        ResolvedPlayer known = fromWhitelist(server, query);
        if (known == null) known = fromRoles(server, query);
        if (known == null) known = fromPlaytime(server, query);
        if (known == null) known = fromServerCache(query);

        return known;
    }

    /** Die Whitelist kennt Namen samt UUID - und bleibt bewusst erhalten. */
    private static ResolvedPlayer fromWhitelist(ProjectServer server, String query) {
        for (WhitelistTable.WhitelistEntry entry : server.whitelist().table().all()) {
            if (!matches(entry.name(), query)) continue;

            // "/whitelist add" legt Eintraege ohne UUID an, solange der
            // Spieler den Server nie betreten hat. Eine solche Zeile ist
            // kein Ziel - ein null-UUID-Eintrag wuerde nur craschen.
            if (entry.uuid() == null) continue;

            return new ResolvedPlayer(entry.uuid(), entry.name(), false);
        }

        return null;
    }

    private static ResolvedPlayer fromRoles(ProjectServer server, String query) {
        for (Map.Entry<String, RoleTable.RoleEntry> entry
                : server.permissions().table().entrySet()) {

            if (!matches(entry.getValue().name(), query)) continue;

            UUID uuid = parseUuid(entry.getKey());
            if (uuid == null) continue;

            return new ResolvedPlayer(uuid, entry.getValue().name(), false);
        }

        return null;
    }

    private static ResolvedPlayer fromPlaytime(ProjectServer server, String query) {
        for (Map.Entry<UUID, PlaytimeTable.PlaytimeEntry> entry
                : server.playtime().table().entrySet()) {

            if (!matches(entry.getValue().name(), query)) continue;

            return new ResolvedPlayer(entry.getKey(), entry.getValue().name(), false);
        }

        return null;
    }

    /**
     * Zuletzt der Mojang-Cache des Servers. Findet sich dort nichts, gilt der
     * Name als unbekannt - ein blockierender Aufruf an Mojang waere an dieser
     * Stelle bewusst falsch.
     */
    private static ResolvedPlayer fromServerCache(String query) {
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(query);
        if (cached == null || cached.getUniqueId() == null) return null;

        String known = cached.getName() != null ? cached.getName() : query;

        return new ResolvedPlayer(cached.getUniqueId(), known, false);
    }

    private static boolean matches(String stored, String query) {
        return stored != null && stored.equalsIgnoreCase(query);
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
