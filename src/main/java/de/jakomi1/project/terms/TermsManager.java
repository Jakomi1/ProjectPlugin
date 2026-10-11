package de.jakomi1.project.terms;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Verwaltet die Nutzungsbedingungen eines Projekts.
 *
 * <p>Die Zustimmung wird pro Spieler in der {@link TermsTable} gespeichert.
 * Beim ersten Join schickt der Manager die konfigurierten {@link #messages()
 * Texte} in den Chat und vermerkt die Zustimmung direkt - ein Dialog ist dabei
 * nicht beteiligt. Ist die Zustimmung bereits vorhanden, passiert nichts.
 *
 * <p>Der Manager ist opt-in ({@link #defaultState()} ist {@code false}):
 * Erst ein {@link #enable()} richtet Tabelle und Listener ein, damit eine
 * Library-Nutzung ohne Nutzungsbedingungen nichts veraendert.
 */
public final class TermsManager implements Manager {

    private final ProjectServer server;
    private final TermsTable table;

    private TermsListener listener;

    private boolean enabled;
    private boolean tableRegistered;
    private List<Component> messages = List.of();

    public TermsManager(ProjectServer server) {
        this.server = server;
        this.table = new TermsTable();
    }

    @Override
    public TermsManager enable() {
        if (enabled) return this;
        enabled = true;

        termsTable();

        listener = new TermsListener(this);
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
     * Ersetzt die Texte, die einem Spieler beim ersten Join geschickt werden.
     * Jeder Eintrag wird als eigene Zeile gesendet.
     */
    public TermsManager messages(List<Component> messages) {
        this.messages = messages == null ? List.of() : List.copyOf(messages);
        return this;
    }

    /** Haengt eine weitere Zeile an die Join-Texte an. */
    public TermsManager message(Component message) {
        if (message == null) return this;

        List<Component> updated = new ArrayList<>(messages);
        updated.add(message);
        this.messages = List.copyOf(updated);
        return this;
    }

    public List<Component> messages() {
        return messages;
    }

    public TermsTable table() {
        return termsTable();
    }

    public boolean hasAccepted(UUID uniqueId) {
        return termsTable().hasAccepted(uniqueId);
    }

    public boolean hasAccepted(Player player) {
        return player != null && hasAccepted(player.getUniqueId());
    }

    public TermsManager accept(Player player) {
        if (player != null) {
            accept(player.getUniqueId(), player.getName());
        }
        return this;
    }

    public TermsManager accept(UUID uniqueId, String playerName) {
        termsTable().accept(uniqueId, playerName);
        return this;
    }

    public TermsManager revoke(UUID uniqueId) {
        termsTable().revoke(uniqueId);
        return this;
    }

    public TermsManager revoke(Player player) {
        if (player != null) {
            revoke(player.getUniqueId());
        }
        return this;
    }

    /** Entfernt alle Zustimmungen und liefert deren Anzahl zurueck. */
    public int clearAll() {
        int removed = termsTable().size();
        termsTable().clearAll();
        return removed;
    }

    public int count() {
        return termsTable().acceptedCount();
    }

    /** Wird vom {@link TermsListener} beim Join aufgerufen. */
    void handleJoin(Player player) {
        if (player == null || hasAccepted(player)) return;

        termsTable().accept(player.getUniqueId(), player.getName());

        for (Component message : messages) {
            player.sendMessage(message);
        }
    }

    private TermsTable termsTable() {
        if (!tableRegistered) {
            table.register(server.plugin());
            tableRegistered = true;
        }
        return table;
    }
}
