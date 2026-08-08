package de.jakomi1.project.permission;

import de.jakomi1.database.KeyValueTable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Lokale Zusatz-Permissions pro Rolle (Key: Rollenname, Value: kommagetrennte
 * Bukkit-Permissions). Diese sind rein lokal und werden nie nach Supabase gepusht.
 */
public final class RolePermissionTable extends KeyValueTable<String, String> {

    private static final String SEPARATOR = ";";

    public RolePermissionTable() {
        super("role_permissions", String.class, String.class, "role", "permissions");
    }

    /** Alle zusaetzlichen Bukkit-Permissions einer Rolle. */
    public Set<String> getPermissions(Role role) {
        if (role == null) return Set.of();
        return parse(get(role.name()));
    }

    public void addPermission(Role role, String permission) {
        if (role == null || permission == null || permission.isBlank()) return;

        Set<String> permissions = getPermissions(role);
        if (!permissions.add(permission.trim())) return;

        put(role.name(), join(permissions));
        flushNow();
    }

    public void removePermission(Role role, String permission) {
        if (role == null || permission == null) return;

        Set<String> permissions = getPermissions(role);
        if (!permissions.remove(permission)) return;

        if (permissions.isEmpty()) {
            remove(role.name());
        } else {
            put(role.name(), join(permissions));
        }
        flushNow();
    }

    private static Set<String> parse(String raw) {
        if (raw == null || raw.isBlank()) return new LinkedHashSet<>();

        Set<String> result = new LinkedHashSet<>();
        for (String part : raw.split(SEPARATOR)) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static String join(Set<String> permissions) {
        return String.join(SEPARATOR, permissions);
    }
}
