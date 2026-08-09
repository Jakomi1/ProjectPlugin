package de.jakomi1.project.skin;

import de.jakomi1.database.table.SkinTable;
import de.jakomi1.project.ProjectServer;

import java.util.UUID;

public final class SkinManager {

    private final ProjectServer server;
    private final SkinTable table;

    public SkinManager(ProjectServer server, SkinTable table) {
        this.server = server;
        this.table = table;
    }

    public ProjectServer server() {
        return server;
    }

    public SkinTable table() {
        return table;
    }

    public SkinManager set(UUID uuid, String value) {
        if (uuid == null || value == null || value.isBlank()) return this;
        table.put(uuid, value);
        return this;
    }

    public String get(UUID uuid) {
        return uuid == null ? null : table.get(uuid);
    }

    public boolean has(UUID uuid) {
        return uuid != null && table.containsKey(uuid);
    }

    public SkinManager remove(UUID uuid) {
        if (uuid != null) table.remove(uuid);
        return this;
    }
}
