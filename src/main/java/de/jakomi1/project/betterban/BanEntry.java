package de.jakomi1.project.betterban;

/**
 * Ein gespeicherter Ban-Eintrag.
 *
 * @param endTimestamp Ablaufzeit in Millisekunden oder {@code -1} fuer permanent
 * @param reason       optionaler Grund
 */
public record BanEntry(long endTimestamp, String reason) {

    public static final long PERMANENT = -1L;

    public boolean permanent() {
        return endTimestamp == PERMANENT;
    }

    public boolean expired(long now) {
        return !permanent() && endTimestamp <= now;
    }
}
