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

/** {@code /banlist} - zeigt alle aktiven Bans. */
public final class BanListCommand extends BetterBanCommand {

    public BanListCommand(BetterBanManager manager) {
        super(manager);
    }

    @Override
    public String name() {
        return "banlist";
    }

    @Override
    public String description() {
        return "Zeigt alle aktiven Bans";
    }

    @Override
    public String usage() {
        return "/banlist";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        manager.clearExpiredBans();

        Map<UUID, BanEntry> bans = manager.allBans();

        if (bans.isEmpty()) {
            sender.sendMessage(msg().info("Es sind keine Spieler gebannt."));
            return true;
        }

        sender.sendMessage(msg().success("Gebannte Spieler:"));

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
