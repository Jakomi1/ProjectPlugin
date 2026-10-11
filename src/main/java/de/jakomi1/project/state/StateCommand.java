package de.jakomi1.project.state;

import de.jakomi1.command.CustomCommand;
import de.jakomi1.player.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Steuert den Zustand des Servers zusammen mit der Worldborder.
 *
 * <p>{@code stop} sperrt die Welt auf {@link StateSettings#LOCKED_BORDER_SIZE}
 * Durchmesser ein und laesst nur noch die Rollen herein, die die Einstellungen
 * des Zustands erlauben. Dabei werden alle Spielerdaten geloescht - bis auf die
 * Tabellen, aus denen die Library Whitelist, Rollen und Serverzustand zieht -
 * und die Weltzeit auf {@link de.jakomi1.project.worldcycle.WorldCycleManager#lockedTime()}
 * fixiert. {@code open} hebt die Join-Sperre auf, die Border bleibt klein.
 * Erst {@code start} laesst den Countdown laufen und setzt am Ende die grosse
 * Border.
 *
 * <p>Name, Aliase und Permission kommen aus dem {@link StateManager}, damit
 * jedes Projekt seinen eigenen Befehlsnamen behaelt. Die Texte sind bewusst
 * zentral, weil ein gestoppter Server immer dieselbe Ansage braucht.
 */
public final class StateCommand implements CustomCommand {

    private static final List<String> OPTIONS = List.of("start", "stop", "open", "status");

    private final StateManager manager;

    public StateCommand(StateManager manager) {
        this.manager = manager;
    }

    @Override
    public String name() {
        return manager.commandName();
    }

    @Override
    public String description() {
        return "Startet, stoppt oder oeffnet den Server inklusive Worldborder";
    }

    @Override
    public String usage() {
        return "/" + manager.commandName() + " <start|stop|open|status>";
    }

    @Override
    public String permission() {
        String configured = manager.commandPermission();
        return configured != null
                ? configured
                : CustomCommand.super.generatedPermission(manager.server().plugin());
    }

    @Override
    public List<String> aliases() {
        return manager.commandAliases();
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            usage(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "start" -> handleStart(sender);
            case "stop" -> handleStop(sender);
            case "open" -> handleOpen(sender);
            case "status" -> handleStatus(sender);
            default -> usage(sender);
        }

        return true;
    }

    private void handleStart(CommandSender sender) {
        if (manager.currentState() == ServerState.STARTED) {
            info(sender, "Der Server läuft bereits.");
            return;
        }

        if (!manager.startCountdown()) {
            info(sender, "Der Start läuft bereits.");
            return;
        }

        broadcast("Der Server startet in "
                + StateManager.START_COUNTDOWN_SECONDS
                + " Sekunden.", NamedTextColor.GREEN);
    }

    private void handleStop(CommandSender sender) {
        manager.set(ServerState.STOPPED);
        manager.server().worldCycles().lock();

        int removed = PlayerData.wipe(manager.server());

        broadcast("Server gestoppt. Worldborder "
                + (int) StateSettings.LOCKED_BORDER_SIZE
                + " Durchmesser",
                NamedTextColor.RED);

        info(sender, removed
                + " Spielerdaten gelöscht. Whitelist, Rollen und Serverzustand bleiben erhalten.");
        info(sender, "Zeit auf " + manager.server().worldCycles().lockedTime()
                + " fixiert, Tages- und Wetterzyklus laufen erst nach dem Start-Countdown wieder.");
    }

    private void handleOpen(CommandSender sender) {
        manager.set(ServerState.OPEN);
        broadcast("Server geöffnet für alle Spieler.", NamedTextColor.GRAY);
    }

    private void handleStatus(CommandSender sender) {
        ServerState current = manager.currentState();
        info(sender, "Zustand: " + current.name()
                + (manager.isStarting() ? " (Start laeuft)" : ""));
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length != 1) return List.of();

        String prefix = args[0].toLowerCase(Locale.ROOT);

        return new ArrayList<>(OPTIONS).stream()
                .filter(option -> option.startsWith(prefix))
                .toList();
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(prefix().append(
                Component.text("Verwendung: " + usage(), NamedTextColor.RED)));
    }

    private void info(CommandSender sender, String message) {
        sender.sendMessage(prefix().append(
                Component.text(message, NamedTextColor.YELLOW)));
    }

    private void broadcast(String message, NamedTextColor color) {
        manager.server().broadcast(prefix().append(Component.text(message, color)));
    }

    private Component prefix() {
        Component prefix = manager.server().plugin().getPrefix();
        return prefix != null ? prefix : Component.empty();
    }
}
