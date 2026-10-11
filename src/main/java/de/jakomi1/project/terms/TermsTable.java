package de.jakomi1.project.terms;

import de.jakomi1.database.KeyValueTable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Speichert pro Spieler, ob die Nutzungsbedingungen akzeptiert wurden. Sobald
 * ein Eintrag existiert, wird die Zustimmung beim naechsten Join nicht erneut
 * abgefragt.
 *
 * <p>Der UUID-Key wird bewusst als {@code TEXT} in Kleinschreibung gespeichert
 * und nicht als natives {@link UUID}-Objekt gebunden: der SQLite-Treiber kann
 * UUID-Objekte nicht zuverlaessig zuruecklesen, wodurch die Zustimmung nach
 * jedem Neustart verloren ginge.
 *
 * <p>Eine aeltere Plugin-Version hatte die Spalten {@code uuid} und
 * {@code name} angelegt. {@code CREATE TABLE IF NOT EXISTS} aendert eine
 * bereits vorhandene Tabelle nicht, und ein Lesen mit falschen Spaltennamen
 * schlaegt still fehl - die Zustimmung waere nach jedem Neustart wieder weg.
 * {@link #onInitialize()} benennt solche Altbestandsspalten deshalb um, bevor
 * die Tabelle gelesen wird.
 */
public final class TermsTable extends KeyValueTable<String, String> {

    public static final String COLUMN_UUID = "player_uuid";
    public static final String COLUMN_NAME = "player_name";

    private static TermsTable instance;

    public TermsTable() {
        super("terms_acceptance", String.class, String.class, COLUMN_UUID, COLUMN_NAME);

        instance = this;
    }

    public static TermsTable get() {
        return instance;
    }

    public boolean hasAccepted(UUID uniqueId) {
        return uniqueId != null && containsKey(key(uniqueId));
    }

    public void accept(UUID uniqueId, String playerName) {
        if (uniqueId == null) return;

        put(key(uniqueId), playerName == null ? "accepted" : playerName);
        flushAsync();
    }

    public void revoke(UUID uniqueId) {
        if (uniqueId == null) return;

        remove(key(uniqueId));
        flushAsync();
    }

    public void clearAll() {
        if (size() == 0) return;

        for (String entry : keys()) {
            remove(entry);
        }

        flushAsync();
    }

    public int acceptedCount() {
        return size();
    }

    /**
     * Spalten eines Altbestands umbenennen, bevor die Tabelle gelesen wird.
     * Fehlt die Tabelle komplett, wird sie gleich darueber normal angelegt.
     */
    @Override
    protected void onInitialize() {
        Set<String> columns = existingColumns();

        if (columns.isEmpty()) return;

        rename(columns, "uuid", COLUMN_UUID);
        rename(columns, "name", COLUMN_NAME);
    }

    private Set<String> existingColumns() {
        Set<String> columns = new HashSet<>();

        var connection = connection();
        if (connection == null) return columns;

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA table_info(" + schema().tableName() + ")")) {

            while (rs.next()) {
                columns.add(rs.getString("name").toLowerCase(Locale.ROOT));
            }

        } catch (SQLException ignored) {
        }

        return columns;
    }

    private void rename(Set<String> columns, String from, String to) {
        if (!columns.contains(from) || columns.contains(to)) return;

        var connection = connection();
        if (connection == null) return;

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + schema().tableName()
                    + " RENAME COLUMN " + from + " TO " + to);
        } catch (SQLException ignored) {
        }
    }

    private static String key(UUID uniqueId) {
        return uniqueId.toString().toLowerCase(Locale.ROOT);
    }
}
