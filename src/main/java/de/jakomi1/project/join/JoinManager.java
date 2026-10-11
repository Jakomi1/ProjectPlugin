package de.jakomi1.project.join;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Buendelt die generischen Aufgaben beim Beitritt und Verlassen eines
 * Spielers und bietet Platz fuer projektspezifisches Verhalten.
 *
 * <p>Der Manager uebernimmt genau die Aufgaben, die fuer jedes Projekt gleich
 * sind und nur aktivierte Manager betreffen:
 *
 * <ul>
 *     <li><b>Beitritt:</b> Rollen-Permissions anwenden, Tab-Liste
 *         aktualisieren, Nametags neu sortieren und die Spielzeit
 *         erfassen.</li>
 *     <li><b>Verlassen:</b> Spielzeit sofort speichern und Nametags neu
 *         sortieren.</li>
 * </ul>
 *
 * <p>Projektspezifisches Verhalten - etwa Welt-Regeln, Owner-Erzwingung,
 * Teleport beim ersten Beitritt - wird ueber {@link #onJoin(Consumer)}
 * registriert. Die Haken laufen <b>vor</b> den generischen Aufgaben, damit
 * eine dort gesetzte Rolle bereits in Tab-Liste und Nametags sichtbar ist.
 *
 * <p>Der Manager ist opt-in ({@link #defaultState()} ist {@code false}):
 * Erst ein {@link #enable()} registriert den Listener.
 */
public final class JoinManager implements Manager {

    private final ProjectServer server;
    private final List<Consumer<Player>> joinHandlers = new ArrayList<>();
    private final List<Consumer<Player>> quitHandlers = new ArrayList<>();

    private boolean enabled;
    private JoinListener listener;

    public JoinManager(ProjectServer server) {
        this.server = server;
    }

    @Override
    public JoinManager enable() {
        if (enabled) return this;

        enabled = true;

        listener = new JoinListener(this);
        listener.register(server.plugin());

        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;

        enabled = false;

        if (listener != null) {
            listener.unregister();
            listener = null;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean defaultState() {
        return false;
    }

    public ProjectServer server() {
        return server;
    }

    /**
     * Registriert eine projektspezifische Aufgabe fuer den Beitritt. Mehrere
     * Haken laufen in der Reihenfolge ihrer Registrierung, jeweils vor den
     * generischen Aufgaben.
     */
    public JoinManager onJoin(Consumer<Player> handler) {
        if (handler != null) {
            joinHandlers.add(handler);
        }

        return this;
    }

    /**
     * Registriert eine projektspezifische Aufgabe fuer das Verlassen. Mehrere
     * Haken laufen in der Reihenfolge ihrer Registrierung, jeweils vor den
     * generischen Aufgaben.
     */
    public JoinManager onQuit(Consumer<Player> handler) {
        if (handler != null) {
            quitHandlers.add(handler);
        }

        return this;
    }

    /** Wird vom {@link JoinListener} aufgerufen. */
    public void handleJoin(Player player) {
        if (player == null) return;

        for (Consumer<Player> handler : joinHandlers) {
            handler.accept(player);
        }

        applyJoinDefaults(player);
    }

    /** Wird vom {@link JoinListener} aufgerufen. */
    public void handleQuit(Player player) {
        if (player == null) return;

        for (Consumer<Player> handler : quitHandlers) {
            handler.accept(player);
        }

        applyQuitDefaults(player);
    }

    private void applyJoinDefaults(Player player) {
        if (server.permissions().isEnabled()) {
            server.permissions().apply(player);
        }

        if (server.tab().isEnabled()) {
            server.tab().update(player);
        }

        if (server.nametags().isEnabled()) {
            server.nametags().requestSync();
        }

        if (server.playtime().isEnabled()) {
            server.playtime().track(player);
        }
    }

    private void applyQuitDefaults(Player player) {
        if (server.playtime().isEnabled()) {
            server.playtime().saveNow(player);
        }

        if (server.nametags().isEnabled()) {
            server.nametags().requestSync();
        }
    }
}
