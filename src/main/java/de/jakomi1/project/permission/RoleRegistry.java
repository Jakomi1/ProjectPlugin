package de.jakomi1.project.permission;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Registry aller bekannten Rollen (BUILTIN + projekt-spezifisch LOCAL + aus Supabase).
 *
 * <p>Die Standard-Rollen sind fest vordefiniert. Projekt-spezifische Rollen werden
 * über {@link #register(Role)} ergänzt. Rollen-Definitionen aus Supabase werden über
 * {@link #apply(JsonArray)} übernommen – vorhandene BUILTIN-/LOCAL-Rollen bleiben dabei
 * unangetastet (manuelle Definitionen gewinnen), nur SUPABASE-Rollen und neue Rollen
 * werden aktualisiert.</p>
 */
public final class RoleRegistry {

    private static final RoleRegistry DEFAULT = new RoleRegistry();

    private final Map<String, Role> roles = new LinkedHashMap<>();
    private final Map<String, Role> byName = new LinkedHashMap<>();

    private RoleRegistry() {
        seed();
    }

    public static RoleRegistry getDefault() {
        return DEFAULT;
    }

    private void seed() {
        registerBuiltin("MEMBER", "Mitglied", "#219752", "#2ecc71", 0, null);
        registerBuiltin("CONTENT_CREATOR", "Creator", "#aa2a86", "#f27ba4", 1, "MEMBER");
        registerBuiltin("BOOSTER", "Booster", "#965f7f", "#ffaadc", 2, "MEMBER");
        registerBuiltin("VIP", "VIP", "#beab70", "#fbe7ab", 3, "MEMBER");
        registerBuiltin("DEVELOPER", "Developer", "#4cadd0", "#b2f9ff", 4, "MEMBER");
        registerBuiltin("BUILDER", "Builder", "#6c45b4", "#5d96ff", 5, "MEMBER");
        registerBuiltin("SUPPORTER", "Supporter", "#2a57e9", "#5B7FEB", 6, "MEMBER");
        registerBuiltin("MODERATOR", "Moderator", "#c25a00", "#ecb83e", 7, "SUPPORTER");
        registerBuiltin("ADMIN", "Admin", "#700707", "#ff0000", 8, "MODERATOR");
        registerBuiltin("OWNER", "Owner", "#c305ff", "#2bd9fd", 9, "ADMIN");
    }

    private void registerBuiltin(String name, String display, String gradient1, String gradient2,
                                 int priority, String parent) {
        Role role = new Role(name, display, gradient1, gradient2, priority, parent,
                Set.of(), Role.Source.BUILTIN);
        put(role);
    }

    /**
     * Registriert eine projekt-spezifische Rolle (lokal). Eine bereits existierende
     * Rolle mit demselben Namen wird ersetzt.
     */
    public synchronized Role register(Role role) {
        if (role == null) return null;
        put(role);
        return role;
    }

    /**
     * Uebernimmt Rollen-Definitionen aus Supabase ({@code roles}).
     * BUILTIN- und LOCAL-Rollen werden nicht ueberschrieben.
     *
     * @return Anzahl der uebernommenen/aktualisierten Rollen
     */
    public synchronized int apply(JsonArray supabaseRoles) {
        if (supabaseRoles == null) return 0;

        int applied = 0;
        for (JsonElement element : supabaseRoles) {
            try {
                JsonObject row = element.getAsJsonObject();
                if (!row.has("name") || row.get("name").isJsonNull()) continue;

                String name = row.get("name").getAsString();
                if (name == null || name.isBlank()) continue;

                Role existing = role(name);
                if (existing != null && existing.source() != Role.Source.SUPABASE) {
                    continue; // BUILTIN/LOCAL bleibt unangetastet
                }

                String display = optString(row, "display", name);
                String color1 = optString(row, "color1", "#ffffff");
                String color2 = optString(row, "color2", color1);
                int priority = row.has("priority") && row.get("priority").isJsonPrimitive()
                        ? row.get("priority").getAsInt()
                        : 0;

                Role role = new Role(name, display, color1, color2, priority,
                        "MEMBER", Set.of(), Role.Source.SUPABASE);
                put(role);
                applied++;
            } catch (IllegalArgumentException ignored) {
            }
        }
        return applied;
    }

    private void put(Role role) {
        roles.put(role.name(), role);
        byName.put(role.name().toLowerCase(Locale.ROOT), role);
    }

    public Role role(String name) {
        if (name == null) return null;
        return byName.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean isRegistered(String name) {
        return role(name) != null;
    }

    public int size() {
        return roles.size();
    }

    public Set<String> names() {
        return Set.copyOf(roles.keySet());
    }

    /** Alle Rollen, sortiert nach Prioritaet (aufsteigend). */
    public List<Role> values() {
        List<Role> sorted = new ArrayList<>(roles.values());
        sorted.sort(Comparator.comparingInt(Role::priority).thenComparing(Role::name));
        return List.copyOf(sorted);
    }

    private static String optString(JsonObject row, String key, String fallback) {
        if (!row.has(key) || row.get(key).isJsonNull()) return fallback;
        String value = row.get(key).getAsString();
        return value == null || value.isBlank() ? fallback : value;
    }
}
