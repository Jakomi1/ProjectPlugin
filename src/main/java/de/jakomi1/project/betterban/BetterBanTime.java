package de.jakomi1.project.betterban;

import java.util.Locale;

/**
 * Zeit-Hilfen fuer betterban.
 *
 * <p>Dauern werden in der Kurzform {@code 10m}, {@code 2h}, {@code 1d} oder
 * {@code 1w} angegeben. {@link #formatDuration(long)} liefert eine deutsche
 * lesbare Darstellung.</p>
 */
public final class BetterBanTime {

    private BetterBanTime() {
    }

    /**
     * Wandelt eine Kurzform in Millisekunden um.
     *
     * @throws IllegalArgumentException bei ungueltiger Eingabe
     */
    public static long parseDuration(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Dauer fehlt");
        }

        String value = input.trim().toLowerCase(Locale.ROOT);

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Dauer fehlt");
        }

        char unit = value.charAt(value.length() - 1);
        String number = value.substring(0, value.length() - 1);

        long multiplier;

        switch (unit) {
            case 's':
                multiplier = 1_000L;
                break;
            case 'm':
                multiplier = 60_000L;
                break;
            case 'h':
                multiplier = 3_600_000L;
                break;
            case 'd':
                multiplier = 86_400_000L;
                break;
            case 'w':
                multiplier = 604_800_000L;
                break;
            default:
                throw new IllegalArgumentException("Unbekannte Einheit");
        }

        long amount;

        try {
            amount = Long.parseLong(number);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Ungueltige Zahl", exception);
        }

        if (amount <= 0L) {
            throw new IllegalArgumentException("Dauer muss positiv sein");
        }

        return amount * multiplier;
    }

    /** Deutsche Darstellung einer Dauer, z.B. {@code 1 Tag 2 Stunden 3 Minuten}. */
    public static String formatDuration(long millis) {
        if (millis <= 0L) {
            return "0 Sekunden";
        }

        long totalSeconds = millis / 1_000L;
        long days = totalSeconds / 86_400L;
        long hours = (totalSeconds % 86_400L) / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;

        StringBuilder builder = new StringBuilder();

        append(builder, days, "Tag", "Tage");
        append(builder, hours, "Stunde", "Stunden");
        append(builder, minutes, "Minute", "Minuten");
        append(builder, seconds, "Sekunde", "Sekunden");

        if (builder.length() == 0) {
            return "0 Sekunden";
        }

        return builder.toString().trim();
    }

    private static void append(StringBuilder builder, long amount, String singular, String plural) {
        if (amount <= 0L) {
            return;
        }

        if (builder.length() > 0) {
            builder.append(' ');
        }

        builder.append(amount).append(' ').append(amount == 1L ? singular : plural);
    }
}
