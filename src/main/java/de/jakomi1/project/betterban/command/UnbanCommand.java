package de.jakomi1.project.betterban.command;

import de.jakomi1.project.betterban.BetterBanManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/** {@code /unban <Spieler>} - hebt einen Ban auf. */
public final class UnbanCommand extends BetterBanCommand {

    public UnbanCommand(BetterBanManager manager) {
        super(manager);
    }

    @Override
    public String name() {
        return "unban";
    }

    @Override
    public String description() {
        return "Entbannt einen Spieler";
    }

    @Override
    public String usage() {
        return "/unban <Spieler>";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!requireArgs(sender, args, 1)) {
            return true;
        }

        UUID uuid = manager.uuidOf(args[0]);

        if (uuid == null) {
            sender.sendMessage(msg().error("Unbekannter Spieler: " + args[0]));
            return true;
        }

        String targetName = manager.nameOf(uuid);

        if (!manager.isBanned(uuid)) {
            sender.sendMessage(msg().error(targetName + " ist nicht gebannt."));
            return true;
        }

        manager.unban(uuid);

        sender.sendMessage(msg().success(targetName + " wurde entbannt."));
        manager.sendUnbanWebhook(targetName, sender.getName());

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return bannedNames(args[0]);
        }

        return List.of();
    }
}
