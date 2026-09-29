package de.jakomi1.project.playtime;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.permission.Role;

import java.util.List;

/**
 * Spielzeit-Verwaltung.
 *
 * <p>Ein {@code enable()} reicht vollstaendig aus: Tabelle, laufende
 * Speicherung, Top-Snapshot und die beiden Befehle werden dabei zusammen
 * eingerichtet. Ein {@link PlaytimeStore#disable()} von Hand ist nicht
 * noetig, das uebernimmt {@link #disable()}.
 */
public final class PlaytimeManager implements Manager {

    private final ProjectServer server;
    private final PlaytimeTable table;
    private final PlaytimeCommand playtimeCommand;
    private final TopTenCommand topTenCommand;

    private boolean enabled;
    private boolean tableRegistered;
    private boolean registerCommand = true;
    private Role minimumRole = Role.MODERATOR;

    public PlaytimeManager(ProjectServer server) {
        this.server = server;
        this.table = new PlaytimeTable();
        this.playtimeCommand = new PlaytimeCommand(this);
        this.topTenCommand = new TopTenCommand(this);
    }

    @Override
    public PlaytimeManager enable() {
        if (enabled) return this;
        enabled = true;

        playtimeTable();

        if (registerCommand) {
            playtimeCommand.register(server.plugin());
            topTenCommand.register(server.plugin());
        }

        PlaytimeStore.enable(server, playtimeTable());

        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;

        PlaytimeStore.disable();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** Merkt sich einen Spieler, damit seine Zeit auch offline stimmt. */
    public PlaytimeManager track(org.bukkit.entity.Player player) {
        PlaytimeStore.track(player);
        return this;
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

    public int playtimeTicks(org.bukkit.OfflinePlayer player) {
        if (player == null) return 0;

        try {
            return player.getStatistic(org.bukkit.Statistic.TOTAL_WORLD_TIME);
        } catch (Exception ignored) {
            return 0;
        }
    }

    public long playtime(org.bukkit.OfflinePlayer player) {
        return playtimeTicks(player) / 20L;
    }

    public boolean canViewPlaytime(org.bukkit.OfflinePlayer player) {
        return player != null && player.isOnline()
                && server.permissions().roleOf(player.getUniqueId()).inherits(minimumRole);
    }

    public List<PlaytimeTable.PlaytimeEntry> top(int limit) {
        return PlaytimeStore.top(limit);
    }

    public List<String> knownNames() {
        return PlaytimeStore.knownNames();
    }

    public static String format(long seconds) {
        return PlaytimeStore.format(seconds);
    }

    public PlaytimeTable table() {
        return playtimeTable();
    }

    public ProjectServer server() {
        return server;
    }

    private PlaytimeTable playtimeTable() {
        if (!tableRegistered) {
            table.register(server.plugin());
            tableRegistered = true;
        }
        return table;
    }
}
