package de.jakomi1.project.nickname;

import de.jakomi1.database.Column;
import de.jakomi1.database.DataType;
import de.jakomi1.database.Table;
import de.jakomi1.database.TableSchema;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistierte Anzeigenamen (Nicknames).
 *
 * <p>Schluessel ist die UUID, damit ein Namenswechsel des echten Kontos den
 * Nickname nicht verliert. Der echte Name liegt nur als Spalte daneben und
 * dient der Suche/Anzeige. Der Nickname selbst wird als Text gespeichert und
 * erst bei der Anzeige in eine {@code Component} uebersetzt.
 */
public final class NicknameTable extends Table<UUID, NicknameTable.NicknameEntry> {

    private static final Map<String, UUID> idsByName = new ConcurrentHashMap<>();

    public NicknameTable() {
        super(TableSchema.of("nicknames",
                Column.of("player_uuid", DataType.TEXT),
                Column.of("player_name", DataType.TEXT),
                Column.of("nickname", DataType.TEXT)));
    }

    public NicknameEntry entry(UUID uuid) {
        return uuid == null ? null : get(uuid);
    }

    public NicknameEntry entry(String name) {
        if (name == null || name.isBlank()) return null;

        UUID uuid = idsByName.get(name.toLowerCase(Locale.ROOT));
        return uuid == null ? null : get(uuid);
    }

    public boolean has(UUID uuid) {
        NicknameEntry entry = entry(uuid);
        return entry != null && entry.nickname() != null && !entry.nickname().isBlank();
    }

    public boolean has(String name) {
        NicknameEntry entry = entry(name);
        return entry != null && entry.nickname() != null && !entry.nickname().isBlank();
    }

    /** Roher Nickname (Markup-Text) oder null. */
    public String nickname(UUID uuid) {
        NicknameEntry entry = entry(uuid);
        return entry == null ? null : entry.nickname();
    }

    public String nickname(String name) {
        NicknameEntry entry = entry(name);
        return entry == null ? null : entry.nickname();
    }

    public void set(UUID uuid, String playerName, String nickname) {
        if (uuid == null) return;

        put(uuid, new NicknameEntry(playerName, nickname));
        flushAsync();
    }

    public void clear(UUID uuid) {
        if (uuid == null) return;

        remove(uuid);
        flushAsync();
    }

    @Override
    public UUID readKey(ResultSet rs) throws SQLException {
        return UUID.fromString(rs.getString("player_uuid"));
    }

    @Override
    public int bindKey(PreparedStatement ps, int index, UUID key) throws SQLException {
        ps.setString(index, key.toString());
        return index + 1;
    }

    @Override
    public NicknameEntry readValue(ResultSet rs) throws SQLException {
        return new NicknameEntry(
                rs.getString("player_name"),
                rs.getString("nickname")
        );
    }

    @Override
    public int bindValue(PreparedStatement ps, int index, NicknameEntry value) throws SQLException {
        ps.setString(index, value.playerName());
        ps.setString(index + 1, value.nickname());
        return index + 2;
    }

    @Override
    protected void onLoaded(UUID key, NicknameEntry value) {
        indexName(key, value.playerName());
    }

    @Override
    protected void onPut(UUID key, NicknameEntry value, NicknameEntry previous) {
        if (previous != null && previous.playerName() != null) {
            idsByName.remove(previous.playerName().toLowerCase(Locale.ROOT));
        }

        indexName(key, value.playerName());
    }

    @Override
    protected void onRemoved(UUID key, NicknameEntry removed) {
        if (removed.playerName() == null) return;

        idsByName.remove(removed.playerName().toLowerCase(Locale.ROOT));
    }

    private static void indexName(UUID uuid, String name) {
        if (uuid == null || name == null || name.isBlank()) return;

        idsByName.put(name.toLowerCase(Locale.ROOT), uuid);
    }

    /**
     * @param playerName echter Name des Kontos
     * @param nickname   Anzeige-Text (Markup), oder null
     */
    public record NicknameEntry(String playerName, String nickname) {
    }
}
