package de.jakomi1.project.playtime;

import de.jakomi1.command.CustomCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class TopTenCommand implements CustomCommand {

    private final PlaytimeManager manager;

    public TopTenCommand(PlaytimeManager manager) {
        this.manager = manager;
    }

    @Override
    public String name() {
        return "topten";
    }

    @Override
    public String description() {
        return "Zeigt die Top 10 Spieler nach Spielzeit";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<PlaytimeManager.PlaytimeEntry> top = manager.top(10);

        if (top.isEmpty()) {
            sender.sendMessage(prefix().append(Component.text("Keine Daten zur Spielzeit vorhanden.", NamedTextColor.GRAY)));
            return true;
        }

        sender.sendMessage(prefix().append(Component.text("Top 10 Spieler:", NamedTextColor.GRAY)
                .decoration(TextDecoration.BOLD, false)));

        for (int i = 0; i < top.size(); i++) {
            sender.sendMessage(formatEntry(i + 1, top.get(i)));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }

    private Component formatEntry(int position, PlaytimeManager.PlaytimeEntry entry) {
        return Component.text(">> %d %s - ".formatted(position, entry.name()), NamedTextColor.GRAY)
                .append(Component.text(
                        PlaytimeManager.format(entry.seconds()),
                        NamedTextColor.AQUA
                ));
    }

    private Component prefix() {
        Component prefix = manager.server().plugin().getPrefix();
        return prefix != null ? prefix : Component.empty();
    }
}
