package de.jakomi1.project.terms;

import de.jakomi1.database.KeyValueTable;

import java.util.UUID;

/**
 * Speichert pro Spieler, ob die Nutzungsbedingungen bereits akzeptiert
 * wurden. Sobald ein Eintrag existiert, wird der Zustimmungsscreen beim
 * nächsten Join nicht mehr angezeigt.
 */
public final class TermsTable extends KeyValueTable<UUID, String> {

    private static TermsTable instance;

    public TermsTable() {
        super("terms_acceptance", UUID.class, String.class, "player_uuid", "player_name");
        instance = this;
    }

    public static TermsTable get() {
        return instance;
    }

    public boolean hasAccepted(UUID uniqueId) {
        return uniqueId != null && containsKey(uniqueId);
    }

    public void accept(UUID uniqueId, String playerName) {
        if (uniqueId == null) return;

        put(uniqueId, playerName == null ? "accepted" : playerName);
        flushNow();
    }

    public void revoke(UUID uniqueId) {
        if (uniqueId == null) return;

        remove(uniqueId);
        flushNow();
    }

    public void clearAll() {
        for (UUID uniqueId : keys()) {
            remove(uniqueId);
        }
        flushNow();
    }

    public int acceptedCount() {
        return size();
    }
}
