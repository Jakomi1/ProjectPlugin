package de.jakomi1.project.state;

import de.jakomi1.database.table.GlobalSettingsTable;
import de.jakomi1.project.ProjectServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.EnumMap;
import java.util.Map;

public final class StateManager {

    private final ProjectServer server;
    private final GlobalSettingsTable settingsTable;
    private final Map<ServerState, StateSettings> settings = new EnumMap<>(ServerState.class);
    private final StateJoinListener joinListener;
    private final StateScheduler scheduler;

    public StateManager(ProjectServer server, GlobalSettingsTable settingsTable) {
        this.server = server;
        this.settingsTable = settingsTable;
        this.joinListener = new StateJoinListener(this);
        joinListener.register(server.plugin());
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
                .motd(current.motd())
                .subMotd(current.subMotd())
                .joinAllowed(current.joinAllowed())
                .kickMessage(current.kickMessage())
                .hidePlayers(current.hidePlayers())
                .border(border)
                .build();

        settings.put(state, updated);
        if (state == currentState()) refresh();
        return this;
    }

    public StateManager schedule(StateSchedule schedule) {
        scheduler.schedule(schedule).start();
        return this;
    }

    public StateScheduler scheduler() {
        return scheduler;
    }

    public boolean allowsJoin() {
        return settings(currentState()).joinAllowed();
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
