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

/**
 * Zeigt die zehn Spieler mit der meisten Spielzeit.
 *
 * <p>Die Liste kommt aus einem fertigen Snapshot, der im Hintergrund
 * erneuert wird. Der Command selbst sortiert nichts und liest keine Datei
 * ein - frueher wurde fuer jeden bekannten Spieler die Spielerdatendatei
 * geladen, was bei vielen Spielern spuerbar laggte.
 */
public final class TopTenCommand implements CustomCommand {

    private static final int LIMIT = 10;

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
    public String usage() {
        return "/topten";
    }

    @Override
    public String permission() {
        return "";
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        List<PlaytimeTable.PlaytimeEntry> top = PlaytimeStore.top(LIMIT);

        if (top.isEmpty()) {
            sender.sendMessage(prefix().append(
                    Component.text("Es sind noch keine Spielzeiten erfasst.", NamedTextColor.GRAY)));
            return true;
        }

        sender.sendMessage(prefix().append(
                Component.text("Top %d Spieler:".formatted(LIMIT), NamedTextColor.GRAY)
                        .decoration(TextDecoration.BOLD, false)
        ));

        for (int i = 0; i < top.size(); i++) {
            PlaytimeTable.PlaytimeEntry entry = top.get(i);

            sender.sendMessage(
                    Component.text(">> %d %s - ".formatted(i + 1, entry.name()), NamedTextColor.GRAY)
                            .append(Component.text(PlaytimeStore.format(entry.seconds()), NamedTextColor.AQUA))
            );
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        return List.of();
    }

    private Component prefix() {
        Component prefix = manager.server().plugin().getPrefix();
        return prefix != null ? prefix : Component.empty();
    }
}
