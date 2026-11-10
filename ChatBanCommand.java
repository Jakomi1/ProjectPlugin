package de.jakomi1.projectplugin.betterban.command;

import de.jakomi1.projectplugin.betterban.manager.BanManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.jakomi1.projectplugin.BetterBanPlugin.chatPrefix;
import static de.jakomi1.projectplugin.util.TextUtils.parseDuration;

public class ChatBanCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(chatPrefix + ChatColor.RED + "Usage: /chatban <Spieler> <Dauer> [Grund]");
            return true;
        }

        String targetName = args[0];
        String duration = args[1];
        boolean permanent = duration.equalsIgnoreCase("permanent");
        String reason = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : null;
        long delta = permanent ? 0L : parseDuration(duration.toLowerCase());

        BanManager.chatBan(sender, targetName, reason);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOfflinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(Objects::nonNull)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Stream.of("10m", "30m", "1h", "2h", "1d", "permanent")
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}