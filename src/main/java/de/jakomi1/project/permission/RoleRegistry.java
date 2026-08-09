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
        register(Role.MEMBER);
        register(Role.CONTENT_CREATOR);
        register(Role.BOOSTER);
        register(Role.VIP);
        register(Role.DEVELOPER);
        register(Role.BUILDER);
        register(Role.SUPPORTER);
        register(Role.MODERATOR);
        register(Role.ADMIN);
        register(Role.OWNER);
    }

    public synchronized Role register(Role role) {
        if (role == null) return null;
        put(role);
        return role;
    }

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
                    continue; 
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
