package de.jakomi1.database;

import de.jakomi1.project.ProjectPlugin;
import de.jakomi1.project.Registerable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class Table<K, V> implements Registerable {

    private final TableSchema schema;
    private Database database;

    private final Map<K, V> cache = new ConcurrentHashMap<>();
    private final Set<K> dirtyKeys = ConcurrentHashMap.newKeySet();
    private final Set<K> removedKeys = ConcurrentHashMap.newKeySet();

    protected Table(TableSchema schema) {
        this.schema = schema;
    }

    final void initialize(Database database) {
        this.database = database;
        onInitialize();
        createTable();
        loadAll();
    }

    @Override
    public void register(ProjectPlugin plugin) {
        plugin.getDatabase().register(this);
    }

    protected final Connection connection() {
        return database.connection();
    }

    protected final TableSchema schema() {
        return schema;
    }

    private void createTable() {
        Connection connection = connection();
        if (connection == null) return;

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(schema.createTableSql());
        } catch (SQLException ignored) {
        }
    }

    private void loadAll() {
        Connection connection = connection();
        if (connection == null) return;

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(schema.selectAllSql())) {

            while (rs.next()) {
                try {
                    K key = readKey(rs);
                    V value = readValue(rs);
                    cache.put(key, value);
                    onLoaded(key, value);
                } catch (SQLException ignored) {
                }
            }

        } catch (SQLException ignored) {
        }
    }

    public final V get(K key) {
        return cache.get(key);
    }

    public final void put(K key, V value) {
        V previous = cache.put(key, value);
        dirtyKeys.add(key);
        removedKeys.remove(key);
        onPut(key, value, previous);
    }

    public final void remove(K key) {
        V removed = cache.remove(key);
        if (removed != null) {
            dirtyKeys.remove(key);
            removedKeys.add(key);
            onRemoved(key, removed);
        }
    }

    public final boolean containsKey(K key) {
        return cache.containsKey(key);
    }

    public final int size() {
        return cache.size();
    }

    public final Set<K> keys() {
        return Set.copyOf(cache.keySet());
    }

    public final List<V> values() {
        return List.copyOf(cache.values());
    }

    public final Set<Map.Entry<K, V>> entrySet() {
        return Set.copyOf(cache.entrySet());
    }

    public final V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (key == null || mappingFunction == null) return null;

        V value = cache.get(key);
        if (value != null) return value;

        value = mappingFunction.apply(key);
        if (value == null) return null;

        put(key, value);
        return value;
    }

    public final Optional<V> findBy(Predicate<? super V> predicate) {
        if (predicate == null) return Optional.empty();
        return cache.values().stream().filter(predicate).findFirst();
    }

    /**
     * Speichert alle veränderten Werte dieser Tabelle asynchron im
     * globalen Scheduler (funktioniert auf Folia und Paper).
     */
    public final void flushAsync() {
        if (database == null) return;
        database.scheduler().runGlobal(this::flushNow);
    }

    public final void flushNow() {
        Connection connection = connection();
        if (connection == null) return;

        boolean autoCommit;
        try {
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
        } catch (SQLException ignored) {
            return;
        }

        try {
            try (PreparedStatement upsert = connection.prepareStatement(schema.upsertSql())) {
                for (K key : dirtyKeys) {
                    V value = cache.get(key);
                    if (value == null) continue;

                    try {
                        int index = bindKey(upsert, 1, key);
                        bindValue(upsert, index, value);
                        upsert.addBatch();
                    } catch (SQLException ignored) {
                    }
                }
                upsert.executeBatch();
            }

            try (PreparedStatement delete = connection.prepareStatement(schema.deleteByKeySql())) {
                for (K key : removedKeys) {
                    try {
                        bindKey(delete, 1, key);
                        delete.addBatch();
                    } catch (SQLException ignored) {
                    }
                }
                delete.executeBatch();
            }

            dirtyKeys.clear();
            removedKeys.clear();

            connection.commit();
        } catch (SQLException exception) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
        } finally {
            try {
                connection.setAutoCommit(autoCommit);
            } catch (SQLException ignored) {
            }
        }
    }

    protected abstract K readKey(ResultSet rs) throws SQLException;

    protected abstract int bindKey(PreparedStatement ps, int index, K key) throws SQLException;

    protected abstract V readValue(ResultSet rs) throws SQLException;

    protected abstract int bindValue(PreparedStatement ps, int index, V value) throws SQLException;

    protected void onInitialize() {
    }

    protected void onLoaded(K key, V value) {
    }

    protected void onPut(K key, V value, V previous) {
    }

    protected void onRemoved(K key, V removed) {
    }

    protected void onShutdown() {
    }
}
