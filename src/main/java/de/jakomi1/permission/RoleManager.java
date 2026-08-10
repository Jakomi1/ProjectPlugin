package de.jakomi1.permission;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.supabase.Supabase;
import de.jakomi1.supabase.SupabaseRoleSync;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionDefault;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RoleManager implements Manager {

    private final ProjectServer server;
    private final RoleRegistry registry;
    private final Map<UUID, List<PermissionAttachment>> attachments = new HashMap<>();

    private RoleTable table;
    private RolePermissionTable rolePermissions;

    private SupabaseRoleSync supabaseSync;
    private RolePlayerListener playerListener;

    private String permissionPrefix = "cracked";
    private boolean enabled;
    private boolean tablesRegistered;

    public RoleManager(ProjectServer server) {
        if (server == null) {
            throw new IllegalArgumentException("server cannot be null");
        }

        this.server = server;
        this.registry = RoleRegistry.getDefault();
    }

    public ProjectServer server() {
        return server;
    }

    public RoleRegistry registry() {
        return registry;
    }

    @Override
    public RoleManager enable() {
        if (enabled) {
            return this;
        }

        initializeTables();

        enabled = true;

        registerPermissions();

        playerListener = new RolePlayerListener(this);
        playerListener.register(server.plugin());

        Bukkit.getOnlinePlayers().forEach(this::apply);

        if (supabaseSync != null) {
            supabaseSync.activate();
        }

        return this;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }

        enabled = false;

        if (supabaseSync != null) {
            supabaseSync.disable();
        }

        if (playerListener != null) {
            playerListener.unregister();
            playerListener = null;
        }

        Bukkit.getOnlinePlayers().forEach(this::cleanup);
    }

    public RoleManager syncWith(Supabase supabase) {
        if (supabase == null) {
            throw new IllegalArgumentException("supabase cannot be null");
        }

        this.supabaseSync = new SupabaseRoleSync(supabase, this);

        if (enabled) {
            this.supabaseSync.activate();
        }

        return this;
    }

    public SupabaseRoleSync supabaseSync() {
        return supabaseSync;
    }

    public RoleManager permissionPrefix(String prefix) {
        this.permissionPrefix = prefix == null || prefix.isBlank()
                ? "cracked"
                : prefix;

        if (enabled) {
            registerPermissions();
            applyAll();
        }

        return this;
    }

    public String permissionPrefix() {
        return permissionPrefix;
    }

    public RoleTable table() {
        initializeTables();
        return table;
    }

    public RolePermissionTable rolePermissions() {
        initializeTables();
        return rolePermissions;
    }

    public Role roleOf(UUID uuid) {
        if (uuid == null) {
            return Role.MEMBER;
        }

        initializeTables();

        return table.getRole(uuid);
    }

    public boolean hasRole(Player player, Role role) {
        if (player == null || role == null) {
            return false;
        }

        return roleOf(player.getUniqueId()).inherits(role);
    }

    public RoleManager setRole(UUID uuid, Role role) {
        if (uuid == null || role == null) {
            return this;
        }

        initializeTables();

        Player player = Bukkit.getPlayer(uuid);

        String name = player != null
                ? player.getName()
                : table.getName(uuid);

        table.assign(uuid, role, name);

        if (player != null && enabled) {
            apply(player);
        }

        return this;
    }

    public RoleManager removeRole(UUID uuid) {
        if (uuid == null) {
            return this;
        }

        initializeTables();

        table.block(uuid);

        Player player = Bukkit.getPlayer(uuid);

        if (player != null && enabled) {
            apply(player);
        }

        return this;
    }

    public RoleManager clearOverride(UUID uuid) {
        if (uuid == null) {
            return this;
        }

        initializeTables();

        table.removeEntry(uuid);

        Player player = Bukkit.getPlayer(uuid);

        if (player != null && enabled) {
            apply(player);
        }

        return this;
    }

    public RoleManager recordName(Player player) {
        if (player == null) {
            return this;
        }

        initializeTables();

        table.setName(
                player.getUniqueId(),
                player.getName()
        );

        return this;
    }

    public RoleManager registerRole(
            String name,
            String display,
            String gradient1,
            String gradient2,
            int priority,
            String parent,
            String... permissions
    ) {
        Role role = Role.builder(name)
                .display(display)
                .gradient1(gradient1)
                .gradient2(gradient2)
                .priority(priority)
                .parent(parent)
                .permissions(permissions)
                .build();

        registry.register(role);

        if (enabled) {
            registerPermissions();
            applyAll();
        }

        return this;
    }

    public RoleManager addPermission(Role role, String permission) {
        if (role == null || permission == null || permission.isBlank()) {
            return this;
        }

        initializeTables();

        rolePermissions.addPermission(role, permission);

        if (enabled) {
            applyAll();
        }

        return this;
    }

    public RoleManager removePermission(Role role, String permission) {
        if (role == null || permission == null || permission.isBlank()) {
            return this;
        }

        initializeTables();

        rolePermissions.removePermission(role, permission);

        if (enabled) {
            applyAll();
        }

        return this;
    }

    public RoleManager apply(Player player) {
        if (player == null || !enabled) {
            return this;
        }

        initializeTables();

        removeAttachments(player);

        Role role = roleOf(player.getUniqueId());

        List<PermissionAttachment> added = new ArrayList<>();

        for (Role r : registry.values()) {
            PermissionAttachment attachment =
                    player.addAttachment(server.plugin());

            attachment.setPermission(
                    r.permission(permissionPrefix),
                    false
            );

            added.add(attachment);
        }

        Set<String> granted =
                role.collectPermissions(permissionPrefix);

        granted.addAll(
                rolePermissions.getPermissions(role)
        );

        for (String permission : granted) {
            PermissionAttachment attachment =
                    player.addAttachment(server.plugin());

            attachment.setPermission(
                    permission,
                    true
            );

            added.add(attachment);
        }

        attachments.put(
                player.getUniqueId(),
                added
        );

        player.setOp(role.isOwner());
        player.recalculatePermissions();
        player.updateCommands();

        return this;
    }

    private void removeAttachments(Player player) {
        List<PermissionAttachment> added =
                attachments.remove(player.getUniqueId());

        if (added == null) {
            return;
        }

        for (PermissionAttachment attachment : added) {
            try {
                player.removeAttachment(attachment);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void cleanup(Player player) {
        if (player == null) {
            return;
        }

        removeAttachments(player);
    }

    public void applyAll() {
        if (!enabled) {
            return;
        }

        Bukkit.getOnlinePlayers().forEach(this::apply);
    }

    public void refreshPermissions() {
        if (!enabled) {
            return;
        }

        registerPermissions();
        applyAll();
    }

    private void registerPermissions() {
        for (Role role : registry.values()) {
            String permission = role.permission(permissionPrefix);

            if (Bukkit.getPluginManager().getPermission(permission) != null) {
                continue;
            }

            Map<String, Boolean> children = new HashMap<>();

            Role parent = role.parent();

            if (parent != null) {
                children.put(
                        parent.permission(permissionPrefix),
                        true
                );
            }

            Bukkit.getPluginManager().addPermission(
                    new Permission(
                            permission,
                            PermissionDefault.FALSE,
                            children
                    )
            );
        }
    }

    private void initializeTables() {
        if (tablesRegistered) {
            return;
        }

        table = new RoleTable();
        rolePermissions = new RolePermissionTable();

        table.register(server.plugin());
        rolePermissions.register(server.plugin());

        table = server.plugin()
                .getDatabase()
                .getTable(RoleTable.class);

        rolePermissions = server.plugin()
                .getDatabase()
                .getTable(RolePermissionTable.class);

        if (table == null) {
            throw new IllegalStateException(
                    "RoleTable konnte nicht registriert werden."
            );
        }

        if (rolePermissions == null) {
            throw new IllegalStateException(
                    "RolePermissionTable konnte nicht registriert werden."
            );
        }

        tablesRegistered = true;
    }
}
