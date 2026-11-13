package de.jakomi1.project.betterban.command;

import de.jakomi1.project.betterban.BetterBanManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** {@code /kick <Spieler> [Grund]} - kickt einen online Spieler. */
public final class KickCommand extends BetterBanCommand {

    public KickCommand(BetterBanManager manager) {
        super(manager);
    }

    @Override
    public String name() {
        return "kick";
    }

    @Override
    public String description() {
        return "Kickt einen Spieler";
    }

    @Override
    public String usage() {
        return "/kick <Spieler> [Grund]";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!requireArgs(sender, args, 1)) {
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null) {
            sender.sendMessage(msg().error("Dieser Spieler ist nicht online."));
            return true;
        }

        if (sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(msg().error("Du kannst dich nicht selbst kicken."));
            return true;
        }

        String reason = joinArgs(args, 1);
        String executor = manager.executorName(sender);

        manager.sendKickWebhook(target.getName(), executor, reason);

        sender.sendMessage(msg().success(target.getName() + " wurde von " + executor + " gekickt."));
        // Kicks laufen ueber den Entity-Scheduler, damit sie auch unter Folia
        // in der richtigen Region ausgefuehrt werden.
        manager.server().scheduler().runEntity(target,
                () -> target.kick(manager.messages().kickMessage(reason)));

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return onlineNames(args[0]);
        }

        return List.of();
    }
}
