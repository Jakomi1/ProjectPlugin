package de.jakomi1.project.whitelist;

import de.jakomi1.command.CustomCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class WhitelistCommand implements CustomCommand {

    private final WhitelistManager manager;

    public WhitelistCommand(WhitelistManager manager) {
        this.manager = manager;
    }

    @Override
    public String name() {
        return "whitelist";
    }

    @Override
    public String description() {
        return "Verwaltet die Server-Whitelist";
    }

    @Override
    public String usage() {
        return "/whitelist <add|remove|list>";
    }

    @Override
    public String permission() {
        return manager.permission();
    }

    @Override
    public PermissionDefault permissionDefault() {
        return PermissionDefault.OP;
    }

    @Override
    public List<String> aliases() {
        return List.of("wl");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(prefix().append(Component.text(usage(), NamedTextColor.RED)));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> handleList(sender);
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            default -> sender.sendMessage(prefix().append(Component.text("Unbekannter Unterbefehl.", NamedTextColor.RED)));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("add", "remove", "list");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return manager.table().all().stream()
                    .map(WhitelistTable.WhitelistEntry::name)
                    .filter(Objects::nonNull)
                    .filter(name -> !name.isBlank())
                    .toList();
        }

        return List.of();
    }

    private void handleList(CommandSender sender) {
        List<String> entries = manager.table().all().stream()
                .map(WhitelistTable.WhitelistEntry::name)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        Set<String> online = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toSet());

        Component list = Component.empty();

        for (int i = 0; i < entries.size(); i++) {
            String name = entries.get(i);

            list = list.append(Component.text(
                    name,
                    online.contains(name) ? NamedTextColor.AQUA : NamedTextColor.GRAY
            ));

            if (i < entries.size() - 1) {
                list = list.append(Component.text(", ", NamedTextColor.DARK_GRAY));
            }
        }

        sender.sendMessage(prefix().append(
                Component.text("Whitelist (" + entries.size() + "): ", NamedTextColor.AQUA)
                        .append(list)
        ));
    }

    private void handleAdd(CommandSender sender, String[] args) {
        List<String> names = parseNames(args);

        if (names.isEmpty()) {
            sender.sendMessage(prefix().append(Component.text("Bitte Spieler angeben.", NamedTextColor.RED)));
            return;
        }

        List<String> added = new ArrayList<>();

        for (String name : names) {
            if (!manager.table().isWhitelisted(name)) {
                manager.add(name);
                added.add(name);
            }
        }

        if (added.isEmpty()) {
            sender.sendMessage(prefix().append(Component.text("Alle Spieler sind bereits gewhitelistet.", NamedTextColor.RED)));
            return;
        }

        sender.sendMessage(prefix().append(Component.text(
                formatList(added) + " wurde" + (added.size() > 1 ? "n" : "") + " gewhitelistet.",
                NamedTextColor.GRAY
        )));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        List<String> names = parseNames(args);

        if (names.isEmpty()) {
            sender.sendMessage(prefix().append(Component.text("Bitte Spieler angeben.", NamedTextColor.RED)));
            return;
        }

        List<String> removed = new ArrayList<>();

        for (String name : names) {
            if (manager.table().isWhitelisted(name)) {
                manager.remove(name);
                removed.add(name);

                Player target = Bukkit.getPlayerExact(name);
                if (target != null) {
                    target.kick(prefix().append(
                            Component.text("Du wurdest von der Whitelist entfernt.", NamedTextColor.RED)
                    ));
                }
            }
        }

        if (removed.isEmpty()) {
            sender.sendMessage(prefix().append(Component.text("Keiner der Spieler ist gewhitelistet.", NamedTextColor.RED)));
            return;
        }

        sender.sendMessage(prefix().append(Component.text(
                formatList(removed) + " wurde" + (removed.size() > 1 ? "n" : "") + " entfernt.",
                NamedTextColor.RED
        )));
    }

    private static List<String> parseNames(String[] args) {
        if (args.length < 2) return List.of();

        return Arrays.stream(String.join(" ", Arrays.copyOfRange(args, 1, args.length)).split("[, ]+"))
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .toList();
    }

    private static String formatList(List<String> names) {
        return String.join(", ", names);
    }

    private Component prefix() {
        Component prefix = manager.server().plugin().getPrefix();
        return prefix != null ? prefix : Component.empty();
    }
}
