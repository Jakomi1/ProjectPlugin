package de.jakomi1.database;

public enum DataType {
    TEXT("TEXT"),
    INTEGER("INTEGER"),
    REAL("REAL");

    private final String sql;

    DataType(String sql) {
        this.sql = sql;
    }

    public String sql() {
        return sql;
    }
}
