package de.jakomi1.project.silent;

import de.jakomi1.command.CustomCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Schaltet den {@link SilentManager#tag() Silent-Tag} des ausfuehrenden
 * Spielers um. Die Wirkung - Chat ausblenden, Befehls-Meldungen unterdruecken -
 * liegt bei den jeweiligen Managern.
 */
public final class SilentCommand implements CustomCommand {

    private final SilentManager manager;

    public SilentCommand(SilentManager manager) {
        this.manager = manager;
    }

    @Override
    public String name() {
        return manager.commandName();
    }

    @Override
    public String description() {
        return "Schaltet den Silent-Modus um";
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        boolean silent = manager.toggle(player);

        player.sendMessage(prefix().append(Component.text(
                silent ? "Silent mode enabled." : "Silent mode disabled.",
                silent ? NamedTextColor.GREEN : NamedTextColor.RED)));

        return true;
    }

    private Component prefix() {
        Component prefix = manager.server().plugin().getPrefix();
        return prefix != null ? prefix : Component.empty();
    }
}
