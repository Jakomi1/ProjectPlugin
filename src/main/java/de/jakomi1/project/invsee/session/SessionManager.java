/*
 * SessionManager – Port aus dem Plugin "Invsee".
 * Original: at.noahb.invsee (Autor: MCmitNoah), GNU General Public License v3.0 (GPL-3.0).
 * Siehe LICENSE.md in diesem Repository.
 *
 * Geändert von Jakomi1 (08.08.2026) für ProjectPlugin: an die Library-Architektur
 * (ProjectServer/Manager) angepasst, auf Paper 26.2 aktualisiert.
 */
package de.jakomi1.project.invsee.session;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Registry für aktive Invsee-Sessions.
 */
public abstract class SessionManager {

    private final Set<Session> sessions = new HashSet<>();

    private final Plugin plugin;

    public SessionManager(Plugin plugin) {
        this.plugin = plugin;
    }

    protected Plugin plugin() {
        return plugin;
    }

    public void addSubscriberToSession(OfflinePlayer player, UUID subscriber) {
        this.sessions.stream().filter(session -> session.getSubscribers().contains(subscriber))
                .forEach(session -> session.removeSubscriber(subscriber));

        this.sessions.stream()
                .filter(filterSession -> player.getUniqueId().equals(filterSession.getUniqueIdOfObservedPlayer()))
                .findFirst()
                .ifPresentOrElse(session -> session.addSubscriber(subscriber), () -> createSession(player, subscriber));
    }

    public void removeSubscriberFromSession(@NotNull HumanEntity subscriber) {
        Optional<? extends Session> first = this.sessions.stream()
                .filter(session -> session.getSubscribers().contains(subscriber.getUniqueId()))
                .findFirst();

        first.ifPresent(session -> {
            session.removeSubscriber(subscriber.getUniqueId());
            if (session.getSubscribers().isEmpty()) {
                session.save();
                this.sessions.remove(session);
            }
            subscriber.getScheduler().run(this.plugin,
                    scheduledTask -> subscriber.closeInventory(InventoryCloseEvent.Reason.PLUGIN), null);
        });
    }

    public void updateContent(Player player) {
        Optional<? extends Session> optionalSession = this.sessions.stream()
                .filter(session -> session.getUniqueIdOfObservedPlayer().equals(player.getUniqueId()))
                .findFirst();

        if (optionalSession.isPresent()) {
            optionalSession.get().updateSubscriberInventory();
            return;
        }

        optionalSession = this.sessions.stream()
                .filter(session -> session.hasSubscriber(player.getUniqueId()))
                .findFirst();

        optionalSession.ifPresent(Session::updateObservedInventory);
    }

    protected void addSession(Session session) {
        this.sessions.add(session);
    }

    public Optional<Session> getSessionForSubscriber(UUID subscriber) {
        return sessions.stream()
                .filter(session -> session.hasSubscriber(subscriber))
                .findFirst();
    }

    protected abstract Session createSession(OfflinePlayer offlinePlayer, UUID subscriber);
}
