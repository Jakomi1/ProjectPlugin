package de.jakomi1.project.permission;

import de.jakomi1.database.Column;
import de.jakomi1.database.DataType;
import de.jakomi1.database.Table;
import de.jakomi1.database.TableSchema;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RoleTable extends Table<String, RoleTable.RoleEntry> {

    private static final String TABLE = "role_assignments";
    private static final String COL_UUID = "uuid";
    private static final String COL_ROLE = "role";
    private static final String COL_NAME = "name";
    private static final String COL_OVERRIDE = "override";

    public RoleTable() {
        super(TableSchema.of(
                TABLE,
                Column.of(COL_UUID, DataType.TEXT),
                Column.of(COL_ROLE, DataType.TEXT),
                Column.of(COL_NAME, DataType.TEXT),
                Column.of(COL_OVERRIDE, DataType.INTEGER)
        ));
    }

    public record RoleEntry(Role role, String name, boolean override) {
    }

    public record PlayerRole(Role role, String name) {
    }

    public Role getRole(UUID uuid) {
        RoleEntry entry = getEntry(uuid);
        return entry != null && entry.role() != null ? entry.role() : Role.MEMBER;
    }

    public String getName(UUID uuid) {
        RoleEntry entry = getEntry(uuid);
        return entry != null ? entry.name() : null;
    }

    public RoleEntry getEntry(UUID uuid) {
        return uuid == null ? null : get(uuid.toString());
    }

    public boolean hasRole(UUID uuid) {
        RoleEntry entry = getEntry(uuid);
        return entry != null && entry.role() != null;
    }

    public boolean isOverride(UUID uuid) {
        RoleEntry entry = getEntry(uuid);
        return entry != null && entry.override();
    }

    public boolean isRemoved(UUID uuid) {
        RoleEntry entry = getEntry(uuid);
        return entry != null && entry.override() && entry.role() == null;
    }

    public void assign(UUID uuid, Role role, String name) {
        if (uuid == null || role == null) return;

        put(uuid.toString(), new RoleEntry(role, name, true));
        flushNow();
    }

    public void block(UUID uuid) {
        if (uuid == null) return;

        put(uuid.toString(), new RoleEntry(null, null, true));
        flushNow();
    }

    public void setRole(UUID uuid, Role role, String name) {
        if (uuid == null || role == null) return;

        put(uuid.toString(), new RoleEntry(role, name, false));
        flushNow();
    }

    public void removeEntry(UUID uuid) {
        if (uuid == null) return;

        remove(uuid.toString());
        flushNow();
    }

    public void setName(UUID uuid, String name) {
        if (uuid == null || name == null) return;

        String key = uuid.toString();
        RoleEntry previous = get(key);
        if (previous == null) return;

        put(key, new RoleEntry(previous.role(), name, previous.override()));
        flushNow();
    }

    public void sync(Map<UUID, PlayerRole> supabaseRoles) {
        if (supabaseRoles == null) return;

        Set<String> seen = new HashSet<>();
        for (Map.Entry<UUID, PlayerRole> entry : supabaseRoles.entrySet()) {
            String key = entry.getKey().toString();
            seen.add(key);

            RoleEntry existing = get(key);
            if (existing != null && existing.override()) continue;

            PlayerRole playerRole = entry.getValue();
            put(key, new RoleEntry(playerRole.role(), playerRole.name(), false));
        }

        for (String key : new ArrayList<>(keys())) {
            RoleEntry existing = get(key);
            if (existing != null && !existing.override() && !seen.contains(key)) {
                remove(key);
            }
        }

        flushNow();
    }

    @Override
    protected String readKey(ResultSet rs) throws SQLException {
        return rs.getString(COL_UUID);
    }

    @Override
    protected int bindKey(PreparedStatement ps, int index, String key) throws SQLException {
        ps.setString(index, key);
        return index + 1;
    }

    @Override
    protected RoleEntry readValue(ResultSet rs) throws SQLException {
        Role role = Role.role(rs.getString(COL_ROLE));
        String name = rs.getString(COL_NAME);
        boolean override = rs.getInt(COL_OVERRIDE) == 1;
        return new RoleEntry(role, name, override);
    }

    @Override
    protected int bindValue(PreparedStatement ps, int index, RoleEntry value) throws SQLException {
        ps.setString(index, value.role() == null ? null : value.role().name());
        ps.setString(index + 1, value.name());
        ps.setInt(index + 2, value.override() ? 1 : 0);
        return index + 3;
    }
}
