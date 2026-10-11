package de.jakomi1.project.advancement;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import de.jakomi1.listener.EventListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

/**
 * Bricht das Kriterium-Grant-Event ab, solange der Spieler laut
 * {@link AdvancementManager} keine Errungenschaften erhalten soll.
 */
public final class AdvancementListener extends EventListener {

    private final AdvancementManager manager;

    public AdvancementListener(AdvancementManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAdvancement(PlayerAdvancementCriterionGrantEvent event) {
        if (manager.shouldBlock(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}