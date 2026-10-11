package de.jakomi1.project.commandlog;

import de.jakomi1.listener.EventListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

/**
 * Reicht ausgefuehrte Befehle an den {@link CommandLogManager} weiter.
 */
public final class CommandLogListener extends EventListener {

    private final CommandLogManager manager;

    public CommandLogListener(CommandLogManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        String command = message.startsWith("/") ? message.substring(1) : message;

        if (manager.isBlockedPlayerCommand(command)) {
            event.setCancelled(true);
            return;
        }

        manager.log(event.getPlayer().getName(), message);
        manager.notifyAdmins(event.getPlayer(), message);
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand();
        String commandName = command.split(" ", 2)[0];

        if (manager.isBlockedConsoleCommand(commandName)) {
            event.setCancelled(true);
            return;
        }

        manager.log("CONSOLE", "/" + event.getCommand());
    }
}
