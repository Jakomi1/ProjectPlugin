package de.jakomi1.project.playtime;

import de.jakomi1.database.Column;
import de.jakomi1.database.DataType;
import de.jakomi1.database.Table;
import de.jakomi1.database.TableSchema;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistierte Spielzeit aller Spieler.
 *
 * <p>Schluessel ist die UUID, damit ein Namenswechsel keinen neuen Eintrag
 * erzeugt und {@code /playtime} auch offline verlaesslich funktioniert. Der
 * Name liegt als eigene Spalte daneben und dient ausschliesslich der
 * Anzeige und den Tab-Vorschlaegen.
 *
 * <p>Die Tabelle haelt ihren Inhalt bereits im Speicher, Abfragen kosten
 * deshalb keinen Datenbankzugriff.
 */
public final class PlaytimeTable extends Table<UUID, PlaytimeTable.PlaytimeEntry> {

    /** Cache fuer die Namenssuche, damit Tab-Vorschlaege nicht linear sind. */
    private static final Map<String, UUID> idsByName = new ConcurrentHashMap<>();

    public PlaytimeTable() {
        super(TableSchema.of("playtime",
                Column.of("player_uuid", DataType.TEXT),
                Column.of("player_name", DataType.TEXT),
                Column.of("seconds", DataType.INTEGER),
                Column.of("last_seen", DataType.INTEGER)));
    }

    @Override
    public UUID readKey(ResultSet rs) throws SQLException {
        return UUID.fromString(rs.getString("player_uuid"));
    }

    @Override
    public int bindKey(PreparedStatement ps, int index, UUID key) throws SQLException {
        ps.setString(index, key.toString());
        return index + 1;
    }

    @Override
    public PlaytimeEntry readValue(ResultSet rs) throws SQLException {
        return new PlaytimeEntry(
                rs.getString("player_name"),
                rs.getLong("seconds"),
                rs.getLong("last_seen")
        );
    }

    @Override
    public int bindValue(PreparedStatement ps, int index, PlaytimeEntry value) throws SQLException {
        ps.setString(index, value.name());
        ps.setLong(index + 1, value.seconds());
        ps.setLong(index + 2, value.lastSeen());
        return index + 3;
    }

    @Override
    protected void onLoaded(UUID key, PlaytimeEntry value) {
        indexName(key, value.name());
    }

    @Override
    protected void onPut(UUID key, PlaytimeEntry value, PlaytimeEntry previous) {
        if (previous != null && previous.name() != null) {
            // Bei einem Namenswechsel darf der alte Name nicht weiter
            // auf die UUID zeigen, sonst liefert /playtime den falschen.
            idsByName.remove(previous.name().toLowerCase(Locale.ROOT));
        }

        indexName(key, value.name());
    }

    @Override
    protected void onRemoved(UUID key, PlaytimeEntry removed) {
        if (removed.name() == null) return;

        idsByName.remove(removed.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Spielzeit eines Spielers, oder null wenn er nie auf dem Server war.
     */
    public PlaytimeEntry entry(String name) {
        if (name == null || name.isBlank()) return null;

        UUID uuid = idsByName.get(name.toLowerCase(Locale.ROOT));
        if (uuid == null) return null;

        return get(uuid);
    }

    /** Ist der Name ueberhaupt bekannt? Klauft nicht die ganze Liste ab. */
    public boolean hasName(String name) {
        if (name == null || name.isBlank()) return false;

        return idsByName.containsKey(name.toLowerCase(Locale.ROOT));
    }

    private static void indexName(UUID uuid, String name) {
        if (uuid == null || name == null || name.isBlank()) return;

        idsByName.put(name.toLowerCase(Locale.ROOT), uuid);
    }

    /**
     * @param name     Anzeigename
     * @param seconds  Spielzeit in Sekunden
     * @param lastSeen Zeitpunkt des letzten Besuchs in Millisekunden
     */
    public record PlaytimeEntry(String name, long seconds, long lastSeen) {
    }
}
