package de.jakomi1.player;

import de.jakomi1.database.Table;
import de.jakomi1.database.table.SkinTable;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.project.combat.DamageAttribution;
import de.jakomi1.project.pearl.PearlFixTable;
import de.jakomi1.project.playtime.PlaytimeTable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;

/**
 * Loescht die Daten einer Saison, wenn der Server gestoppt wird.
 *
 * <p>Bewusst erhalten bleiben:
 * <ul>
 *     <li>die Whitelist - sonst koennte nach dem Stopp niemand mehr herein,
 *         auch der Owner nicht</li>
 *     <li>die Rollenzuweisungen - das Team bleibt auch bei geschlossenem
 *         Server in der Lage, den Server wieder zu starten</li>
 *     <li>die globalen Einstellungen und der Serverzustand, sonst waere der
 *         Zustand STOPPED beim naechsten Start verloren</li>
 * </ul>
 *
 * <p>Geloescht werden dagegen alles, was sich pro Saison sammelt: Spielzeit,
 * akzeptierte Nutzungsbedingungen, gespeicherte Skins, der Endbperlenzustand
 * und das Befehls-Protokoll. Dazu kommen die Angaben im
 * PersistentDataContainer der gerade online Spieler, damit niemand mit einem
 * gespeicherten Bett oder einer laufenden PvP-Sperre in die neue Saison
 * startet.
 */
public final class PlayerData {

    private PlayerData() {
    }

    /**
     * Loescht alle Spielerdaten ausser Whitelist, Rollen und Serverzustand.
     *
     * @return Anzahl der geloeschten Datenbankeintraege
     */
    public static int wipe(ProjectServer server) {
        if (server == null) return 0;

        int removed = 0;

        removed += server.terms().clearAll();
        removed += clear(server.plugin().getDatabase().getTable(PlaytimeTable.class));
        removed += clear(server.plugin().getDatabase().getTable(SkinTable.class));
        removed += clear(server.plugin().getDatabase().getTable(PearlFixTable.class));
        removed += clear(server.commandLog().table());

        wipeOnlinePlayers(server);

        server.plugin().getDatabase().flushAll();

        return removed;
    }

    /**
     * Setzt die Laufzeit-Daten der online Spieler zurueck. Erwartet wird der
     * Zustand STOPPED, in dem nur noch Owner und Admin online sind.
     */
    private static void wipeOnlinePlayers(ProjectServer server) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            server.respawn().forget(player);
            server.combat().deactivatePvp(player);
            DamageAttribution.forget(player);
        }
    }

    /**
     * Leert eine Tabelle, falls sie ueberhaupt registriert ist.
     *
     * @return Anzahl der geloeschten Eintraege
     */
    private static <K, V> int clear(Table<K, V> table) {
        if (table == null) return 0;

        int removed = table.size();
        if (removed == 0) return 0;

        // Kopie, weil waehrend der Schleife aus der Tabelle entfernt wird.
        for (K key : new ArrayList<>(table.keys())) {
            table.remove(key);
        }

        return removed;
    }
}
