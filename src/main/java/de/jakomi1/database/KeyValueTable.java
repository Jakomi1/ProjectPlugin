package de.jakomi1.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class KeyValueTable<K, V> extends Table<K, V> {

    private final Class<K> keyType;
    private final Class<V> valueType;

    protected KeyValueTable(String tableName, Class<K> keyType, Class<V> valueType) {
        this(tableName, keyType, valueType, "key", "value");
    }

    protected KeyValueTable(
            String tableName,
            Class<K> keyType,
            Class<V> valueType,
            String keyColumnName,
            String valueColumnName
    ) {
        super(TableSchema.of(
                tableName,
                Column.of(keyColumnName, typeFor(keyType)),
                Column.of(valueColumnName, typeFor(valueType))
        ));
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    protected final K readKey(ResultSet rs) throws SQLException {
        return rs.getObject(schema().keyColumn().name(), keyType);
    }

    @Override
    protected final int bindKey(PreparedStatement ps, int index, K key) throws SQLException {
        ps.setObject(index, key);
        return index + 1;
    }

    @Override
    protected final V readValue(ResultSet rs) throws SQLException {
        return rs.getObject(schema().valueColumns().get(0).name(), valueType);
    }

    @Override
    protected final int bindValue(PreparedStatement ps, int index, V value) throws SQLException {
        ps.setObject(index, value);
        return index + 1;
    }

    private static DataType typeFor(Class<?> type) {
        if (type == String.class) {
            return DataType.TEXT;
        }

        if (type == Integer.class || type == int.class
                || type == Long.class || type == long.class
                || type == Boolean.class || type == boolean.class) {
            return DataType.INTEGER;
        }

        if (type == Double.class || type == double.class) {
            return DataType.REAL;
        }

        return DataType.TEXT;
    }
}
