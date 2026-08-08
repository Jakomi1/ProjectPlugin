package de.jakomi1.database.table;

import de.jakomi1.database.KeyValueTable;
import de.jakomi1.project.state.ServerState;

public final class GlobalSettingsTable extends KeyValueTable<String, String> {

    private static final String SERVER_STATE_KEY = "server_state";

    public GlobalSettingsTable() {
        super("global_settings_table", String.class, String.class);
    }

    public ServerState getServerState() {
        String value = getString(SERVER_STATE_KEY, ServerState.STOPPED.name());

        for (ServerState state : ServerState.values()) {
            if (state.name().equalsIgnoreCase(value)) {
                return state;
            }
        }

        return ServerState.STOPPED;
    }

    public void setServerState(ServerState state) {
        setString(SERVER_STATE_KEY, state.name());
    }

    public void advanceServerState() {
        setServerState(getServerState().next());
    }

    public void ensureInt(String key, int value) {
        if (get(key) == null) {
            setInt(key, value);
        }
    }

    public int getInt(String key, int fallback) {
        String value = get(key);
        if (value == null) return fallback;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public void setInt(String key, int value) {
        setString(key, String.valueOf(value));
    }

    public String getString(String key, String fallback) {
        String value = get(key);
        return value == null ? fallback : value;
    }

    public void setString(String key, String value) {
        put(key, value);
        flushNow();
    }

    public boolean getBoolean(String key, boolean fallback) {
        String value = get(key);
        if (value == null) return fallback;

        return switch (value.toLowerCase()) {
            case "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;
            default -> fallback;
        };
    }

    public void setBoolean(String key, boolean value) {
        setString(key, Boolean.toString(value));
    }
}
