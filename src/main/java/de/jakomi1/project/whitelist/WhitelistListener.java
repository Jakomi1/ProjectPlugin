package de.jakomi1.project.whitelist;

import de.jakomi1.listener.EventListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public final class WhitelistListener extends EventListener {

    private final WhitelistManager manager;

    public WhitelistListener(WhitelistManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (manager.bypass(event.getUniqueId())) return;

        if (manager.table().isWhitelisted(event.getUniqueId(), event.getName())) return;

        event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                manager.kickMessage()
        );
    }
}
