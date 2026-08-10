package de.jakomi1.project.bossbar;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pro-Spieler BossBar-Verwaltung auf Basis der Adventure-BossBar-API.
 */
public final class BossBarManager implements Manager {

    private final ProjectServer server;
    private final BossBarListener listener;

    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();

    private boolean enabled;

    public BossBarManager(ProjectServer server) {
        this.server = server;
        this.listener = new BossBarListener(this);
    }

    @Override
    public BossBarManager enable() {
        if (enabled) return this;
        enabled = true;

        listener.register(server.plugin());
        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;

        listener.unregister();
        clearAll();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public ProjectServer server() {
        return server;
    }

    public BossBar bossBar(Player player) {
        if (player == null) return null;
        return bossBars.get(player.getUniqueId());
    }

    public boolean isShowing(Player player) {
        return player != null && bossBars.containsKey(player.getUniqueId());
    }

    public BossBar show(Player player, BossBar bossBar) {
        if (player == null || bossBar == null) return bossBar;

        hide(player);
        player.showBossBar(bossBar);
        bossBars.put(player.getUniqueId(), bossBar);
        return bossBar;
    }

    public BossBar show(Player player, Component name) {
        return show(player, name, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
    }

    public BossBar show(Player player, Component name, BossBar.Color color, BossBar.Overlay overlay) {
        return show(player, BossBar.bossBar(name, 1.0f, color, overlay));
    }

    public BossBarManager hide(Player player) {
        if (player == null) return this;

        BossBar bossBar = bossBars.remove(player.getUniqueId());
        if (bossBar != null && player.isOnline()) {
            player.hideBossBar(bossBar);
        }
        return this;
    }

    /**
     * Blendet eine BossBar nach dem Re-Join wieder ein.
     */
    public BossBarManager sync(Player player) {
        if (player == null || !enabled || !player.isOnline()) return this;

        BossBar bossBar = bossBars.get(player.getUniqueId());
        if (bossBar != null) {
            player.showBossBar(bossBar);
        }
        return this;
    }

    public BossBarManager clearAll() {
        for (UUID uuid : bossBars.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                hide(player);
            }
        }
        bossBars.clear();
        return this;
    }

    public Set<UUID> viewers() {
        return Set.copyOf(bossBars.keySet());
    }

    public Map<UUID, BossBar> bossBars() {
        return Map.copyOf(bossBars);
    }
}
