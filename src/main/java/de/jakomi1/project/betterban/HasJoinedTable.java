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
 * Merkt sich, welche Spieler den Server bereits betreten haben. Nur so kann
 * ein Ban auch fuer einen aktuell unbekannten Namen eindeutig zugeordnet
 * werden.
 */
public final class HasJoinedTable extends Table<UUID, String> {

    public HasJoinedTable() {
        super(TableSchema.of(
                "has_joined",
                Column.of("uuid", DataType.TEXT),
                Column.of("name", DataType.TEXT)
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
    protected String readValue(ResultSet rs) throws SQLException {
        return rs.getString("name");
    }

    @Override
    protected int bindValue(PreparedStatement ps, int index, String value) throws SQLException {
        ps.setString(index, value);
        return index + 1;
    }
}
