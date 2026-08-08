package de.jakomi1.database.table;

import de.jakomi1.database.Entry;
import de.jakomi1.database.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class GlobalSettingsTable extends Table<String> {

    private static GlobalSettingsTable instance;

    public GlobalSettingsTable() {
        super("global_settings_table");
        if (instance != null) {
            throw new IllegalStateException("GlobalSettingsTable wurde bereits instanziiert.");
        }
        instance = this;
    }

    public static GlobalSettingsTable get() {
        return instance;
    }

    public void ensureInt(String key, int value) {
        if (get(key) == null) {
            put(new Entry<>(key, String.valueOf(value)));
            flushNow();
        }
    }

    public int getInt(String key, int fallback) {
        Entry<String> entry = get(key);
        if (entry == null) return fallback;

        try {
            return Integer.parseInt(entry.value());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public void setInt(String key, int value) {
        put(new Entry<>(key, String.valueOf(value)));
        flushNow();
    }

    public String getString(String key, String fallback) {
        Entry<String> entry = get(key);
        return entry == null ? fallback : entry.value();
    }

    public void setString(String key, String value) {
        put(new Entry<>(key, value));
        flushNow();
    }

    public boolean getBoolean(String key, boolean fallback) {
        String value = get(key) == null ? null : get(key).value();
        if (value == null) return fallback;

        return switch (value.toLowerCase()) {
            case "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;
            default -> fallback;
        };
    }

    public void setBoolean(String key, boolean value) {
        put(new Entry<>(key, Boolean.toString(value)));
        flushNow();
    }

    @Override
    protected String columns() {
        return "key TEXT PRIMARY KEY, value TEXT";
    }

    @Override
    protected Entry<String> read(ResultSet rs) throws SQLException {
        return new Entry<>(rs.getString("key"), rs.getString("value"));
    }

    @Override
    protected void write(Connection connection, Entry<String> value) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO global_settings_table(key, value)
            VALUES(?, ?)
            ON CONFLICT(key) DO UPDATE SET
                value = excluded.value
        """)) {
            ps.setString(1, value.key());
            ps.setString(2, value.value());
            ps.executeUpdate();
        }
    }

    @Override
    protected void delete(Connection connection, String key) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
            DELETE FROM global_settings_table
            WHERE key = ?
        """)) {
            ps.setString(1, key);
            ps.executeUpdate();
        }
    }
}
