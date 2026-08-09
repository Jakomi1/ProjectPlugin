package de.jakomi1.project.team;

import de.jakomi1.project.listener.EventListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

public final class TeamListener extends EventListener {

    private final TeamManager manager;

    public TeamListener(TeamManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.server().scheduler().runGlobal(() -> manager.sync(event.getPlayer()));
    }
}
