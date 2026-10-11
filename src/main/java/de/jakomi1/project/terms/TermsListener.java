package de.jakomi1.project.terms;

import de.jakomi1.listener.EventListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Vermerkt die Zustimmung zu den Nutzungsbedingungen beim ersten Join und
 * schickt die vom {@link TermsManager} konfigurierten Texte.
 */
public final class TermsListener extends EventListener {

    private final TermsManager manager;

    public TermsListener(TermsManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.handleJoin(event.getPlayer());
    }
}
