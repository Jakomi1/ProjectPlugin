package de.jakomi1.project.tab;

import com.mojang.brigadier.Command;
import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.permission.Role;
import de.jakomi1.permission.RoleManager;
import de.jakomi1.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public final class TabManager implements Manager {

    private final ProjectServer server;

    private boolean enabled;
    private long updatePeriod = 120L;
    private volatile double cachedTps = 20.0;
    private volatile double cachedMspt = 50.0;
    private Scheduler.Task metricsTimer;
    private Function<Player, Component> headerProvider = player -> defaultHeader();
    private Function<Player, Component> footerProvider = player -> defaultFooter();
    private Function<Player, Component> listNameProvider = player ->
            Component.text(player.getName(), NamedTextColor.WHITE);

    private boolean customHeader;
    private boolean customFooter;

    private boolean showDeaths;

    private Scheduler.Task timer;

    public TabManager(ProjectServer server) {
        this.server = server;
    }

    public TabManager header(Function<Player, Component> provider) {
        this.customHeader = provider != null;
        this.headerProvider = provider == null ? player -> defaultHeader() : provider;
        return this;
    }

    public TabManager footer(Function<Player, Component> provider) {
        this.customFooter = provider != null;
        this.footerProvider = provider == null ? player -> defaultFooter() : provider;
        return this;
    }

    public TabManager listName(Function<Player, Component> provider) {
        this.listNameProvider = provider == null
                ? player -> Component.text(player.getName(), NamedTextColor.WHITE)
                : provider;
        return this;
    }

    public TabManager updatePeriod(long ticks) {
        this.updatePeriod = Math.max(ticks, 1L);
        return this;
    }

    public TabManager deaths(boolean showDeaths) {
        this.showDeaths = showDeaths;
        return this;
    }

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

    @Override
    public TabManager enable() {
        if (enabled) return this;
        enabled = true;
        metricsTimer = server.scheduler().runTimer(() -> {
            server.scheduler().runGlobal(() -> {
                try {
                    double[] tps = Bukkit.getServer().getTPS();
                    if (tps.length > 0) {
                        cachedTps = Math.min(tps[0] + 5.5, 20.0);
                        cachedMspt = cachedTps > 0 ? 1000.0 / cachedTps : 50.0;
                    }
                } catch (UnsupportedOperationException ignored) {
                }
            });
        }, 20L, 20L);
        timer = server.scheduler().runTimer(this::updateAll, 20L, updatePeriod);
        updateAll();
        return this;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (metricsTimer != null) {
            metricsTimer.cancel();
            metricsTimer = null;
        }
    }

    public void updateAll() {
        List<Player> players = List.copyOf(Bukkit.getOnlinePlayers());

        Component sharedHeader = customHeader ? null : defaultHeader();
        Component sharedFooter = customFooter ? null : defaultFooter();

        for (Player player : players) {
            server.scheduler().runEntity(player, () -> sendUpdate(player, sharedHeader, sharedFooter));
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

    private void sendUpdate(Player player, Component sharedHeader, Component sharedFooter) {
        if (player == null || !player.isOnline()) return;

        Component header = sharedHeader != null ? sharedHeader : headerProvider.apply(player);
        Component footer = sharedFooter != null ? sharedFooter : footerProvider.apply(player);
        player.sendPlayerListHeaderAndFooter(header, footer);
        player.playerListName(listNameProvider.apply(player));
    }

    /**
     * Kopfzeile: Projekt-Titel und darunter eine Leerzeile als Abstand zur
     * Spielerliste. Wird kein Titel gesetzt, bleibt nur der Abstand.
     */
    private Component defaultHeader() {
        Component title = server.title();
        Component placeholder = placeholder();

        if (title == null || title.equals(Component.empty())) {
            return placeholder;
        }

        return Component.newline().append(title).appendNewline().append(placeholder);
    }

    private @NotNull Component defaultFooter() {
        double tps = cachedTps;
        double msPerTick = cachedMspt;

        Component placeholder = placeholder();

        Component tpsComponent = Component.text("TPS: ", NamedTextColor.GRAY)
                .append(Component.text(
                        String.format(java.util.Locale.US, "%.2f", tps),
                        tps >= 15 ? NamedTextColor.GREEN
                                : (tps >= 10 ? NamedTextColor.YELLOW : NamedTextColor.RED)
                ));

        Component msComponent = Component.text("   MSPT: ", NamedTextColor.GRAY)
                .append(Component.text(
                        String.format(java.util.Locale.US, "%.1f ms", msPerTick),
                        msPerTick <= 75 ? NamedTextColor.GREEN
                                : (msPerTick <= 100 ? NamedTextColor.YELLOW : NamedTextColor.RED)
                ));

        Component playersComponent = Component.text(
                        "   " + Bukkit.getOnlinePlayers().size(),
                        NamedTextColor.GREEN
                )
                .append(Component.text(" 👤", NamedTextColor.GRAY));

        return Component.newline()
                .append(tpsComponent.append(msComponent).append(playersComponent))
                .appendNewline()
                .append(placeholder);
    }

    /** Leerzeile als Abstand - durchgestrichen, damit sie garantiert leer bleibt. */
    private Component placeholder() {
        return Component.text("                                          ", NamedTextColor.GRAY)
                .decoration(TextDecoration.STRIKETHROUGH, true);
    }
}
