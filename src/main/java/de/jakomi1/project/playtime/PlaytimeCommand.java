package de.jakomi1.project.playtime;

import de.jakomi1.command.CustomCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Zeigt die Spielzeit eines Spielers an.
 *
 * <p>Ohne Argument wird die eigene Zeit angezeigt. Die Tab-Vorschlaege
 * kommen aus der persistenten Tabelle und enthalten damit jeden Spieler, der
 * den Server schon einmal besucht hat - auch wenn er gerade offline ist.
 * Eine Abfrage von {@code getOfflinePlayers()} waere hier nicht nur langsam,
 * sie wuerde zudem bei jedem Tastendruck die Spielerdatendatei laden.
 */
public final class PlaytimeCommand implements CustomCommand {

    private static final int SUGGESTION_LIMIT = 60;

    private final PlaytimeManager manager;

    public PlaytimeCommand(PlaytimeManager manager) {
        this.manager = manager;
    }

    @Override
    public String name() {
        return "playtime";
    }

    @Override
    public String description() {
        return "Zeigt die Spielzeit eines Spielers an";
    }

    @Override
    public String usage() {
        return "/playtime [Spieler]";
    }

    @Override
    public String permission() {
        return "";
    }

    @Override
    public List<String> aliases() {
        return List.of("playt", "ptime");
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(prefix().append(
                        Component.text("Bitte gib einen Spielernamen an!", NamedTextColor.RED)));
                return true;
            }

            sender.sendMessage(result("Deine Spielzeit: ", player.getName()));
            return true;
        }

        String query = args[0];

        if (!PlaytimeStore.isKnown(query)) {
            sender.sendMessage(prefix().append(Component.text(
                    "Spieler \"" + query + "\" wurde auf diesem Server noch nicht gesehen.",
                    NamedTextColor.RED
            )));
            return true;
        }

        String name = PlaytimeStore.nameOf(query);

        sender.sendMessage(result(name + "'s Spielzeit: ", name));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length != 1) return List.of();

        String prefix = args[0].toLowerCase(Locale.ROOT);
        if (prefix.isEmpty()) return List.of();

        return PlaytimeStore.knownNames().stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(prefix))
                .limit(SUGGESTION_LIMIT)
                .toList();
    }

    private Component result(String label, String name) {
        return prefix().append(
                Component.text(label, NamedTextColor.GRAY)
                        .decoration(TextDecoration.BOLD, false)
                        .append(Component.text(PlaytimeStore.format(PlaytimeStore.seconds(name)),
                                NamedTextColor.AQUA))
        );
    }

    private Component prefix() {
        Component prefix = manager.server().plugin().getPrefix();
        return prefix != null ? prefix : Component.empty();
    }
}
