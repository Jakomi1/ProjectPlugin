package de.jakomi1.project.commandlog;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Protokolliert ausgefuehrte Befehle und kann bestimmte davon unterbinden.
 *
 * <p>Der Manager ist bewusst opt-in ({@link #defaultState()} false): Ohne
 * {@code enable()} passiert nichts. Beim Aktivieren wird die Tabelle
 * registriert und der Listener eingehaengt.
 *
 * <p>Blockierte Befehle werden per Praefix verglichen, damit auch
 * {@code bukkit:} &amp; Co. erfasst werden. Spielern wird bei einem Treffer der
 * Befehl verworfen, der Konsole nur eine feste Auswahl.
 *
 * <p>Ist eine Benachrichtigung eingerichtet, erhalten passende Empfaenger eine
 * Nachricht. Format und Empfaenger bleiben dem Aufrufer ueberlassen, damit die
 * Library keine Textvorgaben macht.
 */
public final class CommandLogManager implements Manager {

    private final ProjectServer server;
    private final CommandLogTable table = new CommandLogTable();

    private boolean enabled;
    private boolean tableRegistered;
    private CommandLogListener listener;

    private final Set<String> blockedPlayerCommands = new LinkedHashSet<>();
    private final Set<String> blockedConsoleCommands = new LinkedHashSet<>();

    private Predicate<Player> notifyRecipients = player -> false;
    private BiFunction<Player, String, Component> notifyFormat = (sender, message) -> null;
    private String silentTag;

    public CommandLogManager(ProjectServer server) {
        this.server = server;
    }

    @Override
    public CommandLogManager enable() {
        if (enabled) return this;
        enabled = true;

        table();
        listener = new CommandLogListener(this);
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

    /**
     * Verwirft bei Spielern jeden Befehl, der mit einem der Praefixe beginnt.
     */
    public CommandLogManager blockPlayer(String... prefixes) {
        if (prefixes != null) {
            for (String prefix : prefixes) {
                if (prefix != null && !prefix.isBlank()) {
                    blockedPlayerCommands.add(prefix.toLowerCase(Locale.ROOT));
                }
            }
        }
        return this;
    }

    /**
     * Verwirft bei der Konsole jeden Befehl, dessen Name exakt einem der
     * Eintraege entspricht.
     */
    public CommandLogManager blockConsole(String... commands) {
        if (commands != null) {
            for (String command : commands) {
                if (command != null && !command.isBlank()) {
                    blockedConsoleCommands.add(command.toLowerCase(Locale.ROOT));
                }
            }
        }
        return this;
    }

    /** Traeger dieses Scoreboard-Tags loesen keine Benachrichtigung aus. */
    public CommandLogManager silentTag(String tag) {
        this.silentTag = tag == null || tag.isBlank() ? null : tag;
        return this;
    }

    /**
     * Empfaenger und Format der Benachrichtigung.
     *
     * @param recipients wer benachrichtigt wird (der Ausfuehrer ausgenommen)
     * @param format     erzeugt die Nachricht aus Ausfuehrer und rohem Befehl;
     *                   {@code null} unterdrueckt die Zustellung
     */
    public CommandLogManager notify(Predicate<Player> recipients,
                                    BiFunction<Player, String, Component> format) {
        this.notifyRecipients = recipients == null ? player -> false : recipients;
        this.notifyFormat = format == null ? (sender, message) -> null : format;
        return this;
    }

    public boolean isBlockedPlayerCommand(String command) {
        if (command == null) return false;

        String normalized = command.toLowerCase(Locale.ROOT);

        for (String blocked : blockedPlayerCommands) {
            if (normalized.startsWith(blocked)) return true;
        }

        return false;
    }

    public boolean isBlockedConsoleCommand(String commandName) {
        return commandName != null && blockedConsoleCommands.contains(commandName.toLowerCase(Locale.ROOT));
    }

    public boolean isSilent(Player player) {
        return silentTag != null && player != null && player.getScoreboardTags().contains(silentTag);
    }

    /** Schreibt einen Eintrag, sofern die Tabelle registriert ist. */
    public void log(String player, String command) {
        CommandLogTable log = table();
        if (log == null) return;

        long now = System.currentTimeMillis();
        log.put(now + "_" + player, new CommandLogTable.CommandLogEntry(player, command, now));
    }

    /** Stellt die Benachrichtigung an alle passenden Empfaenger zu. */
    public void notifyAdmins(Player sender, String message) {
        if (sender == null) return;
        if (isSilent(sender)) return;

        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (recipient.equals(sender)) continue;
            if (!notifyRecipients.test(recipient)) continue;

            Component text = notifyFormat.apply(sender, message);
            if (text != null) {
                recipient.sendMessage(text);
            }
        }
    }

    public CommandLogTable table() {
        if (!tableRegistered) {
            table.register(server.plugin());
            tableRegistered = true;
        }
        return table;
    }

    public ProjectServer server() {
        return server;
    }
}
