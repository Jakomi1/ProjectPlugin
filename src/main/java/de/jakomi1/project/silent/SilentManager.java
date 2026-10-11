package de.jakomi1.project.silent;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Der Silent-Modus: ein Scoreboard-Tag, den ein Spieler an sich selbst haengen
 * kann, um Chat-Nachrichten und Befehls-Benachrichtigungen der anderen zu
 * ueberspringen.
 *
 * <p>Der Manager besitzt den {@link #tag() Tag-Namen} und den Umschalt-Befehl
 * {@code /silent}. Andere Manager lesen denselben Tag: {@link
 * de.jakomi1.project.chat.ChatManager#silentTag(String)} blendet stille
 * Empfaenger aus, {@link de.jakomi1.project.commandlog.CommandLogManager#silentTag(String)}
 * unterdrueckt deren Befehls-Meldungen. Der Tag steht damit nur an einer
 * Stelle - hier.
 *
 * <p>Der Manager ist opt-in ({@link #defaultState()} false).
 */
public final class SilentManager implements Manager {

    private final ProjectServer server;

    private boolean enabled;
    private SilentCommand command;

    private String tag = "silent";
    private String commandName = "silent";
    private String commandPermission;
    private List<String> commandAliases = List.of();

    public SilentManager(ProjectServer server) {
        this.server = server;
    }

    @Override
    public SilentManager enable() {
        if (enabled) return this;
        enabled = true;

        if (commandName != null) {
            command = new SilentCommand(this);
            command.register(server.plugin());
        }

        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;
        command = null;
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

    public SilentManager tag(String tag) {
        this.tag = tag == null || tag.isBlank() ? "silent" : tag;
        return this;
    }

    public String tag() {
        return tag;
    }

    public boolean isSilent(Player player) {
        return player != null && tag != null && player.getScoreboardTags().contains(tag);
    }

    /** Schaltet den Silent-Tag des Spielers um und liefert den neuen Zustand. */
    public boolean toggle(Player player) {
        if (player == null) return false;

        if (player.getScoreboardTags().contains(tag)) {
            player.getScoreboardTags().remove(tag);
            return false;
        }

        player.getScoreboardTags().add(tag);
        return true;
    }

    /** Name des Umschalt-Befehls; {@code null} deaktiviert ihn. */
    public SilentManager commandName(String name) {
        this.commandName = name == null || name.isBlank() ? null : name;
        return this;
    }

    public String commandName() {
        return commandName;
    }

    /** Permission des Befehls. Ohne Angabe die generierte Permission. */
    public SilentManager commandPermission(String permission) {
        this.commandPermission = permission == null || permission.isBlank() ? null : permission;
        return this;
    }

    public String commandPermission() {
        return commandPermission;
    }

    public SilentManager commandAliases(String... aliases) {
        if (aliases == null || aliases.length == 0) {
            this.commandAliases = List.of();
            return this;
        }

        List<String> cleaned = new ArrayList<>();

        for (String alias : aliases) {
            if (alias != null && !alias.isBlank()) {
                cleaned.add(alias);
            }
        }

        this.commandAliases = List.copyOf(cleaned);
        return this;
    }

    public List<String> commandAliases() {
        return commandAliases;
    }
}
