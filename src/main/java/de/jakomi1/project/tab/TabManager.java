package de.jakomi1.project.tab;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.project.permission.Role;
import de.jakomi1.project.permission.RoleManager;
import de.jakomi1.project.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Verwaltet die Tab-Liste: Header/Footer (Titel + TPS/MSPT) und den
 * Playerlist-Namen pro Spieler. Der ListName kann optional den
 * Scoreboard-Wert (Todeszahl) hinter dem Namen anzeigen, wie im
 * CrackedAttack-Tab-Fix.
 */
public final class TabManager {

    private final ProjectServer server;

    private boolean enabled;
    private long updatePeriod = 120L;

    private Function<Player, Component> headerProvider = player -> defaultHeader();
    private Function<Player, Component> footerProvider = player -> defaultFooter();
    private Function<Player, Component> listNameProvider = player ->
            Component.text(player.getName(), NamedTextColor.WHITE);

    private boolean showDeaths;

    private Scheduler.Task timer;

    public TabManager(ProjectServer server) {
        this.server = server;
    }

    public TabManager header(Function<Player, Component> provider) {
        this.headerProvider = provider == null ? player -> defaultHeader() : provider;
        return this;
    }

    public TabManager footer(Function<Player, Component> provider) {
        this.footerProvider = provider == null ? player -> defaultFooter() : provider;
        return this;
    }

    public TabManager listName(Function<Player, Component> provider) {
        this.listNameProvider = provider == null
                ? player -> Component.text(player.getName(), NamedTextColor.WHITE)
                : provider;
        return this;
    }

    /**
     * Wie oft Header/Footer aktualisiert werden (in Ticks, Standard 120).
     */
    public TabManager updatePeriod(long ticks) {
        this.updatePeriod = Math.max(ticks, 1L);
        return this;
    }

    /**
     * Zeigt hinter dem Namen die Todeszahl in der Tab-Liste an
     * (CrackedAttack-Scoreboard-Fix).
     */
    public TabManager deaths(boolean showDeaths) {
        this.showDeaths = showDeaths;
        return this;
    }

    /**
     * Verbindet das System mit dem Rollen-System: ListName wird zu
     * {@code [Rollen-Display] Name [Tode]}. Standard-Rolle ({@link Role#MEMBER})
     * zeigt nur den Namen.
     */
    public TabManager roles(RoleManager roles) {
        this.listNameProvider = player -> {
            Role role = roles.roleOf(player.getUniqueId());
            Component name = Component.text(player.getName(), NamedTextColor.WHITE);

            if (role != null && !role.isMember()) {
                name = NametagManager.rolePrefix(role).append(name);
            }

            if (showDeaths) {
                name = name.append(Component.text(" [", NamedTextColor.GRAY))
                        .append(Component.text(player.getStatistic(Statistic.DEATHS), NamedTextColor.YELLOW))
                        .append(Component.text("]", NamedTextColor.GRAY));
            }

            return name;
        };
        return this;
    }

    public TabManager enable() {
        if (enabled) return this;
        enabled = true;

        timer = server.scheduler().runTimer(this::updateAll, 20L, updatePeriod);
        updateAll();
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
    }

    public void update(Player player) {
        if (player == null || !player.isOnline()) return;

        player.sendPlayerListHeaderAndFooter(
                headerProvider.apply(player),
                footerProvider.apply(player)
        );
        player.playerListName(listNameProvider.apply(player));
    }

    private Component defaultHeader() {
        Component title = server.title();

        if (title == null || title.equals(Component.empty())) {
            return Component.text("                                          ", NamedTextColor.GRAY);
        }

        return Component.text("                                          ", NamedTextColor.GRAY)
                .appendNewline()
                .append(title)
                .appendNewline();
    }

    private @NotNull Component defaultFooter() {
        double tps = Math.min(Bukkit.getServer().getTPS()[0] + 0.5, 20);
        double msPerTick = tps > 0 ? 1000.0 / tps : 50;

        Component placeholder = Component.text("                                          ", NamedTextColor.GRAY);

        Component tpsComponent = Component.text("TPS: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.2f", tps).replace(",", "."),
                        tps >= 15 ? NamedTextColor.GREEN : (tps >= 10 ? NamedTextColor.YELLOW : NamedTextColor.RED)));

        Component msComponent = Component.text("  |  MSPT: ", NamedTextColor.GRAY)
                .append(Component.text(String.format("%.1f ms", msPerTick).replace(",", "."),
                        msPerTick <= 75 ? NamedTextColor.GREEN : (msPerTick <= 100 ? NamedTextColor.YELLOW : NamedTextColor.RED)));

        return Component.newline()
                .append(tpsComponent.append(msComponent))
                .appendNewline()
                .append(placeholder);
    }
}
