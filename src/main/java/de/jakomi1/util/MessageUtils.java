package de.jakomi1.util;

import de.jakomi1.project.ProjectPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)", Pattern.CASE_INSENSITIVE);

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

    /**
     * Baut eine Nachricht mit klickbaren http(s)-Links. Der Text selbst wird
     * als Klartext uebernommen und in Weiss gesetzt; Links erscheinen blau,
     * unterstrichen und lassen sich direkt im Browser oeffnen.
     */
    public static Component linkify(Component message) {
        return linkify(message == null
                ? ""
                : PlainTextComponentSerializer.plainText().serialize(message));
    }

    public static Component linkify(String plain) {
        if (plain == null || plain.isEmpty()) {
            return Component.empty();
        }

        Matcher matcher = URL_PATTERN.matcher(plain);
        Component result = Component.empty();
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String before = plain.substring(lastEnd, matcher.start());
                if (!before.isEmpty()) {
                    result = result.append(Component.text(before).color(NamedTextColor.WHITE));
                }
            }

            String url = matcher.group(1);
            result = result.append(Component.text(url)
                    .color(NamedTextColor.BLUE)
                    .decorate(TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.openUrl(url))
                    .hoverEvent(HoverEvent.showText(
                            Component.text("Klicke zum \u00D6ffnen").color(NamedTextColor.GRAY)
                    )));

            lastEnd = matcher.end();
        }

        if (lastEnd < plain.length()) {
            String after = plain.substring(lastEnd);
            if (!after.isEmpty()) {
                result = result.append(Component.text(after).color(NamedTextColor.WHITE));
            }
        }

        return result;
    }

    public static boolean isAdmin(Player player) {
        if (player == null) return false;
        if (player.isOp()) return true;
        return plugin != null && player.hasPermission(plugin.getName() + ".admin");
    }
}
