package de.jakomi1.database;

import java.util.ArrayList;
import java.util.List;

public final class TableSchema {

    private final String tableName;
    private final Column keyColumn;
    private final List<Column> valueColumns;

    private final String createTableSql;
    private final String selectAllSql;
    private final String upsertSql;
    private final String deleteByKeySql;

    private TableSchema(String tableName, Column keyColumn, List<Column> valueColumns) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("Tabellenname darf nicht leer sein.");
        }
        if (keyColumn == null) {
            throw new IllegalArgumentException("Schlüsselspalte darf nicht null sein.");
        }

        this.tableName = tableName;
        this.keyColumn = keyColumn;
        this.valueColumns = List.copyOf(valueColumns);

        this.createTableSql = buildCreateTable();
        this.selectAllSql = "SELECT * FROM " + tableName;
        this.upsertSql = buildUpsert();
        this.deleteByKeySql = "DELETE FROM " + tableName + " WHERE " + keyColumn.name() + " = ?";
    }

    public static TableSchema of(String tableName, Column keyColumn, Column... valueColumns) {
        return new TableSchema(
                tableName,
                keyColumn,
                valueColumns == null ? List.of() : List.of(valueColumns)
        );
    }

    public String tableName() {
        return tableName;
    }

    public Column keyColumn() {
        return keyColumn;
    }

    public List<Column> valueColumns() {
        return valueColumns;
    }

    public String createTableSql() {
        return createTableSql;
    }

    public String selectAllSql() {
        return selectAllSql;
    }

    public String upsertSql() {
        return upsertSql;
    }

    public String deleteByKeySql() {
        return deleteByKeySql;
    }

    private String buildCreateTable() {
        List<String> columns = new ArrayList<>();
        columns.add(keyColumn.name() + " " + keyColumn.type().sql() + " PRIMARY KEY");

        for (Column column : valueColumns) {
            columns.add(column.name() + " " + column.type().sql());
        }

        return "CREATE TABLE IF NOT EXISTS " + tableName
                + " (" + String.join(", ", columns) + ")";
    }

    private String buildUpsert() {
        if (valueColumns.isEmpty()) {
            return "INSERT OR IGNORE INTO " + tableName
                    + " (" + keyColumn.name() + ") VALUES (?)";
        }

        List<String> allColumns = new ArrayList<>();
        allColumns.add(keyColumn.name());
        for (Column column : valueColumns) {
            allColumns.add(column.name());
        }

        List<String> assignments = new ArrayList<>();
        for (Column column : valueColumns) {
            assignments.add(column.name() + " = excluded." + column.name());
        }

        String placeholders = "?, ".repeat(allColumns.size());
        placeholders = placeholders.substring(0, placeholders.length() - 2);

        return "INSERT INTO " + tableName
                + " (" + String.join(", ", allColumns) + ")"
                + " VALUES (" + placeholders + ")"
                + " ON CONFLICT(" + keyColumn.name() + ") DO UPDATE SET "
                + String.join(", ", assignments);
    }
}
