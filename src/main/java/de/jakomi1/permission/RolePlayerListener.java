package de.jakomi1.permission;

import de.jakomi1.listener.EventListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class RolePlayerListener extends EventListener {

    private final RoleManager manager;

    public RolePlayerListener(RoleManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.recordName(event.getPlayer());
        manager.apply(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.cleanup(event.getPlayer());
    }
}
