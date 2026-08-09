package de.jakomi1.project.state;

import de.jakomi1.database.table.GlobalSettingsTable;
import de.jakomi1.project.ProjectServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class StateManager {

    private final ProjectServer server;
    private final GlobalSettingsTable settingsTable;
    private final Map<ServerState, StateSettings> settings = new EnumMap<>(ServerState.class);
    private final StateJoinListener joinListener;
    private final StateRestrictionListener restrictionListener;
    private final StateScheduler scheduler;

    public StateManager(ProjectServer server, GlobalSettingsTable settingsTable) {
        this.server = server;
        this.settingsTable = settingsTable;
        this.joinListener = new StateJoinListener(this);
        joinListener.register(server.plugin());
        this.restrictionListener = new StateRestrictionListener(this);
        restrictionListener.register(server.plugin());
        this.scheduler = new StateScheduler(server);

        for (ServerState state : ServerState.values()) {
            settings.put(state, StateSettings.defaults(state));
        }

        refresh();
    }

    public StateManager settings(ServerState state, StateSettings stateSettings) {
        settings.put(state, stateSettings);
        if (state == currentState()) refresh();
        return this;
    }

    public StateSettings settings(ServerState state) {
        return settings.getOrDefault(state, StateSettings.defaults(state));
    }

    public ServerState currentState() {
        return settingsTable.getServerState();
    }

    public StateManager set(ServerState state) {
        settingsTable.setServerState(state);
        refresh();
        return this;
    }

    public StateManager advance() {
        settingsTable.advanceServerState();
        refresh();
        return this;
    }

    public StateManager border(ServerState state, BorderSettings border) {
        StateSettings current = settings(state);
        StateSettings updated = StateSettings.builder()
                .from(current)
                .border(border)
                .build();

        settings.put(state, updated);
        if (state == currentState()) refresh();
        return this;
    }

    public StateManager movement(ServerState state, StateRule rule) {
        return rules(state, rule, settings(state).damage(), settings(state).blocks());
    }

    public StateManager damage(ServerState state, StateRule rule) {
        return rules(state, settings(state).movement(), rule, settings(state).blocks());
    }

    public StateManager blocks(ServerState state, StateRule rule) {
        return rules(state, settings(state).movement(), settings(state).damage(), rule);
    }

    public StateManager rules(ServerState state, StateRule movement, StateRule damage, StateRule blocks) {
        StateSettings current = settings(state);
        StateSettings updated = StateSettings.builder()
                .from(current)
                .movement(movement)
                .damage(damage)
                .blocks(blocks)
                .build();

        settings.put(state, updated);
        if (state == currentState()) refresh();
        return this;
    }

    public boolean allowsMovement(Player player) {
        return player != null && settings(currentState()).movement().allows(server.permissions(), player.getUniqueId());
    }

    public boolean allowsDamage(Player player) {
        return player != null && settings(currentState()).damage().allows(server.permissions(), player.getUniqueId());
    }

    public boolean allowsBreaking(Player player) {
        return player != null && settings(currentState()).blocks().allows(server.permissions(), player.getUniqueId());
    }

    public StateManager schedule(StateSchedule schedule) {
        scheduler.schedule(schedule).start();
        return this;
    }

    public StateScheduler scheduler() {
        return scheduler;
    }

    public boolean allowsJoin() {
        return allowsJoin(null);
    }

    public boolean allowsJoin(UUID uuid) {
        return settings(currentState()).join().allows(server.permissions(), uuid);
    }

    public Component kickMessage() {
        return settings(currentState()).kickMessage();
    }

    public void refresh() {
        StateSettings stateSettings = settings(currentState());

        Component mainMotd = stateSettings.motd() != null
                ? stateSettings.motd()
                : server.title();

        server.serverPing().setMotd(mainMotd, stateSettings.subMotd());
        server.serverPing().hideOnlinePlayers(stateSettings.hidePlayers());
        applyBorder(stateSettings.border());
    }

    private void applyBorder(BorderSettings border) {
        if (border == null) return;
        for (World world : Bukkit.getWorlds()) {
            border.applyToWorld(world);
        }
    }
}
