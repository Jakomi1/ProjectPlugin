package de.jakomi1.project.playtime;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.permission.Role;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class PlaytimeManager implements Manager {

    private final ProjectServer server;
    private final PlaytimeCommand playtimeCommand;
    private final TopTenCommand topTenCommand;

    private boolean enabled;
    private boolean registerCommand = true;
    private Role minimumRole = Role.MODERATOR;

    public PlaytimeManager(ProjectServer server) {
        this.server = server;
        this.playtimeCommand = new PlaytimeCommand(this);
        this.topTenCommand = new TopTenCommand(this);
    }

    @Override
    public PlaytimeManager enable() {
        if (enabled) return this;
        enabled = true;

        if (registerCommand) {
            playtimeCommand.register(server.plugin());
            topTenCommand.register(server.plugin());
        }
        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public PlaytimeManager command(boolean registerCommand) {
        this.registerCommand = registerCommand;
        return this;
    }

    public boolean command() {
        return registerCommand;
    }

    public PlaytimeManager minimumRole(Role role) {
        this.minimumRole = role != null ? role : Role.MODERATOR;
        return this;
    }

    public Role minimumRole() {
        return minimumRole;
    }

    public int playtimeTicks(OfflinePlayer player) {
        if (player == null) return 0;

        try {
            return player.getStatistic(Statistic.TOTAL_WORLD_TIME);
        } catch (Exception ignored) {
            return 0;
        }
    }

    public long playtime(OfflinePlayer player) {
        return playtimeTicks(player) / 20L;
    }

    public boolean canViewPlaytime(OfflinePlayer player) {
        return player != null && player.isOnline()
                && server.permissions().roleOf(player.getUniqueId()).inherits(minimumRole);
    }

    public List<PlaytimeEntry> top(int limit) {
        int max = Math.max(1, limit);

        return Arrays.stream(Bukkit.getOfflinePlayers())
                .map(player -> new PlaytimeEntry(
                        player.getName() != null ? player.getName() : "Unbekannt",
                        playtime(player)
                ))
                .filter(entry -> entry.seconds() > 0)
                .sorted(Comparator.comparingLong(PlaytimeEntry::seconds).reversed())
                .limit(max)
                .toList();
    }

    public static String format(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;

        return days > 0
                ? "%dd %dh %02dmin".formatted(days, hours, minutes)
                : "%dh %02dmin".formatted(hours, minutes);
    }

    public ProjectServer server() {
        return server;
    }

    public record PlaytimeEntry(String name, long seconds) {
    }
}
