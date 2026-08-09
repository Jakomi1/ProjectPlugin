package de.jakomi1.project.bossbar;

import de.jakomi1.listener.EventListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class BossBarListener extends EventListener {

    private final BossBarManager manager;

    public BossBarListener(BossBarManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.server().scheduler().runGlobal(() -> {
            if (event.getPlayer().isOnline()) {
                manager.sync(event.getPlayer());
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.hide(event.getPlayer());
    }
}
