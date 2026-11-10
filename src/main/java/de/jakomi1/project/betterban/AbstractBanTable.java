package de.jakomi1.project.betterban;

import de.jakomi1.database.Column;
import de.jakomi1.database.DataType;
import de.jakomi1.database.Table;
import de.jakomi1.database.TableSchema;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Gemeinsame Tabellen-Implementierung fuer Ban-, ChatBan- und VoiceBan-Eintraege.
 *
 * <p>Alle drei Tabellen haben dasselbe Schema: {@code uuid},
 * {@code end_timestamp} und {@code reason}. Die UUID wird bewusst als Text
 * gebunden, damit die Klasse unabhaengig von der UUID-Unterstuetzung des
 * jeweiligen JDBC-Treibers funktioniert.</p>
 */
abstract class AbstractBanTable extends Table<UUID, BanEntry> {

    protected AbstractBanTable(String tableName) {
        super(TableSchema.of(
                tableName,
                Column.of("uuid", DataType.TEXT),
                Column.of("end_timestamp", DataType.INTEGER),
                Column.of("reason", DataType.TEXT)
        ));
    }

    @Override
    protected UUID readKey(ResultSet rs) throws SQLException {
        return UUID.fromString(rs.getString("uuid"));
    }

    @Override
    protected int bindKey(PreparedStatement ps, int index, UUID key) throws SQLException {
        ps.setString(index, key.toString());
        return index + 1;
    }

    @Override
    protected BanEntry readValue(ResultSet rs) throws SQLException {
        return new BanEntry(rs.getLong("end_timestamp"), rs.getString("reason"));
    }

    @Override
    protected int bindValue(PreparedStatement ps, int index, BanEntry value) throws SQLException {
        ps.setLong(index++, value.endTimestamp());
        ps.setString(index++, value.reason());
        return index;
    }
}
