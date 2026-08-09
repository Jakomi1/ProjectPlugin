package de.jakomi1.project.whitelist;

import de.jakomi1.database.Column;
import de.jakomi1.database.DataType;
import de.jakomi1.database.Table;
import de.jakomi1.database.TableSchema;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WhitelistTable extends Table<String, WhitelistTable.WhitelistEntry> {

    private static final String UUID_PREFIX = "uuid:";
    private static final String NAME_PREFIX = "name:";

    private final ConcurrentHashMap<String, WhitelistEntry> nameEntries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, WhitelistEntry> uuidEntries = new ConcurrentHashMap<>();

    public WhitelistTable() {
        super(TableSchema.of(
                "whitelist_table",
                Column.of("id", DataType.TEXT),
                Column.of("name", DataType.TEXT),
                Column.of("uuid", DataType.TEXT)
        ));
    }

    public void add(String name) {
        if (name == null) return;

        String normalized = normalize(name);
        WhitelistEntry byName = nameEntries.get(normalized);

        if (byName != null) {
            if (byName.name() != null && !byName.name().equals(name)) {
                put(byName.uuid() != null ? uuidKey(byName.uuid()) : nameKey(normalized),
                        new WhitelistEntry(name, byName.uuid()));
                flushNow();
            }
            return;
        }

        put(nameKey(normalized), new WhitelistEntry(name, null));
        flushNow();
    }

    public void add(UUID uuid) {
        if (uuid == null) return;

        if (uuidEntries.containsKey(uuid)) return;

        put(uuidKey(uuid), new WhitelistEntry(null, uuid));
        flushNow();
    }

    public void add(UUID uuid, String name) {
        if (uuid == null) return;

        WhitelistEntry existing = uuidEntries.get(uuid);

        if (existing != null) {
            if (name != null && !name.equals(existing.name())) {
                put(uuidKey(uuid), new WhitelistEntry(name, uuid));
                flushNow();
            }
            return;
        }

        put(uuidKey(uuid), new WhitelistEntry(name, uuid));
        flushNow();
    }

    public boolean isWhitelisted(String name) {
        if (name == null) return false;

        return nameEntries.containsKey(normalize(name));
    }

    public boolean isWhitelisted(UUID uuid) {
        return uuid != null && uuidEntries.containsKey(uuid);
    }

    public boolean isWhitelisted(UUID uuid, String name) {
        if (uuid != null && uuidEntries.containsKey(uuid)) return true;

        return name != null && nameEntries.containsKey(normalize(name));
    }

    public void removeEntry(String name) {
        if (name == null) return;

        String normalized = normalize(name);
        WhitelistEntry entry = nameEntries.remove(normalized);
        if (entry == null) return;

        if (entry.uuid() != null) {
            remove(uuidKey(entry.uuid()));
        } else {
            remove(nameKey(normalized));
        }

        flushNow();
    }

    public void removeEntry(UUID uuid) {
        if (uuid == null) return;

        WhitelistEntry entry = uuidEntries.remove(uuid);
        if (entry == null) return;

        if (entry.name() != null) {
            nameEntries.remove(normalize(entry.name()));
        }

        remove(uuidKey(uuid));
        flushNow();
    }

    public void ensure(UUID uuid, String name) {
        if (uuid == null || name == null) return;

        WhitelistEntry byUUID = uuidEntries.get(uuid);

        if (byUUID != null) {
            if (byUUID.name() == null || !byUUID.name().equals(name)) {
                put(uuidKey(uuid), new WhitelistEntry(name, uuid));
                flushNow();
            }
            return;
        }

        WhitelistEntry byName = nameEntries.get(normalize(name));

        if (byName != null) {
            remove(nameKey(normalize(name)));
            put(uuidKey(uuid), new WhitelistEntry(name, uuid));
            flushNow();
            return;
        }

        put(uuidKey(uuid), new WhitelistEntry(name, uuid));
        flushNow();
    }

    public Collection<WhitelistEntry> all() {
        return keys().stream().map(this::get).toList();
    }

    public int count() {
        return size();
    }

    @Override
    protected String readKey(ResultSet rs) throws SQLException {
        return rs.getString("id");
    }

    @Override
    protected int bindKey(PreparedStatement ps, int index, String key) throws SQLException {
        ps.setString(index, key);
        return index + 1;
    }

    @Override
    protected WhitelistEntry readValue(ResultSet rs) throws SQLException {
        String name = rs.getString("name");

        UUID uuid = null;
        String uuidRaw = rs.getString("uuid");
        if (uuidRaw != null) {
            try {
                uuid = UUID.fromString(uuidRaw);
            } catch (IllegalArgumentException ignored) {
            }
        }

        return new WhitelistEntry(name, uuid);
    }

    @Override
    protected int bindValue(PreparedStatement ps, int index, WhitelistEntry value) throws SQLException {
        ps.setString(index, value.name());
        ps.setString(index + 1, value.uuid() == null ? null : value.uuid().toString());
        return index + 2;
    }

    @Override
    protected void onLoaded(String key, WhitelistEntry value) {
        index(value);
    }

    @Override
    protected void onPut(String key, WhitelistEntry value, WhitelistEntry previous) {
        unindex(previous);
        index(value);
    }

    @Override
    protected void onRemoved(String key, WhitelistEntry removed) {
        unindex(removed);
    }

    private void index(WhitelistEntry entry) {
        if (entry == null) return;

        if (entry.name() != null) {
            nameEntries.put(normalize(entry.name()), entry);
        }

        if (entry.uuid() != null) {
            uuidEntries.put(entry.uuid(), entry);
        }
    }

    private void unindex(WhitelistEntry entry) {
        if (entry == null) return;

        if (entry.name() != null) {
            nameEntries.remove(normalize(entry.name()), entry);
        }

        if (entry.uuid() != null) {
            uuidEntries.remove(entry.uuid(), entry);
        }
    }

    private static String uuidKey(UUID uuid) {
        return UUID_PREFIX + uuid;
    }

    private static String nameKey(String name) {
        return NAME_PREFIX + normalize(name);
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public record WhitelistEntry(String name, UUID uuid) {
    }
}
