/*
 * Session – Port aus dem Plugin "Invsee".
 * Original: at.noahb.invsee (Autor: MCmitNoah), GNU General Public License v3.0 (GPL-3.0).
 * Siehe LICENSE.md in diesem Repository.
 *
 * Geändert von Jakomi1 (08.08.2026) für ProjectPlugin:
 *  - an die Library-Architektur (ProjectServer/Manager) angepasst,
 *  - auf Paper 26.2 aktualisiert,
 *  - Guava/Paper-MaterialTags-Abhängigkeiten entfernt.
 */
package de.jakomi1.project.invsee.session;

import com.mojang.authlib.GameProfile;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static net.kyori.adventure.text.Component.text;

public interface Session {

    default void addSubscriber(UUID subscriber) {
        if (subscriber == null) return;
        if (hasSubscriber(subscriber)) return;
        Player player = Bukkit.getServer().getPlayer(subscriber);
        if (player == null) return;

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(getUniqueIdOfObservedPlayer());

        Optional<Player> other = getPlayerOffline(offlinePlayer);
        if (other.isEmpty()) {
            return;
        }

        getSubscribers().add(subscriber);
        player.getScheduler().run(plugin(), scheduledTask -> player.openInventory(getInventory()), null);
    }

    default void save() {
        Player cachedPlayer = getCachedPlayer();
        if (cachedPlayer instanceof CraftPlayer craftPlayer) {
            craftPlayer.saveData();
        }
    }

    default void update(Runnable runnable) {
        try {
            getLock().lock();
            runnable.run();
            if (isOffline()) {
                save();
            }
        } finally {
            if (getLock().isHeldByCurrentThread()) getLock().unlock();
        }
    }

    default boolean isOffline() {
        return !Bukkit.getServer().getOfflinePlayer(getUniqueIdOfObservedPlayer()).isOnline();
    }

    default Optional<Player> getPlayerOffline(OfflinePlayer offlinePlayer) {
        Player cached = getCachedPlayer();
        if (cached != null) {
            return Optional.of(cached);
        }

        GameProfile profile = new GameProfile(offlinePlayer.getUniqueId(),
                offlinePlayer.getName() != null ? offlinePlayer.getName() : offlinePlayer.getUniqueId().toString());
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) {
            plugin().getComponentLogger().error(text("Unable to find overworld level", NamedTextColor.RED));
            return Optional.empty();
        }

        ServerPlayer serverPlayer = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        CraftPlayer target = (CraftPlayer) serverPlayer.getBukkitEntity();
        target.loadData();
        cache(target);
        return Optional.of(target);
    }

    org.bukkit.plugin.Plugin plugin();

    UUID getUniqueIdOfObservedPlayer();

    void updateObservedInventory();

    void updateSubscriberInventory();

    Inventory getInventory();

    Set<UUID> getSubscribers();

    void removeSubscriber(UUID subscriber);

    boolean hasSubscriber(UUID subscriber);

    ReentrantLock getLock();

    void cache(Player player);

    Player getCachedPlayer();
}
