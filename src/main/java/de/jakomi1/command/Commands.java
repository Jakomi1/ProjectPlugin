package de.jakomi1.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Commands {

    private Commands() {
    }

    public static @NotNull Component message(String text) {
        return Component.text(text, NamedTextColor.RED);
    }

    public static @Nullable Player player(@NotNull CommandSender sender) {
        return sender instanceof Player player ? player : null;
    }

    public static boolean requirePlayer(@NotNull CommandSender sender) {
        return requirePlayer(sender, Component.text(
                "Dieser Befehl ist nur für Spieler.",
                NamedTextColor.RED
        ));
    }

    public static boolean requirePlayer(@NotNull CommandSender sender, @NotNull Component message) {
        if (sender instanceof Player) return true;
        sender.sendMessage(message);
        return false;
    }

    public static boolean requirePermission(@NotNull CommandSender sender, @Nullable String permission) {
        return requirePermission(sender, permission, Component.text(
                "Dazu hast du keine Berechtigung!",
                NamedTextColor.RED
        ));
    }

    public static boolean requirePermission(@NotNull CommandSender sender, @Nullable String permission,
                                            @NotNull Component message) {
        if (permission == null || permission.isEmpty()) return true;
        if (sender.hasPermission(permission)) return true;

        sender.sendMessage(message);
        return false;
    }

    public static boolean requireArgs(@NotNull CommandSender sender, String[] args, int required,
                                      @NotNull Component usage) {
        if (args != null && args.length >= required) return true;

        sender.sendMessage(usage);
        return false;
    }
}
