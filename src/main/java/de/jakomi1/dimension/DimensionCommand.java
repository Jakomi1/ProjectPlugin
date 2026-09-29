package de.jakomi1.dimension;

import de.jakomi1.command.CustomCommand;
import de.jakomi1.project.ProjectServer;
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
import java.util.List;
import java.util.Locale;

public final class DimensionCommand implements CustomCommand {

    private static final String SUB_TELEPORT = "teleport";
    private static final String SUB_ENABLE = "enable";
    private static final String SUB_DISABLE = "disable";

    private final DimensionManager manager;
    private final ProjectServer server;

    public DimensionCommand(DimensionManager manager) {
        this.manager = manager;
        this.server = manager.server();
    }

    @Override
    public String name() {
        return "dimension";
    }

    @Override
    public String description() {
        return "Teleportiert Spieler in beliebige Dimensionen und sperrt Dimensionen";
    }

    @Override
    public String usage() {
        return "/dimension " + SUB_TELEPORT + " <dimension> <player> <x> <y> <z> | /dimension "
                + SUB_ENABLE + " <dimension> | /dimension " + SUB_DISABLE + " <dimension>";
    }

    @Override
    public List<String> aliases() {
        return List.of("dim");
    }

    @Override
    public String permission() {
        String permission = manager.permission();
        return permission != null ? permission : "";
    }

    @Override
    public PermissionDefault permissionDefault() {
        return PermissionDefault.OP;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            sender.sendMessage(prefix().append(Component.text(usage(), NamedTextColor.RED)));
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case SUB_TELEPORT -> handleTeleport(sender, args);
            case SUB_ENABLE -> handleToggle(sender, args, true);
            case SUB_DISABLE -> handleToggle(sender, args, false);
            default -> {
                sender.sendMessage(prefix().append(
                        Component.text("Unbekannter Unterbefehl '" + args[0] + "'. Nutze: " + usage(),
                                NamedTextColor.RED)));
                yield true;
            }
        };
    }

    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix().append(
                    Component.text("Dieser Befehl ist nur für Spieler ausführbar.", NamedTextColor.RED)));
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(prefix().append(
                    Component.text("/dimension " + SUB_TELEPORT + " <dimension> <player> [x y z]",
                            NamedTextColor.RED)));
            return true;
        }

        String dimension = args[1];

        if (manager.isDisabled(dimension)) {
            sender.sendMessage(prefix().append(Component.text(
                    "Die Dimension '" + dimension + "' ist gesperrt.", NamedTextColor.RED)));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(prefix().append(
                    Component.text("Spieler '" + args[2] + "' ist nicht online.", NamedTextColor.RED)));
            return true;
        }

        if (args.length < 7) {
            // Ohne Koordinaten reicht der Spawn der Dimension.
            try {
                manager.teleport(target, dimension);
                sender.sendMessage(prefix().append(Component.text(
                        "Teleportiere '" + target.getName() + "' nach " + dimension + " ...",
                        NamedTextColor.GRAY)));
            } catch (IllegalStateException e) {
                sender.sendMessage(prefix().append(Component.text(e.getMessage(), NamedTextColor.RED)));
            }
            return true;
        }

        double x = parseCoordinate(args[4], target.getLocation().getX());
        double y = parseCoordinate(args[5], target.getLocation().getY());
        double z = parseCoordinate(args[6], target.getLocation().getZ());

        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
            sender.sendMessage(prefix().append(Component.text("Ungültige Koordinaten.", NamedTextColor.RED)));
            return true;
        }

        try {
            manager.teleport(target, dimension, x, y, z);
            sender.sendMessage(prefix().append(Component.text(
                    "Teleportiere '" + target.getName() + "' nach " + dimension
                            + " (" + x + ", " + y + ", " + z + ") ...", NamedTextColor.GRAY)));
        } catch (IllegalStateException e) {
            sender.sendMessage(prefix().append(Component.text(e.getMessage(), NamedTextColor.RED)));
        }

        return true;
    }

    private boolean handleToggle(CommandSender sender, String[] args, boolean enable) {
        if (args.length < 2) {
            sender.sendMessage(prefix().append(Component.text(
                    (enable ? "/dimension " + SUB_ENABLE : "/dimension " + SUB_DISABLE) + " <dimension>",
                    NamedTextColor.RED)));
            return true;
        }

        String dimension = args[1];

        if (!manager.contains(dimension) && !manager.isLoaded(dimension)
                && !manager.isDisabled(dimension)) {
            sender.sendMessage(prefix().append(Component.text(
                    "Unbekannte Dimension '" + dimension + "'.", NamedTextColor.RED)));
            return true;
        }

        boolean changed = enable
                ? manager.enableDimension(dimension)
                : manager.disableDimension(dimension);

        if (enable) {
            if (changed) {
                sender.sendMessage(prefix().append(Component.text(
                        "Die Dimension '" + dimension + "' ist wieder freigegeben.", NamedTextColor.GREEN)));
            } else {
                sender.sendMessage(prefix().append(Component.text(
                        "Die Dimension '" + dimension + "' war nicht gesperrt.", NamedTextColor.GRAY)));
            }
        } else {
            if (changed) {
                sender.sendMessage(prefix().append(Component.text(
                        "Die Dimension '" + dimension + "' ist jetzt gesperrt. Niemand kann sie betreten.",
                        NamedTextColor.RED)));
            } else {
                sender.sendMessage(prefix().append(Component.text(
                        "Die Dimension '" + dimension + "' war bereits gesperrt.", NamedTextColor.GRAY)));
            }
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
        if (args.length == 1) {
            return List.of(SUB_TELEPORT, SUB_ENABLE, SUB_DISABLE);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 2) {
            if (SUB_TELEPORT.equals(sub) || SUB_ENABLE.equals(sub) || SUB_DISABLE.equals(sub)) {
                return dimensionSuggestions();
            }
            return List.of();
        }

        if (SUB_TELEPORT.equals(sub) && args.length == 3) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
        }

        if (SUB_TELEPORT.equals(sub) && (args.length == 4 || args.length == 5 || args.length == 6)) {
            return List.of("~");
        }

        return List.of();
    }

    private List<String> dimensionSuggestions() {
        List<String> suggestions = new ArrayList<>(manager.keys());
        suggestions.add("minecraft:overworld");
        suggestions.add("minecraft:the_nether");
        suggestions.add("minecraft:the_end");
        return suggestions;
    }

    private static double parseCoordinate(String input, double relative) {
        if (input == null) return Double.NaN;

        String trimmed = input.trim();

        if (trimmed.startsWith("~")) {
            String offset = trimmed.substring(1);
            if (offset.isEmpty()) return relative;
            try {
                return relative + Double.parseDouble(offset);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }

        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private Component prefix() {
        Component prefix = server.plugin().getPrefix();
        return prefix != null ? prefix : Component.empty();
    }
}
