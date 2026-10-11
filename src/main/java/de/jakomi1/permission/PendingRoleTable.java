package de.jakomi1.permission;

import de.jakomi1.database.Column;
import de.jakomi1.database.DataType;
import de.jakomi1.database.Table;
import de.jakomi1.database.TableSchema;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Offene Rollen-Vergaben fuer Spieler, die den Server noch nie betreten
 * haben.
 *
 * <p>Das Rollen-System speichert Rollen unter der UUID. Von einem noch nie da
 * gewesenen Spieler existiert aber keine UUID - gerade bei
 * {@code online-mode=true} ist sie erst nach dem ersten Login bekannt. Eine
 * solche Vergabe wird deshalb hier namentlich geparkt und beim ersten
 * Beitritt in eine echte UUID-Rolle umgewandelt.
 *
 * <p>Der Schluessel ist bewusst der kleingeschriebene Name: Minecraft-Namen
 * sind nicht case-sensitiv, die Vergabe soll also auf {@code "/role set
 * Zocker VIP"} genauso greifen wie auf den spaeteren Login als {@code zocker}.
 */
public class PendingRoleTable extends Table<String, String> {

    public static final String COLUMN_NAME = "player_name";
    public static final String COLUMN_ROLE = "role_name";

    public PendingRoleTable() {
        super(TableSchema.of(
                "pending_roles",
                Column.of(COLUMN_NAME, DataType.TEXT),
                Column.of(COLUMN_ROLE, DataType.TEXT)
        ));
    }

    /** @return null, wenn fuer den Namen keine offene Vergabe vorliegt */
    public Role pendingRole(String name) {
        if (name == null || name.isBlank()) return null;

        String stored = get(key(name));
        if (stored == null) return null;

        return Role.role(stored);
    }

    public void set(String name, Role role) {
        if (name == null || name.isBlank() || role == null) return;

        put(key(name), role.name());
        flushAsync();
    }

    /** @return true, wenn unter dem Namen eine offene Vergabe entfernt wurde */
    public boolean clear(String name) {
        if (name == null || name.isBlank()) return false;

        if (!containsKey(key(name))) return false;

        remove(key(name));
        flushAsync();
        return true;
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    @Override
    protected String readKey(ResultSet rs) throws SQLException {
        return rs.getString(COLUMN_NAME);
    }

    @Override
    protected int bindKey(PreparedStatement ps, int index, String key) throws SQLException {
        ps.setString(index, key);
        return index + 1;
    }

    @Override
    protected String readValue(ResultSet rs) throws SQLException {
        return rs.getString(COLUMN_ROLE);
    }

    @Override
    protected int bindValue(PreparedStatement ps, int index, String value) throws SQLException {
        ps.setString(index, value);
        return index + 1;
    }
}
