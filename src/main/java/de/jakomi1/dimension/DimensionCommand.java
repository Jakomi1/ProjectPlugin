package de.jakomi1.dimension;

import de.jakomi1.command.CustomCommand;
import de.jakomi1.project.ProjectServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class DimensionCommand implements CustomCommand {

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
        return "Teleportiert Spieler in beliebige Dimensionen";
    }

    @Override
    public String usage() {
        return "/dimension <dimension> teleport <x> <y> <z>";
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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix().append(Component.text("Dieser Befehl ist nur für Spieler.", NamedTextColor.RED)));
            return true;
        }

        if (args.length < 5) {
            sender.sendMessage(prefix().append(Component.text(usage(), NamedTextColor.RED)));
            return true;
        }

        String dimension = args[0];
        if (!"teleport".equalsIgnoreCase(args[1])) {
            sender.sendMessage(prefix().append(Component.text("Unbekannter Unterbefehl '" + args[1] + "'.", NamedTextColor.RED)));
            return true;
        }

        double x = parseCoordinate(args[2], player.getLocation().getX());
        double y = parseCoordinate(args[3], player.getLocation().getY());
        double z = parseCoordinate(args[4], player.getLocation().getZ());

        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
            sender.sendMessage(prefix().append(Component.text("Ungültige Koordinaten.", NamedTextColor.RED)));
            return true;
        }

        try {
            manager.teleport(player, dimension, x, y, z);
            sender.sendMessage(prefix().append(Component.text("Teleportiere nach " + dimension + " (" + x + ", " + y + ", " + z + ") ...", NamedTextColor.GRAY)));
        } catch (IllegalStateException e) {
            sender.sendMessage(prefix().append(Component.text(e.getMessage(), NamedTextColor.RED)));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.addAll(manager.keys());
            suggestions.add("minecraft:overworld");
            suggestions.add("minecraft:the_nether");
            suggestions.add("minecraft:the_end");
            return suggestions;
        }
        if (args.length == 2) {
            return List.of("teleport");
        }
        if (args.length == 3 || args.length == 4 || args.length == 5) {
            return List.of("~");
        }
        return List.of();
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