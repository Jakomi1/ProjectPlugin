package de.jakomi1.project.join;

import de.jakomi1.listener.EventListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Reicht die Join-/Quit-Ereignisse an den {@link JoinManager} weiter.
 *
 * <p>Der Listener enthaelt bewusst keine eigene Logik: Alles laeuft ueber
 * den Manager, damit ein Projekt nur konfigurieren muss und die Reihenfolge
 * an genau einer Stelle festgelegt ist.
 */
public final class JoinListener extends EventListener {

    private final JoinManager manager;

    public JoinListener(JoinManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        manager.handleJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        manager.handleQuit(event.getPlayer());
    }
}
