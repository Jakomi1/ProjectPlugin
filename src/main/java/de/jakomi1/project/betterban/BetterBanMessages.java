package de.jakomi1.project.betterban;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Deutsche, hartkodierte Nachrichten fuer betterban.
 *
 * <p>Die Klasse haelt nur Texte und Farben. Das Prefix kommt vom
 * {@link de.jakomi1.project.ProjectPlugin} und wird einmal beim Aktivieren
 * uebergeben.</p>
 */
public final class BetterBanMessages {

    private final Component prefix;

    public BetterBanMessages(Component prefix) {
        this.prefix = prefix == null ? Component.empty() : prefix;
    }

    public Component prefix() {
        return prefix;
    }

    /** Graue Hinweiszeile mit Prefix. */
    public Component info(String text) {
        return prefix.append(Component.text(text == null ? "" : text, NamedTextColor.GRAY));
    }

    /** Gelbe Erfolgszeile mit Prefix. */
    public Component success(String text) {
        return prefix.append(Component.text(text == null ? "" : text, NamedTextColor.YELLOW));
    }

    /** Rote Fehlerzeile mit Prefix. */
    public Component error(String text) {
        return prefix.append(Component.text(text == null ? "" : text, NamedTextColor.RED));
    }

    /** Neutraler Text ohne Prefix. */
    public Component plain(String text, NamedTextColor color) {
        return Component.text(text == null ? "" : text, color == null ? NamedTextColor.GRAY : color);
    }

    /** Nachricht, die einem gebannten Spieler beim Login gezeigt wird. */
    public Component banMessage(BanEntry entry) {
        if (entry == null) {
            return error("Du bist nicht gebannt.");
        }

        Component message = entry.permanent()
                ? error("Du wurdest permanent gebannt.")
                : error("Du wurdest fuer " + BetterBanTime.formatDuration(remaining(entry)) + " gebannt.");

        return appendReason(message, entry.reason());
    }

    /** Nachricht, die ein chat-gebrannter Spieler beim Sprechen sieht. */
    public Component chatBanMessage(BanEntry entry) {
        if (entry == null) {
            return error("Du bist nicht aus dem Chat gebannt.");
        }

        Component message = entry.permanent()
                ? error("Du wurdest permanent aus dem Chat gebannt.")
                : error("Du wurdest fuer " + BetterBanTime.formatDuration(remaining(entry)) + " aus dem Chat gebannt.");

        return appendReason(message, entry.reason());
    }

    /** Nachricht, die ein voice-gebrannter Spieler als Actionbar sieht. */
    public Component voiceBanMessage(BanEntry entry) {
        if (entry == null) {
            return error("Du bist nicht aus dem Voicechat gebannt.");
        }

        Component message = entry.permanent()
                ? error("Du wurdest permanent aus dem Voicechat gebannt.")
                : error("Du wurdest fuer " + BetterBanTime.formatDuration(remaining(entry)) + " aus dem Voicechat gebannt.");

        return appendReason(message, entry.reason());
    }

    /** Kick-Nachricht fuer {@code /kick}. */
    public Component kickMessage(String reason) {
        Component message = error("Du wurdest vom Server gekickt.");
        return appendReason(message, reason);
    }

    private Component appendReason(Component message, String reason) {
        if (reason == null || reason.isBlank()) {
            return message;
        }

        return message.append(Component.newline())
                .append(Component.text("Grund: " + reason, NamedTextColor.GRAY));
    }

    private static long remaining(BanEntry entry) {
        if (entry.permanent()) {
            return 0L;
        }

        return Math.max(entry.endTimestamp() - System.currentTimeMillis(), 0L);
    }
}
