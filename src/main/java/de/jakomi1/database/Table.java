package de.jakomi1.database;

import de.jakomi1.project.ProjectPlugin;
import de.jakomi1.project.Registerable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Table<K> implements Registerable {

    protected final Map<K, Entry<K>> cache = new ConcurrentHashMap<>();
    private final Set<K> dirtyKeys = ConcurrentHashMap.newKeySet();
    private final Set<K> removedKeys = ConcurrentHashMap.newKeySet();

    private final String tableName;
    private Database database;

    protected Table(String tableName) {
        this.tableName = tableName;
    }

    final void initialize(Database database) {
        this.database = database;
        update();
        createTable();
        loadAll();
    }

    @Override
    public void handleRegister(ProjectPlugin plugin) {
        plugin.getDatabase().register(this);
    }

    protected final Connection connection() {
        return database.connection();
    }

    protected final String tableName() {
        return tableName;
    }

    private void createTable() {
        try (Statement statement = connection().createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS %s (
                        %s
                    )
                    """.formatted(tableName, columns()));
        } catch (SQLException ignored) {
        }
    }

    private void loadAll() {
        try (Statement statement = connection().createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM " + tableName)) {

            while (rs.next()) {
                try {
                    Entry<K> entry = read(rs);
                    cache.put(entry.key(), entry);
                    onLoaded(entry.key(), entry);
                } catch (SQLException ignored) {
                }
            }

        } catch (SQLException ignored) {
        }
    }

    public final Entry<K> get(K key) {
        return cache.get(key);
    }

    public final void put(Entry<K> entry) {
        K key = entry.key();
        Entry<K> previous = cache.put(key, entry);
        dirtyKeys.add(key);
        removedKeys.remove(key);
        onPut(key, entry, previous);
    }

    public final void remove(K key) {
        Entry<K> removed = cache.remove(key);
        if (removed != null) {
            dirtyKeys.remove(key);
            removedKeys.add(key);
            onRemoved(key, removed);
        }
    }

    public final void flushNow() {
        Connection connection = connection();

        try {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            for (K key : dirtyKeys) {
                Entry<K> entry = cache.get(key);
                if (entry != null) {
                    try {
                        write(connection, entry);
                    } catch (SQLException ignored) {
                    }
                }
            }

            for (K key : removedKeys) {
                try {
                    delete(connection, key);
                } catch (SQLException ignored) {
                }
            }

            dirtyKeys.clear();
            removedKeys.clear();

            connection.commit();
            connection.setAutoCommit(autoCommit);

        } catch (SQLException exception) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
        }
    }

    protected abstract String columns();
    protected abstract Entry<K> read(ResultSet rs) throws SQLException;
    protected abstract void write(Connection connection, Entry<K> value) throws SQLException;
    protected abstract void delete(Connection connection, K key) throws SQLException;
    protected void update() {
    }

    protected void onLoaded(K key, Entry<K> value) {
    }

    protected void onPut(K key, Entry<K> value, Entry<K> previous) {
    }

    protected void onRemoved(K key, Entry<K> removed) {
    }

    protected void onShutdown() {
    }
}
