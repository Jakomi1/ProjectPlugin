package de.jakomi1.database;

public final class Column {

    private final String name;
    private final DataType type;

    private Column(String name, DataType type) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Spaltenname darf nicht leer sein.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Spaltentyp darf nicht null sein.");
        }
        this.name = name;
        this.type = type;
    }

    public static Column of(String name, DataType type) {
        return new Column(name, type);
    }

    public String name() {
        return name;
    }

    public DataType type() {
        return type;
    }
}
