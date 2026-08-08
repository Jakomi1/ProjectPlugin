package de.jakomi1.project;

/**
 * Einheitlicher Lebenszyklus für alle Feature-/Server-Manager der Library.
 *
 * Jeder Manager folgt dem gleichen API-Stil:
 * <pre>
 *   server.roles().enable();            // Aktivieren (idempotent, flüssig)
 *   server.roles().isEnabled();         // Zustand abfragen
 *   server.roles().disable();           // Deaktivieren (Timer/Listener räumen auf)
 * </pre>
 *
 * Konkrete Manager überschreiben {@link #enable()} mit kovariantem Rückgabetyp,
 * damit die flüssige Konfiguration typisiert bleibt.
 */
public interface Manager {

    /**
     * Aktiviert den Manager. Idempotent – wiederholte Aufrufe sind unkritisch.
     */
    Manager enable();

    /**
     * Deaktiviert den Manager und räumt Ressourcen auf (Timer, Listener).
     */
    default void disable() {
    }

    /**
     * Gibt zurück, ob der Manager aktuell aktiv ist.
     */
    boolean isEnabled();
}
