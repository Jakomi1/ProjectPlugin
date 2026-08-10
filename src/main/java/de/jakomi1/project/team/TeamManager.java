package de.jakomi1.project.team;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.project.nms.NmsBridge;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class TeamManager implements Manager {

    private final ProjectServer server;
    private final NmsBridge bridge;
    private final TeamListener listener;

    private final Map<String, Team> teams = new LinkedHashMap<>();

    private boolean enabled;

    public TeamManager(ProjectServer server) {
        this.server = server;
        this.bridge = new NmsBridge(server.plugin().getLogger());
        this.listener = new TeamListener(this);
    }

    public NmsBridge bridge() {
        return bridge;
    }

    @Override
    public TeamManager enable() {
        if (enabled) return this;
        enabled = true;

        bridge.init();
        listener.register(server.plugin());
        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;

        listener.unregister();
        removeAll();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Team team(String name) {
        if (name == null || name.isBlank()) return null;

        Team existing = teams.get(name);
        if (existing != null) return existing;

        return create(name);
    }

    public Team create(String name) {
        if (name == null || name.isBlank()) return null;

        Team existing = teams.get(name);
        if (existing != null) return existing;

        Object scoreboard = bridge.newScoreboard();
        Object handle = bridge.newTeam(scoreboard, name);
        if (handle == null) return null;

        Team team = new Team(this, name, scoreboard, handle);
        teams.put(name, team);
        team.apply();
        return team;
    }

    public boolean exists(String name) {
        return name != null && teams.containsKey(name);
    }

    public TeamManager remove(String name) {
        if (name == null) return this;

        Team team = teams.remove(name);
        if (team != null) {
            team.delete();
        }
        return this;
    }

    public TeamManager removeAll() {
        for (Team team : new ArrayList<>(teams.values())) {
            team.delete();
        }
        teams.clear();
        return this;
    }

    public Collection<Team> teams() {
        return new ArrayList<>(teams.values());
    }

    public int size() {
        return teams.size();
    }

    public void broadcast(Object packet) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            bridge.send(viewer, packet);
        }
    }

    public void sync(Player viewer) {
        if (viewer == null || !viewer.isOnline()) return;

        for (Team team : teams.values()) {
            bridge.send(viewer, team.packet(true));
        }
    }

    public ProjectServer server() {
        return server;
    }

    public static final class Team {

        private final TeamManager manager;
        private final String name;
        private final Object scoreboard;
        private final Object handle;
        private final Set<String> members = new LinkedHashSet<>();

        private Component displayName = Component.empty();
        private Component prefix = Component.empty();
        private Component suffix = Component.empty();
        private Object color;
        private Object visibility;
        private Object collisionRule;
        private boolean friendlyFire;
        private boolean seeFriendlyInvisibles;

        private Team(TeamManager manager, String name, Object scoreboard, Object handle) {
            this.manager = manager;
            this.name = name;
            this.scoreboard = scoreboard;
            this.handle = handle;
            this.color = manager.bridge.colorWhite();
            this.visibility = manager.bridge.visibilityAlways();
            this.collisionRule = manager.bridge.collisionRuleNever();
        }

        public String name() {
            return name;
        }

        public Object handle() {
            return handle;
        }

        public Set<String> members() {
            return Set.copyOf(members);
        }

        public Team displayName(Component displayName) {
            if (displayName != null) {
                this.displayName = displayName;
            }
            return this;
        }

        public Team prefix(Component prefix) {
            if (prefix != null) {
                this.prefix = prefix;
            }
            return this;
        }

        public Team suffix(Component suffix) {
            if (suffix != null) {
                this.suffix = suffix;
            }
            return this;
        }

        public Team color(String color) {
            this.color = manager.bridge.color(color);
            return this;
        }

        public Team visibility(String visibility) {
            this.visibility = manager.bridge.visibility(visibility);
            return this;
        }

        public Team collision(String collisionRule) {
            this.collisionRule = manager.bridge.collisionRule(collisionRule);
            return this;
        }

        public Team friendlyFire(boolean friendlyFire) {
            this.friendlyFire = friendlyFire;
            return this;
        }

        public Team seeFriendlyInvisibles(boolean seeFriendlyInvisibles) {
            this.seeFriendlyInvisibles = seeFriendlyInvisibles;
            return this;
        }

        public Team add(Player player) {
            if (player == null) return this;

            return add(player.getName());
        }

        public Team add(String playerName) {
            if (playerName == null || playerName.isBlank()) return this;

            if (members.add(playerName)) {
                manager.bridge.addPlayerToTeam(scoreboard, handle, playerName);
                manager.broadcast(manager.bridge.playerPacket(handle, playerName, true));
            }
            return this;
        }

        public Team addAll(Collection<Player> players) {
            if (players == null) return this;

            for (Player player : players) {
                add(player);
            }
            return this;
        }

        public Team remove(Player player) {
            if (player == null) return this;

            return remove(player.getName());
        }

        public Team remove(String playerName) {
            if (playerName == null || playerName.isBlank()) return this;

            if (members.remove(playerName)) {
                manager.broadcast(manager.bridge.playerPacket(handle, playerName, false));
            }
            return this;
        }

        public Team apply() {
            NmsBridge bridge = manager.bridge;

            bridge.setTeamDisplayName(handle, displayName);
            bridge.setTeamPrefix(handle, prefix);
            bridge.setTeamSuffix(handle, suffix);
            bridge.setColor(handle, color);
            bridge.setNameTagVisibility(handle, visibility);
            bridge.setCollisionRule(handle, collisionRule);
            bridge.setAllowFriendlyFire(handle, friendlyFire);
            bridge.setSeeFriendlyInvisibles(handle, seeFriendlyInvisibles);

            manager.broadcast(bridge.addOrModifyPacket(handle, false));
            return this;
        }

        public Team applyWithMembers() {
            apply();
            manager.broadcast(manager.bridge.addOrModifyPacket(handle, true));
            return this;
        }

        public void delete() {
            manager.broadcast(manager.bridge.removePacket(handle));
        }

        private Object packet(boolean withPlayers) {
            return manager.bridge.addOrModifyPacket(handle, withPlayers);
        }
    }
}
