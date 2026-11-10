package de.jakomi1.util;

import de.jakomi1.project.ProjectPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Allgemeine Nachrichten-Hilfsklasse fuer alle ProjectPlugin-basierten Plugins.
 *
 * <p>Das Prefix und der Logger werden aus der aktuell laufenden
 * {@link ProjectPlugin}-Instanz bezogen. {@link ProjectPlugin#onEnable()}
 * initialisiert die Klasse automatisch, sodass Subplugins keine eigene
 * MessageUtils-Kopie mehr benoetigen.</p>
 */
public final class MessageUtils {

    public static String playerSymbol = "\uD83D\uDC64";

    private static ProjectPlugin plugin;

    private MessageUtils() {
    }

    public static void init(ProjectPlugin projectPlugin) {
        plugin = projectPlugin;
    }

    private static Component prefix() {
        if (plugin == null) {
            return Component.empty();
        }
        Component prefix = plugin.getPrefix();
        return prefix == null ? Component.empty() : prefix;
    }

    private static void log(Component message) {
        if (plugin != null) {
            plugin.getLogger().info(ComponentUtils.plainText(message));
        }
    }

    public static void sendMessage(Player player, String text) {
        sendMessage(player, Component.text(text));
    }

    public static void sendMessage(List<Player> players, Component message) {
        if (players == null) return;
        players.forEach(player -> sendMessage(player, message));
    }

    public static void sendMessage(Player player, Component message) {
        if (player == null || message == null) return;
        player.sendMessage(prefix().append(message));
    }

    public static void sendMessageToAllAdmins(Component message) {
        if (message == null) return;
        Bukkit.getOnlinePlayers().stream()
                .filter(MessageUtils::isAdmin)
                .forEach(player -> player.sendMessage(prefix().append(message)));
    }

    public static void sendMessageToAllWithoutPrefix(Component message) {
        if (message == null) return;
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
        log(message);
    }

    public static void sendMessageToAll(Component message) {
        if (message == null) return;
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(prefix().append(message)));
        log(message);
    }

    public static void sendMessageToAllExceptFor(Component message, Player excluded) {
        if (message == null) return;
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> !player.equals(excluded))
                .forEach(player -> player.sendMessage(prefix().append(message)));
        log(message);
    }

    public static void sendUnstyledMessage(Player player, Component message) {
        if (player == null || message == null) return;
        player.sendMessage(message);
    }

    public static boolean isAdmin(Player player) {
        if (player == null) return false;
        if (player.isOp()) return true;
        return plugin != null && player.hasPermission(plugin.getName() + ".admin");
    }
}
