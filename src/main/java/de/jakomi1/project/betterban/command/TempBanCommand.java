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

/** {@code /tempban <Spieler> <Dauer> [Grund]} - temporaerer Ban. */
public final class TempBanCommand extends BetterBanCommand {

    public TempBanCommand(BetterBanManager manager) {
        super(manager);
    }

    @Override
    public String name() {
        return "tempban";
    }

    @Override
    public String description() {
        return "Bannt einen Spieler auf Zeit";
    }

    @Override
    public String usage() {
        return "/tempban <Spieler> <Dauer> [Grund]";
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

        if (manager.isBanned(uuid)) {
            sender.sendMessage(msg().error(targetName + " ist bereits gebannt."));
            return true;
        }

        long duration;

        try {
            duration = BetterBanTime.parseDuration(args[1]);
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(msg().error("Ungueltige Zeitangabe. Nutze z.B. 10m, 2h oder 1d."));
            return true;
        }

        String reason = joinArgs(args, 2);

        manager.tempBan(uuid, duration, reason);

        String formatted = BetterBanTime.formatDuration(duration);

        sender.sendMessage(msg().success(targetName + " wurde fuer " + formatted + " gebannt."));

        if (reason != null) {
            sender.sendMessage(msg().info("Grund: " + reason));
        }

        manager.sendBanWebhook(targetName, sender.getName(), formatted, reason, false);

        Player online = Bukkit.getPlayer(uuid);

        if (online != null && online.isOnline()) {
            // Kicks laufen ueber den Entity-Scheduler, damit sie auch unter
            // Folia in der richtigen Region ausgefuehrt werden.
            manager.server().scheduler().runEntity(online,
                    () -> online.kick(manager.messages().banMessage(manager.banEntry(uuid))));
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
            return filter(List.of("10m", "30m", "1h", "2h", "1d", "7d"), args[1]);
        }

        return List.of();
    }
}
