package de.jakomi1.project.scoreboard;

import de.jakomi1.listener.EventListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class TabNumberListener extends EventListener {

    private final ScoreboardManager manager;

    public TabNumberListener(ScoreboardManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player viewer = event.getPlayer();
        manager.clearViewer(viewer);

        manager.server().scheduler().runGlobal(() -> {
            if (!viewer.isOnline()) return;

            manager.sendFullSync(viewer);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.clearViewer(event.getPlayer());
    }
}
