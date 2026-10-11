/*
 * EcseeSession – Enderchest-Pendant zur InvseeSession.
 * Basiert auf dem Port des Plugins "Invsee" (at.noahb.invsee, Autor: MCmitNoah,
 * GNU General Public License v3.0, GPL-3.0). Siehe LICENSE.md in diesem Repository.
 *
 * Erstellt von Jakomi1 (10.10.2026) für ProjectPlugin: nutzt das generische
 * Session-Framework des Invsee-Ports, aber für die 27-slot Enderchest.
 */
package de.jakomi1.project.ecsee.session;

import de.jakomi1.project.invsee.session.PlayerCache;
import de.jakomi1.project.invsee.session.Session;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static net.kyori.adventure.text.Component.text;

public class EcseeSession implements Session {

    private static final int ENDER_CHEST_SIZE = 27;

    private final Plugin plugin;
    private final UUID uuid;
    private final Set<UUID> subscribers;
    private final Inventory inventory;
    private final ReentrantLock lock = new ReentrantLock();
    private final PlayerCache playerCache = new PlayerCache();

    public EcseeSession(Plugin plugin, OfflinePlayer offlinePlayer, UUID subscriber) {
        this.plugin = plugin;
        this.uuid = offlinePlayer.getUniqueId();
        this.subscribers = new HashSet<>();

        String name = offlinePlayer.getName() == null ? "Unbekannt" : offlinePlayer.getName();
        Component title = text(name).append(text("'s Enderchest"));

        if (offlinePlayer instanceof Player player) {
            this.inventory = Bukkit.createInventory(player, ENDER_CHEST_SIZE, title);
        } else {
            this.inventory = Bukkit.getServer().createInventory(null, ENDER_CHEST_SIZE, title);
        }

        updateSubscriberInventory();
        addSubscriber(subscriber);
    }

    @Override
    public Plugin plugin() {
        return this.plugin;
    }

    @Override
    public UUID getUniqueIdOfObservedPlayer() {
        return this.uuid;
    }

    @Override
    public Set<UUID> getSubscribers() {
        return this.subscribers;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    @Override
    public void removeSubscriber(UUID subscriber) {
        this.subscribers.remove(subscriber);
    }

    @Override
    public boolean hasSubscriber(UUID uuid) {
        return this.subscribers.contains(uuid);
    }

    @Override
    public void updateSubscriberInventory() {
        update(() -> {
            OfflinePlayer offlinePlayer = Bukkit.getServer().getOfflinePlayer(this.uuid);
            Inventory enderChest = getEnderChest(offlinePlayer);
            if (enderChest == null) {
                return;
            }

            for (int i = 0; i < ENDER_CHEST_SIZE; i++) {
                this.inventory.setItem(i, enderChest.getItem(i));
            }
        });
    }

    @Override
    public void updateObservedInventory() {
        update(() -> {
            OfflinePlayer offlinePlayer = Bukkit.getServer().getOfflinePlayer(this.uuid);
            Inventory enderChest = getEnderChest(offlinePlayer);
            if (enderChest == null) {
                return;
            }

            for (int i = 0; i < ENDER_CHEST_SIZE; i++) {
                enderChest.setItem(i, this.inventory.getItem(i));
            }
        });
    }

    private Inventory getEnderChest(OfflinePlayer offlinePlayer) {
        if (offlinePlayer instanceof Player player) {
            return player.getEnderChest();
        }

        return getPlayerOffline(offlinePlayer).map(Player::getEnderChest).orElse(null);
    }

    @Override
    public ReentrantLock getLock() {
        return this.lock;
    }

    @Override
    public void cache(Player player) {
        this.playerCache.put(this.uuid, player);
    }

    @Override
    public Player getCachedPlayer() {
        return this.playerCache.getIfPresent(this.uuid);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        EcseeSession session = (EcseeSession) object;
        return Objects.equals(uuid, session.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
