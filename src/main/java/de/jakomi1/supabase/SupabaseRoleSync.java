package de.jakomi1.supabase;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.jakomi1.permission.Role;
import de.jakomi1.permission.RoleManager;
import de.jakomi1.permission.RoleTable;
import de.jakomi1.scheduler.Scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SupabaseRoleSync {

    public static final String ROLES_TABLE = "roles";
    public static final String PLAYER_ROLES_TABLE = "player_roles";

    private static final String SELECT_ROLES = "name,display,priority,color1,color2";
    private static final String SELECT_PLAYER_ROLES = "uuid,role,name";
    private static final long DEFAULT_PULL_PERIOD = 20L * 60 * 10;

    private final Supabase supabase;
    private final RoleManager roles;

    private Scheduler.Task syncTask;
    private long pullPeriod = DEFAULT_PULL_PERIOD;

    public SupabaseRoleSync(Supabase supabase, RoleManager roles) {
        this.supabase = supabase;
        this.roles = roles;
    }

    public SupabaseRoleSync pullPeriod(long ticks) {
        this.pullPeriod = Math.max(ticks, 1L);
        return this;
    }

    public void activate() {
        if (syncTask != null && !syncTask.isCancelled()) return;

        pullDefinitions();
        pull();

        syncTask = roles.server().scheduler().runTimer(this::pull, pullPeriod, pullPeriod);
    }

    public void disable() {
        if (syncTask == null) return;
        syncTask.cancel();
        syncTask = null;
    }

    public void pullDefinitions() {
        supabase.selectAsync(ROLES_TABLE, SELECT_ROLES, result -> {
            if (result == null || result.size() == 0) return;

            int applied = roles.registry().apply(result);
            if (applied > 0) {
                roles.refreshPermissions();
            }
        });
    }

    public void pull() {
        supabase.selectAsync(PLAYER_ROLES_TABLE, SELECT_PLAYER_ROLES, result -> {
            if (result == null || result.size() == 0) return;

            Map<UUID, RoleTable.PlayerRole> rows = new HashMap<>();
            for (JsonElement element : result) {
                try {
                    JsonObject row = element.getAsJsonObject();

                    UUID uuid = UUID.fromString(row.get("uuid").getAsString());
                    Role role = Role.role(row.get("role").getAsString());
                    if (role == null) continue;

                    String name = row.has("name") && !row.get("name").isJsonNull()
                            ? row.get("name").getAsString()
                            : null;

                    rows.put(uuid, new RoleTable.PlayerRole(role, name));
                } catch (IllegalArgumentException ignored) {
                }
            }

            roles.table().sync(rows);
            roles.applyAll();
        });
    }
}
