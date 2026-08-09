/*
 * PlayerCache – einfacher TTL-Cache als Ersatz für Guava CacheBuilder
 * (aus dem Port des "Invsee"-Plugins, at.noahb.invsee, GPL-3.0).
 * Siehe LICENSE.md in diesem Repository.
 */
package de.jakomi1.project.invsee.session;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerCache {

    private static final long TTL_MILLIS = 10_000;

    private final Map<UUID, Entry> map = new ConcurrentHashMap<>();

    public Player getIfPresent(UUID uuid) {
        if (uuid == null) return null;

        Entry entry = map.get(uuid);
        if (entry == null) return null;

        if (System.currentTimeMillis() > entry.expiresAt()) {
            map.remove(uuid, entry);
            return null;
        }

        return entry.player();
    }

    public void put(UUID uuid, Player player) {
        if (uuid == null || player == null) return;
        map.put(uuid, new Entry(player, System.currentTimeMillis() + TTL_MILLIS));
    }

    public void invalidate(UUID uuid) {
        if (uuid == null) return;
        map.remove(uuid);
    }

    private record Entry(Player player, long expiresAt) {
    }
}
