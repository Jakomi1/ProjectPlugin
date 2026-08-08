package de.jakomi1.project.permission;

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
    private final RoleTable table;
    private final RolePermissionTable rolePermissions;
    private final Map<UUID, List<PermissionAttachment>> attachments = new HashMap<>();

    private SupabaseRoleSync supabaseSync;
    private RolePlayerListener playerListener;

    private String permissionPrefix = "cracked";
    private boolean enabled;
    private boolean tablesRegistered;

    public RoleManager(ProjectServer server) {
        this.server = server;
        this.registry = RoleRegistry.getDefault();
        this.table = new RoleTable();
        this.rolePermissions = new RolePermissionTable();
    }

    public ProjectServer server() {
        return server;
    }

    public RoleRegistry registry() {
        return registry;
    }

    /**
     * Aktiviert das Rollen-Permissions-System fuer diesen Server.
     *
     * <p>Ist eine Supabase-Synchronisierung über {@link #syncWith(Supabase)}
     * konfiguriert, werden die Rollen-Definitionen gezogen, die Rollen aus Supabase
     * uebernommen (lokale Overrides bleiben) und periodisch abgeglichen.</p>
     */
    @Override
    public RoleManager enable() {
        if (enabled) return this;
        enabled = true;

        if (!tablesRegistered) {
            table.register(server.plugin());
            rolePermissions.register(server.plugin());
            registerPermissions();
            seedDefaults();
            tablesRegistered = true;
        }

        this.playerListener = new RolePlayerListener(this);
        playerListener.register(server.plugin());

        Bukkit.getOnlinePlayers().forEach(this::apply);

        if (supabaseSync != null) {
            supabaseSync.enable();
        }
        return this;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Deaktiviert das Rollen-System: Sync stoppen, Listener abmelden und
     * alle getrackten Permission-Attachments entfernen.
     */
    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;

        if (supabaseSync != null) {
            supabaseSync.disable();
        }
        if (playerListener != null) {
            playerListener.unregister();
        }
        Bukkit.getOnlinePlayers().forEach(this::cleanup);
    }

    /**
     * Verbindet das Rollen-System mit Supabase (read-only, nur GET).
     * Rollen-Definitionen ({@code roles}) und Zuordnungen ({@code player_roles})
     * werden gezogen; lokale Overrides bleiben erhalten.
     */
    public RoleManager syncWith(Supabase supabase) {
        this.supabaseSync = new SupabaseRoleSync(supabase, this);
        return this;
    }

    public SupabaseRoleSync supabaseSync() {
        return supabaseSync;
    }

    public RoleManager permissionPrefix(String prefix) {
        this.permissionPrefix = prefix == null || prefix.isBlank() ? "cracked" : prefix;
        return this;
    }

    public String permissionPrefix() {
        return permissionPrefix;
    }

    public RoleTable table() {
        return table;
    }

    public RolePermissionTable rolePermissions() {
        return rolePermissions;
    }

    public Role roleOf(UUID uuid) {
        return uuid == null ? Role.MEMBER : table.getRole(uuid);
    }

    public boolean hasRole(Player player, Role role) {
        return roleOf(player.getUniqueId()).inherits(role);
    }

    /**
     * Weist einem Spieler lokal eine Rolle zu (manueller Override).
     * Wird nicht nach Supabase gepusht und nicht durch Supabase-Updates ueberschrieben.
     */
    public RoleManager setRole(UUID uuid, Role role) {
        if (uuid == null || role == null) return this;

        Player player = Bukkit.getPlayer(uuid);
        String name = player != null ? player.getName() : table.getName(uuid);

        table.assign(uuid, role, name);
        if (player != null) apply(player);
        return this;
    }

    /**
     * Nimmt einem Spieler lokal die Rolle (manueller Override).
     * Die Entfernung bleibt auch bei Supabase-Updates erhalten.
     */
    public RoleManager removeRole(UUID uuid) {
        if (uuid == null) return this;

        table.block(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) apply(player);
        return this;
    }

    /**
     * Entfernt einen lokalen Override: Beim naechsten Sync gilt wieder der Supabase-Stand.
     */
    public RoleManager clearOverride(UUID uuid) {
        if (uuid == null) return this;

        table.removeEntry(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) apply(player);
        return this;
    }

    /**
     * Speichert den Spielernamen eines online Spielers lokal.
     */
    public RoleManager recordName(Player player) {
        if (player == null || !enabled) return this;
        table.setName(player.getUniqueId(), player.getName());
        return this;
    }

    /**
     * Registriert eine zusaetzliche, projekt-spezifische Rolle (lokal).
     * Beispiel:
     * <pre>
     *   server.permissions().registerRole("BURGER", "Burger-Flipper",
     *           "#ff9900", "#ffcc66", 6, "MEMBER", "project.command.burger");
     * </pre>
     */
    public RoleManager registerRole(String name, String display, String gradient1, String gradient2,
                                    int priority, String parent, String... permissions) {
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

    /**
     * Fuegt einer Rolle eine zusaetzliche Bukkit-Permission hinzu (lokal gespeichert).
     */
    public RoleManager addPermission(Role role, String permission) {
        rolePermissions.addPermission(role, permission);
        applyAll();
        return this;
    }

    /**
     * Entfernt eine zusaetzliche Bukkit-Permission einer Rolle (lokal gespeichert).
     */
    public RoleManager removePermission(Role role, String permission) {
        rolePermissions.removePermission(role, permission);
        applyAll();
        return this;
    }

    public RoleManager apply(Player player) {
        if (player == null || !enabled) return this;

        removeAttachments(player);

        Role role = roleOf(player.getUniqueId());
        List<PermissionAttachment> added = new ArrayList<>();

        for (Role r : registry.values()) {
            PermissionAttachment attachment = player.addAttachment(server.plugin());
            attachment.setPermission(r.permission(permissionPrefix), false);
            added.add(attachment);
        }

        Set<String> granted = role.collectPermissions(permissionPrefix);
        granted.addAll(rolePermissions.getPermissions(role));

        for (String permission : granted) {
            PermissionAttachment attachment = player.addAttachment(server.plugin());
            attachment.setPermission(permission, true);
            added.add(attachment);
        }

        attachments.put(player.getUniqueId(), added);

        player.setOp(role.isOwner());
        player.recalculatePermissions();
        player.updateCommands();
        return this;
    }

    private void removeAttachments(Player player) {
        List<PermissionAttachment> added = attachments.remove(player.getUniqueId());
        if (added == null) return;

        for (PermissionAttachment attachment : added) {
            try {
                player.removeAttachment(attachment);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    /**
     * Entfernt die getrackten Attachments, wenn ein Spieler offline geht.
     */
    public void cleanup(Player player) {
        if (player == null) return;
        removeAttachments(player);
    }

    public void applyAll() {
        Bukkit.getOnlinePlayers().forEach(this::apply);
    }

    /**
     * Registriert (neu) die Bukkit-Permissions aller bekannten Rollen.
     * Idempotent – bereits registrierte Permissions werden uebersprungen.
     */
    public void refreshPermissions() {
        registerPermissions();
        applyAll();
    }

    private void registerPermissions() {
        for (Role role : registry.values()) {
            String permission = role.permission(permissionPrefix);
            if (Bukkit.getPluginManager().getPermission(permission) != null) continue;

            Map<String, Boolean> children = new HashMap<>();
            Role parent = role.parent();
            if (parent != null) {
                children.put(parent.permission(permissionPrefix), true);
            }

            Bukkit.getPluginManager().addPermission(
                    new Permission(permission, PermissionDefault.FALSE, children)
            );
        }
    }

    private void seedDefaults() {
        if (table.size() > 0) return;

        for (Map.Entry<UUID, String> entry : DEFAULTS.entrySet()) {
            Role role = registry.role(entry.getValue());
            if (role != null) {
                table.setRole(entry.getKey(), role, null);
            }
        }
    }

    /**
     * Standard-Rollen aus CrackedCommunity (RolePlayer), Rollenname als Wert.
     */
    public static final Map<UUID, String> DEFAULTS = Map.ofEntries(
            Map.entry(UUID.fromString("d6fa4a1b-6e3c-4e3e-a8f8-f7dd4c9ba290"), "OWNER"),
            Map.entry(UUID.fromString("f4af1633-7110-48af-97ec-f5d84e3cd142"), "ADMIN"),
            Map.entry(UUID.fromString("64cb482a-36c1-41f6-af59-5b304c576c32"), "MODERATOR"),
            Map.entry(UUID.fromString("c4c849dc-d3db-4038-9830-7bd6456c15e0"), "SUPPORTER"),
            Map.entry(UUID.fromString("d7195fa6-41cd-4f57-a7e0-11354129aef2"), "SUPPORTER"),
            Map.entry(UUID.fromString("42d4c62e-28de-4cbf-8231-726ac6475750"), "SUPPORTER"),
            Map.entry(UUID.fromString("510f65d0-dea7-4f96-8c34-9f86d0d4a852"), "SUPPORTER"),
            Map.entry(UUID.fromString("65f290b5-846c-49b7-b9a7-bddd42808ef4"), "SUPPORTER"),
            Map.entry(UUID.fromString("ff290473-aaf5-486c-b3fb-891de42788f3"), "SUPPORTER"),
            Map.entry(UUID.fromString("4c491bd1-b522-4ba1-8878-bf0365082521"), "SUPPORTER"),
            Map.entry(UUID.fromString("e9cd9857-22d7-4d06-8d0c-54513cc12fa1"), "DEVELOPER"),
            Map.entry(UUID.fromString("6ba782be-0a31-49c6-b1f4-f6fafc30e2ea"), "BUILDER"),
            Map.entry(UUID.fromString("324d452d-24fb-4e50-9cdd-874c2247a61c"), "BUILDER")
    );
}
