package de.jakomi1.project.betterban.command;

import de.jakomi1.project.betterban.BetterBanManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/** {@code /voiceunban <Spieler>} - hebt eine Voice-Sperre auf. */
public final class VoiceUnbanCommand extends BetterBanCommand {

    public VoiceUnbanCommand(BetterBanManager manager) {
        super(manager);
    }

    @Override
    public String name() {
        return "voiceunban";
    }

    @Override
    public List<String> aliases() {
        return List.of("unmute");
    }

    @Override
    public String description() {
        return "Hebt eine Voice-Sperre auf";
    }

    @Override
    public String usage() {
        return "/voiceunban <Spieler>";
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

        if (!manager.isVoiceBanned(uuid)) {
            sender.sendMessage(msg().error(targetName + " ist nicht aus dem Voicechat gebannt."));
            return true;
        }

        manager.voiceUnban(uuid);
        manager.refreshVoiceDisabled(uuid);

        String executor = manager.executorName(sender);

        sender.sendMessage(msg().success(targetName + " wurde aus dem Voicechat entbannt."));
        manager.sendVoiceUnbanWebhook(targetName, executor);

        Player online = Bukkit.getPlayer(uuid);

        if (online != null && online.isOnline()) {
            online.sendMessage(msg().info("Du wurdest von " + executor + " aus dem Voicechat entbannt."));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return voiceBannedNames(args[0]);
        }

        return List.of();
    }
}
