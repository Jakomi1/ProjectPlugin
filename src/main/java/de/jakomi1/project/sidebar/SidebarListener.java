package de.jakomi1.project.sidebar;

import de.jakomi1.listener.EventListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class SidebarListener extends EventListener {

    private final SidebarManager manager;

    public SidebarListener(SidebarManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player viewer = event.getPlayer();
        manager.clearViewer(viewer);

        manager.server().scheduler().runGlobal(() -> {
            if (!viewer.isOnline()) return;

            manager.sync(viewer);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.clearViewer(event.getPlayer());
    }
}
