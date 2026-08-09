package de.jakomi1.project.state;

import de.jakomi1.listener.EventListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public final class StateJoinListener extends EventListener {

    private final StateManager manager;

    public StateJoinListener(StateManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!manager.allowsJoin()) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    manager.kickMessage()
            );
        }
    }
}
