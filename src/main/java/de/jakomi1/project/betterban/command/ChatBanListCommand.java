package de.jakomi1.project.betterban.command;

import de.jakomi1.project.betterban.BanEntry;
import de.jakomi1.project.betterban.BetterBanManager;
import de.jakomi1.project.betterban.BetterBanTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** {@code /chatbanlist} - zeigt alle aktiven Chat-Sperren. */
public final class ChatBanListCommand extends BetterBanCommand {

    public ChatBanListCommand(BetterBanManager manager) {
        super(manager);
    }

    @Override
    public String name() {
        return "chatbanlist";
    }

    @Override
    public String description() {
        return "Zeigt alle aktiven Chat-Sperren";
    }

    @Override
    public String usage() {
        return "/chatbanlist";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        manager.clearExpiredChatBans();

        Map<UUID, BanEntry> bans = manager.allChatBans();

        if (bans.isEmpty()) {
            sender.sendMessage(msg().info("Es sind keine Spieler aus dem Chat gebannt."));
            return true;
        }

        sender.sendMessage(msg().success("Chat-gebannte Spieler:"));

        for (Map.Entry<UUID, BanEntry> entry : bans.entrySet()) {
            BanEntry ban = entry.getValue();
            String name = manager.nameOf(entry.getKey());
            String duration = ban.permanent()
                    ? "permanent"
                    : BetterBanTime.formatDuration(Math.max(ban.endTimestamp() - System.currentTimeMillis(), 0L));

            sender.sendMessage(Component.text(name + " >> " + duration, NamedTextColor.GRAY));

            if (ban.reason() != null && !ban.reason().isBlank()) {
                sender.sendMessage(Component.text("-> Grund: " + ban.reason(), NamedTextColor.DARK_GRAY));
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
