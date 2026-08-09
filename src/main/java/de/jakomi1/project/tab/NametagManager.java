package de.jakomi1.project.tab;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.project.nms.NmsBridge;
import de.jakomi1.permission.Role;
import de.jakomi1.permission.RoleManager;
import de.jakomi1.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public final class NametagManager implements Manager {

    private final ProjectServer server;
    private final NmsBridge bridge;

    private boolean enabled;
    private long syncPeriod = 40L;

    private Function<Player, String> teamResolver = player -> null;
    private Function<Player, Component> prefixResolver = player -> Component.empty();
    private Function<Player, Component> suffixResolver = player -> Component.empty();

    private final Map<UUID, Map<String, KnownTeam>> known = new HashMap<>();
    private Scheduler.Task timer;

    public NametagManager(ProjectServer server) {
        this.server = server;
        this.bridge = new NmsBridge(server.plugin().getLogger());
    }

    public NametagManager team(Function<Player, String> resolver) {
        this.teamResolver = resolver == null ? player -> null : resolver;
        return this;
    }

    public NametagManager prefix(Function<Player, Component> resolver) {
        this.prefixResolver = resolver == null ? player -> Component.empty() : resolver;
        return this;
    }

    public NametagManager suffix(Function<Player, Component> resolver) {
        this.suffixResolver = resolver == null ? player -> Component.empty() : resolver;
        return this;
    }

    public NametagManager syncPeriod(long ticks) {
        this.syncPeriod = Math.max(ticks, 1L);
        return this;
    }

    public NametagManager roles(RoleManager roles) {
        this.teamResolver = player -> {
            Role role = roles.roleOf(player.getUniqueId());
            return role.isMember() ? null : role.name();
        };
        this.prefixResolver = player -> {
            Role role = roles.roleOf(player.getUniqueId());
            return role.isMember() ? Component.empty() : rolePrefix(role);
        };
        return this;
    }

    @Override
    public NametagManager enable() {
        if (enabled) return this;
        enabled = true;

        bridge.init();
        timer = server.scheduler().runTimer(this::syncAll, 20L, syncPeriod);
        requestSync();
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
        known.clear();
    }

    public void requestSync() {
        server.scheduler().runGlobal(this::syncAll);
    }

    private void syncAll() {
        if (!bridge.isReady()) return;
        if (server.plugin() == null || !server.plugin().isEnabled()) return;

        List<Player> players = List.copyOf(Bukkit.getOnlinePlayers());

        Map<String, DesiredTeam> desired = new LinkedHashMap<>();

        for (Player player : players) {
            String teamName = teamResolver.apply(player);
            if (teamName == null || teamName.isBlank()) continue;

            DesiredTeam team = desired.computeIfAbsent(
                    teamName,
                    name -> new DesiredTeam(name, prefixResolver.apply(player), suffixResolver.apply(player))
            );
            team.members.add(player.getName());
        }

        Set<UUID> online = new HashSet<>();
        for (Player player : players) {
            online.add(player.getUniqueId());
        }
        known.keySet().removeIf(uuid -> !online.contains(uuid));

        for (Player viewer : players) {
            Map<String, KnownTeam> viewerKnown = known.computeIfAbsent(
                    viewer.getUniqueId(),
                    uuid -> new HashMap<>()
            );

            for (DesiredTeam desiredTeam : desired.values()) {
                KnownTeam current = viewerKnown.get(desiredTeam.name);

                if (current == null) {
                    Object scoreboard = bridge.newScoreboard();
                    Object team = bridge.newTeam(scoreboard, desiredTeam.name);
                    if (team == null) continue;

                    bridge.configureTeam(team, desiredTeam.prefix, desiredTeam.suffix);

                    for (String member : desiredTeam.members) {
                        bridge.addPlayerToTeam(scoreboard, team, member);
                    }

                    current = new KnownTeam(
                            desiredTeam.name,
                            team,
                            desiredTeam.prefix,
                            desiredTeam.suffix
                    );
                    current.members.addAll(desiredTeam.members);
                    viewerKnown.put(desiredTeam.name, current);

                    send(viewer, bridge.addOrModifyPacket(team, true));
                } else {
                    boolean styleChanged = !desiredTeam.prefix.equals(current.prefix)
                            || !desiredTeam.suffix.equals(current.suffix);

                    if (styleChanged) {
                        bridge.configureTeam(current.team, desiredTeam.prefix, desiredTeam.suffix);
                        current.prefix = desiredTeam.prefix;
                        current.suffix = desiredTeam.suffix;
                        send(viewer, bridge.addOrModifyPacket(current.team, false));
                    }

                    Set<String> additions = new HashSet<>(desiredTeam.members);
                    additions.removeAll(current.members);

                    Set<String> removals = new HashSet<>(current.members);
                    removals.removeAll(desiredTeam.members);

                    if (!removals.isEmpty()) {
                        send(viewer, bridge.multiplePlayerPacket(current.team, removals, false));
                    }

                    if (!additions.isEmpty()) {
                        send(viewer, bridge.multiplePlayerPacket(current.team, additions, true));
                    }

                    current.members.clear();
                    current.members.addAll(desiredTeam.members);
                }
            }

            Iterator<Map.Entry<String, KnownTeam>> iterator = viewerKnown.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, KnownTeam> entry = iterator.next();

                if (!desired.containsKey(entry.getKey())) {
                    send(viewer, bridge.removePacket(entry.getValue().team));
                    iterator.remove();
                }
            }
        }
    }

    private void send(Player viewer, Object packet) {
        if (packet == null) return;

        server.scheduler().runEntity(viewer, () -> bridge.send(viewer, packet));
    }

    static Component rolePrefix(Role role) {
        return Component.text("[", NamedTextColor.GRAY)
                .append(role.coloredDisplay())
                .append(Component.text("] ", NamedTextColor.GRAY));
    }

    private static final class KnownTeam {
        private final String name;
        private final Object team;
        private final Set<String> members = new HashSet<>();
        private Component prefix;
        private Component suffix;

        private KnownTeam(String name, Object team, Component prefix, Component suffix) {
            this.name = name;
            this.team = team;
            this.prefix = prefix;
            this.suffix = suffix;
        }
    }

    private static final class DesiredTeam {
        private final String name;
        private final Component prefix;
        private final Component suffix;
        private final Set<String> members = new HashSet<>();

        private DesiredTeam(String name, Component prefix, Component suffix) {
            this.name = name;
            this.prefix = prefix;
            this.suffix = suffix;
        }
    }
}
