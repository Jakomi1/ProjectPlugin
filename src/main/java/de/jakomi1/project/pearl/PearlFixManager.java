package de.jakomi1.project.pearl;

import de.jakomi1.project.AutoManager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.scheduler.Scheduler;

import java.util.Set;
import java.util.UUID;

public final class PearlFixManager implements AutoManager {

    private final ProjectServer server;
    private final PearlFixTable table;
    private final PearlFixListener listener;

    private boolean enabled;
    private boolean auto;
    private boolean tableRegistered;

    public PearlFixManager(ProjectServer server) {
        this.server = server;
        this.table = new PearlFixTable();
        this.listener = new PearlFixListener(this);
    }

    @Override
    public PearlFixManager enable() {
        if (enabled) return this;
        enabled = true;

        pearlTable();
        listener.register(server.plugin());
        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;

        listener.saveAllPearlStates();
        listener.unregister();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean auto() {
        return auto;
    }

    @Override
    public PearlFixManager auto(boolean auto) {
        this.auto = auto;
        return this;
    }

    public PearlFixManager saveAll() {
        listener.saveAllPearlStates();
        return this;
    }

    public ProjectServer server() {
        return server;
    }

    public Scheduler scheduler() {
        return server.scheduler();
    }

    public PearlFixTable table() {
        return pearlTable();
    }

    void savePearls(UUID playerId, Set<SavedPearlState> pearls) {
        pearlTable().savePearls(playerId, pearls);
    }

    Set<SavedPearlState> loadPearls(UUID playerId) {
        return pearlTable().loadPearls(playerId);
    }

    void deletePearls(UUID playerId) {
        pearlTable().deletePearls(playerId);
    }

    private PearlFixTable pearlTable() {
        if (!tableRegistered) {
            table.register(server.plugin());
            tableRegistered = true;
        }
        return table;
    }
}
