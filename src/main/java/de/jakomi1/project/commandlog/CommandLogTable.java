package de.jakomi1.project.commandlog;

import de.jakomi1.database.Column;
import de.jakomi1.database.DataType;
import de.jakomi1.database.Table;
import de.jakomi1.database.TableSchema;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Persistiertes Befehls-Protokoll.
 *
 * <p>Jeder Eintrag haelt fest, wer wann welchen Befehl abgeschickt hat. Der
 * Schluessel ist aus Zeitstempel und Ausfuehrer zusammengesetzt und damit
 * eindeutig genug, ohne dass eine zusaetzliche ID vergeben werden muss.
 */
public final class CommandLogTable extends Table<String, CommandLogTable.CommandLogEntry> {

    public CommandLogTable() {
        super(TableSchema.of("command_log",
                Column.of("log_key", DataType.TEXT),
                Column.of("player", DataType.TEXT),
                Column.of("command", DataType.TEXT),
                Column.of("timestamp", DataType.INTEGER)));
    }

    @Override
    public String readKey(ResultSet rs) throws SQLException {
        return rs.getString("log_key");
    }

    @Override
    public int bindKey(PreparedStatement ps, int index, String key) throws SQLException {
        ps.setString(index, key);
        return index + 1;
    }

    @Override
    public CommandLogEntry readValue(ResultSet rs) throws SQLException {
        String player = rs.getString("player");
        String command = rs.getString("command");
        long timestamp = rs.getLong("timestamp");
        return new CommandLogEntry(player, command, timestamp);
    }

    @Override
    public int bindValue(PreparedStatement ps, int index, CommandLogEntry value) throws SQLException {
        ps.setString(index, value.player());
        ps.setString(index + 1, value.command());
        ps.setLong(index + 2, value.timestamp());
        return index + 3;
    }

    /**
     * @param player    Name des Ausfuehrers oder {@code CONSOLE}
     * @param command   der vollstaendige Befehl inklusive fuehrendem Schraegstrich
     * @param timestamp Zeitpunkt der Ausfuehrung in Millisekunden
     */
    public record CommandLogEntry(String player, String command, long timestamp) {
    }
}
