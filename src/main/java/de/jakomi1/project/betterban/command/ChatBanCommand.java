package de.jakomi1.project.betterban.command;

import de.jakomi1.project.betterban.BetterBanManager;
import de.jakomi1.project.betterban.BetterBanTime;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/** {@code /chatban <Spieler> <Dauer|permanent> [Grund]} - Chat-Sperre. */
public final class ChatBanCommand extends BetterBanCommand {

    public ChatBanCommand(BetterBanManager manager) {
        super(manager);
    }

    @Override
    public String name() {
        return "chatban";
    }

    @Override
    public String description() {
        return "Sperrt den Chat eines Spielers";
    }

    @Override
    public String usage() {
        return "/chatban <Spieler> <Dauer|permanent> [Grund]";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!requireArgs(sender, args, 2)) {
            return true;
        }

        UUID uuid = manager.uuidOf(args[0]);

        if (uuid == null) {
            sender.sendMessage(msg().error("Unbekannter Spieler: " + args[0]));
            return true;
        }

        String targetName = manager.nameOf(uuid);

        if (!manager.hasJoined(uuid)) {
            sender.sendMessage(msg().error("Dieser Spieler war noch nie auf dem Server."));
            return true;
        }

        if (manager.isChatBanned(uuid)) {
            sender.sendMessage(msg().error(targetName + " ist bereits aus dem Chat gebannt."));
            return true;
        }

        boolean permanent = args[1].equalsIgnoreCase("permanent");
        long duration = 0L;

        if (!permanent) {
            try {
                duration = BetterBanTime.parseDuration(args[1]);
            } catch (IllegalArgumentException exception) {
                sender.sendMessage(msg().error("Ungueltige Zeitangabe. Nutze z.B. 10m, 2h, 1d oder permanent."));
                return true;
            }
        }

        String reason = joinArgs(args, 2);

        if (permanent) {
            manager.chatPermanentBan(uuid, reason);
        } else {
            manager.chatTempBan(uuid, duration, reason);
        }

        String formatted = permanent ? "permanent" : BetterBanTime.formatDuration(duration);
        String executor = manager.executorName(sender);

        sender.sendMessage(msg().success(targetName + " wurde fuer " + formatted + " aus dem Chat gebannt."));

        if (reason != null) {
            sender.sendMessage(msg().info("Grund: " + reason));
        }

        manager.sendChatBanWebhook(targetName, executor, formatted, reason, permanent);

        Player online = Bukkit.getPlayer(uuid);

        if (online != null && online.isOnline()) {
            online.sendMessage(msg().success("Du wurdest von " + executor + " fuer " + formatted + " aus dem Chat gebannt."));

            if (reason != null) {
                online.sendMessage(msg().info("Grund: " + reason));
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return joinedNames(args[0]);
        }

        if (args.length == 2) {
            return filter(List.of("10m", "30m", "1h", "2h", "1d", "7d", "permanent"), args[1]);
        }

        return List.of();
    }
}
