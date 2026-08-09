package de.jakomi1.project.playtime;

import de.jakomi1.command.CustomCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public final class PlaytimeCommand implements CustomCommand {

    private final PlaytimeManager manager;

    public PlaytimeCommand(PlaytimeManager manager) {
        this.manager = manager;
    }

    @Override
    public String name() {
        return "playtime";
    }

    @Override
    public String description() {
        return "Zeigt die Spielzeit eines Spielers an";
    }

    @Override
    public String usage() {
        return "/playtime [Spieler]";
    }

    @Override
    public List<String> aliases() {
        return List.of("playt", "ptime");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        OfflinePlayer target;
        boolean selfQuery = false;

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix().append(Component.text("Bitte gib einen Spielernamen an!", NamedTextColor.RED)));
                return true;
            }

            target = player;
            selfQuery = true;
        } else {
            target = Bukkit.getOfflinePlayer(args[0]);

            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(prefix().append(Component.text(
                        "Spieler \"" + args[0] + "\" nicht gefunden.",
                        NamedTextColor.RED
                )));
                return true;
            }
        }

        String playerName = target.getName() != null ? target.getName() : "Unbekannt";

        sender.sendMessage(prefix().append(Component.text(
                        selfQuery ? "Deine Spielzeit: " : playerName + "'s Spielzeit: ",
                        NamedTextColor.GRAY
                )
                .decoration(TextDecoration.BOLD, false)
                .append(Component.text(
                        PlaytimeManager.format(manager.playtime(target)),
                        NamedTextColor.AQUA
                ))));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player player) || !manager.canViewPlaytime(player)) {
                return List.of();
            }

            String prefix = args[0].toLowerCase();

            return Arrays.stream(Bukkit.getOfflinePlayers())
                    .map(OfflinePlayer::getName)
                    .filter(name -> name != null && name.toLowerCase().startsWith(prefix))
                    .toList();
        }

        return List.of();
    }

    private Component prefix() {
        Component prefix = manager.server().plugin().getPrefix();
        return prefix != null ? prefix : Component.empty();
    }
}
