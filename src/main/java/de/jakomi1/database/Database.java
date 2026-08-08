package de.jakomi1.database;

import de.jakomi1.project.ProjectPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Database {

    private final ProjectPlugin plugin;
    private Connection connection;
    private final List<Table<?>> tables = new ArrayList<>();
    private final Map<Class<? extends Table<?>>, Table<?>> tableRegistry = new HashMap<>();
    private File pluginFolder;

    public Database(ProjectPlugin plugin) {
        this.plugin = plugin;
        try {
            pluginFolder = plugin.getDataFolder();
            if (!pluginFolder.exists()) pluginFolder.mkdirs();
            File file = new File(pluginFolder, "database.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
                st.execute("PRAGMA synchronous = NORMAL");
                st.execute("PRAGMA busy_timeout = 5000");
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe(exception.getMessage());
        }
    }

    void register(Table<?> table) {
        Class<? extends Table<?>> clazz = tableClass(table);

        if (tableRegistry.containsKey(clazz)) {
            throw new IllegalStateException(
                    "Table already registered: " + clazz.getSimpleName()
            );
        }

        table.initialize(this);
        tables.add(table);
        tableRegistry.put(clazz, table);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Table<?>> tableClass(Table<?> table) {
        return (Class<? extends Table<?>>) table.getClass();
    }


    @SuppressWarnings("unchecked")
    public <T extends Table<?>> T getTable(Class<T> clazz) {
        Table<?> table = tableRegistry.get(clazz);
        if (table == null) throw new IllegalArgumentException("Table not registered: " + clazz.getSimpleName());
        return (T) table;
    }

    public Connection connection() {
        return connection;
    }

    public File pluginFolder() {
        return pluginFolder;
    }

    public void flushAll() {
        for (Table<?> table : tables) table.flushNow();
    }

    public void shutdown() {
        try {
            for (Table<?> table : tables) table.onShutdown();
            flushAll();
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Fehler beim Shutdown der Datenbank: " + exception.getMessage());
        }
    }
}
