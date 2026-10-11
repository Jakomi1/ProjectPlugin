package de.jakomi1.project.combat;

/**
 * Textbausteine des PvP-Systems.
 *
 * <p>Die Library liefert nur neutrale Standardtexte; jede Meldung laesst sich
 * ueber {@link CombatManager#messages(java.util.function.BiFunction)}
 * ueberschreiben. So bleiben Farben, Praefixe und Sprache beim Aufrufer.
 */
public enum PvpMessage {

    /** Schaden in einer geschuetzten Zone (etwa Spawn). */
    SPAWN_PROTECTED,

    /** Der Angreifer hat PvP deaktiviert. */
    SELF_PVP_OFF,

    /** Das Ziel hat PvP deaktiviert. */
    TARGET_PVP_OFF,

    /** Actionbar-Hinweis nach dem Tod. */
    DEATH_DISABLED,

    /** PvP wurde eingeschaltet. */
    TOGGLE_ENABLED,

    /** PvP wurde ausgeschaltet. */
    TOGGLE_DISABLED,

    /** Aenderung ist noch gesperrt. */
    TOGGLE_LOCKED,

    /** Befehl von der Konsole aus aufgerufen. */
    PLAYERS_ONLY,

    /** Falsche oder fehlende Argumente. */
    USAGE
}
