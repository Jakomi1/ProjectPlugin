package de.jakomi1.database.table;

import de.jakomi1.database.KeyValueTable;

import java.util.UUID;

public final class SkinTable extends KeyValueTable<UUID, String> {

    public SkinTable() {
        super("skin_values", UUID.class, String.class, "player_uuid", "skin_value");
    }
}
