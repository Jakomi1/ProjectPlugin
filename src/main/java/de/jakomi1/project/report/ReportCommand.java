package de.jakomi1.project.report;

import de.jakomi1.command.CustomCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReportCommand implements CustomCommand {

    private final ReportManager manager;

    public ReportCommand(ReportManager manager) {
        this.manager = manager;
    }

    @Override
    public String name() {
        return "report";
    }

    @Override
    public String description() {
        return "Meldet einen Spieler oder ein Problem";
    }

    @Override
    public String usage() {
        return "/report <Kategorie> <Spieler> <Text>";
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(
                    "Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(prefix().append(Component.text(
                    "Usage: /report <category> <player> <text>", NamedTextColor.RED)));
            return true;
        }

        String category = manager.resolveCategory(args[0]);

        if (category == null) {
            player.sendMessage(prefix().append(Component.text(
                    "Invalid category. Use: "
                            + String.join(", ", manager.categories()),
                    NamedTextColor.RED)));
            return true;
        }

        long remaining = manager.remainingCooldown(player.getUniqueId());

        if (remaining > 0) {
            player.sendMessage(prefix().append(Component.text(
                    "You must wait " + (remaining / 1000)
                            + " seconds before reporting again.",
                    NamedTextColor.RED)));
            return true;
        }

        String target = args[1];

        StringBuilder textBuilder = new StringBuilder();

        for (int i = 2; i < args.length; i++) {
            if (textBuilder.length() > 0) {
                textBuilder.append(" ");
            }

            textBuilder.append(args[i]);
        }

        int caseId = manager.submit(
                player.getName(),
                category,
                target,
                textBuilder.toString()
        );

        manager.record(player.getUniqueId());

        player.sendMessage(prefix().append(Component.text(
                "Report submitted. Case ID: #" + caseId,
                NamedTextColor.GREEN)));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> completions = new ArrayList<>();

            for (String category : manager.categories()) {
                if (category.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    completions.add(category);
                }
            }

            return completions;
        }

        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);

            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }

        return List.of();
    }

    private Component prefix() {
        Component prefix = manager.server().plugin().getPrefix();
        return prefix != null ? prefix : Component.empty();
    }
}
