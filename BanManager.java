package de.jakomi1.projectplugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BanManager {

    private BanManager() {
        // Utility-Klasse
    }

    public static boolean isBanned(java.util.UUID uuid) {
        return false;
    }

    public static boolean isChatBanned(java.util.UUID uuid) {
        return false;
    }

    public static void permanentBan(CommandSender sender, String targetName, String reason) {
    }

    public static void unban(CommandSender sender, String targetName) {
    }

    public static void tempBan(CommandSender sender, String targetName, String duration, String reason) {
    }

    public static void kick(CommandSender sender, String targetName, String reason) {
    }

    public static void chatBan(CommandSender sender, String targetName, String reason) {
    }

    public static void chatTempBan(CommandSender sender, String targetName, String duration, String reason) {
    }

    public static void chatUnban(CommandSender sender, String targetName) {
    }
}