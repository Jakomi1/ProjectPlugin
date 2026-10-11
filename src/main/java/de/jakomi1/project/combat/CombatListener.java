package de.jakomi1.project.combat;

import de.jakomi1.listener.EventListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Haelt den Kampf-Tag des {@link CombatManager} aktuell.
 *
 * <p>Das eigentliche Markieren geschieht in {@link PvpListener}, sobald ein
 * Treffer durch die PvP-Regeln erlaubt wurde. Hier bleibt nur das Auffrischen
 * nach einem Join und die Kampf-Log-Behandlung beim Verlassen.
 */
public final class CombatListener extends EventListener {

    private final CombatManager combat;

    public CombatListener(CombatManager combat) {
        this.combat = combat;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        combat.refresh(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        combat.handleQuit(event.getPlayer());
    }
}
