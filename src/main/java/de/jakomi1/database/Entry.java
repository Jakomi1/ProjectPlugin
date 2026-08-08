package de.jakomi1.database;

public final class Entry<K> {

    private final K key;
    private final Object value;

    public Entry(K key, Object value) {
        this.key = key;
        this.value = value;
    }

    public K key() {
        return key;
    }

    @SuppressWarnings("unchecked")
    public <T> T value() {
        return (T) value;
    }
}
