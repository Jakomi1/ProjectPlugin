package de.jakomi1.world;

import de.jakomi1.listener.EventListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class WorldPerformanceListener extends EventListener {

    private final WorldPerformance performance;

    public WorldPerformanceListener(WorldPerformance performance) {
        this.performance = performance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        performance.updateDistances();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        performance.scheduleUpdate();
    }
}
