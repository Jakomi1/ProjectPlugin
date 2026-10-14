package de.jakomi1.permission;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.supabase.Supabase;
import de.jakomi1.supabase.SupabaseRoleSync;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionDefault;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class RoleManager implements Manager {

    private final ProjectServer server;
    private final RoleRegistry registry;
    private final Map<UUID, List<PermissionAttachment>> attachments = new ConcurrentHashMap<>();
    private final Map<Role, List<String>> priorityPermissions = new LinkedHashMap<>();

    private RoleTable table;
    private RolePermissionTable rolePermissions;
    private PendingRoleTable pendingRoles;

    private SupabaseRoleSync supabaseSync;
    private RolePlayerListener playerListener;
    private RoleCommand roleCommand;

    private Consumer<Player> changeListener;
    private String permissionPrefix = "cracked";
    private boolean registerCommand = true;
    private String commandPermission;
    private Role opRole = Role.OWNER;
    private volatile boolean enabled;
    private volatile boolean tablesRegistered;

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

        if (registerCommand) {
            roleCommand = new RoleCommand(this);
            roleCommand.register(server.plugin());
        }

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

        roleCommand = null;

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

    /**
     * Registriert einen Callback, der nach jeder Rollenaenderung eines
     * online Spielers auf dem Haupt-Thread laeuft. Darueber zieht ein Projekt
     * beispielsweise die Tab-Sortierung oder den Op-Status nach.
     */
    public RoleManager onChange(Consumer<Player> listener) {
        this.changeListener = listener;
        return this;
    }

    /** Bestimmt, ob {@code /role} beim Aktivieren registriert wird. */
    public RoleManager command(boolean register) {
        this.registerCommand = register;
        return this;
    }

    public boolean command() {
        return registerCommand;
    }

    public RoleManager commandPermission(String permission) {
        this.commandPermission = permission == null || permission.isBlank()
                ? null
                : permission;

        return this;
    }

    /**
     * Permission, die {@code /role} verlangt. Ohne eigene Angabe die
     * OWNER-Permission aus dem gesetzten {@link #permissionPrefix()}.
     */
    public String commandPermission() {
        if (commandPermission != null) {
            return commandPermission;
        }

        return Role.OWNER.permission(permissionPrefix);
    }

    /**
     * Ab dieser Rolle werden Spieler beim Anwenden der Rollen als Operator
     * gesetzt. Voreinstellung ist {@link Role#OWNER}; mit {@link Role#ADMIN}
     * sind etwa Admins und Owner immer Op.
     */
    public RoleManager opRole(Role role) {
        this.opRole = role != null ? role : Role.OWNER;

        if (enabled) {
            applyAll();
        }

        return this;
    }

    public Role opRole() {
        return opRole;
    }

    public RoleTable table() {
        initializeTables();
        return table;
    }

    public RolePermissionTable rolePermissions() {
        initializeTables();
        return rolePermissions;
    }

    /** Offene Rollen-Vergaben fuer noch nie verbundene Spieler. */
    public PendingRoleTable pendingRoles() {
        initializeTables();
        return pendingRoles;
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
            notifyChange(player);
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
            notifyChange(player);
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
            notifyChange(player);
        }

        return this;
    }

    /**
     * Wendet eine beim ersten Beitritt geparkte Rollen-Vergabe an.
     *
     * <p>Wird von {@link RolePlayerListener} aufgerufen, bevor die normale
     * Rolle geladen wird. Eine geparkte Rolle ist eine echte Override-Rolle
     * und ueberlebt das Standard-Rollen-System, bis sie ueber
     * {@code /role remove} wieder entfernt wird.
     *
     * @return die vergebene Rolle, oder null, wenn nichts geparkt war
     */
    public Role applyPendingRole(Player player) {
        if (player == null) {
            return null;
        }

        PendingRoleTable pending = pendingRoles();
        Role role = pending.pendingRole(player.getName());

        if (role == null) {
            return null;
        }

        pending.clear(player.getName());

        table().assign(player.getUniqueId(), role, player.getName());

        Component prefix = server.plugin().getPrefix();
        player.sendMessage((prefix != null ? prefix : Component.empty()).append(
                Component.text("Deine Rolle wurde zu ", NamedTextColor.GRAY)
                        .append(role.coloredDisplay())
                        .append(Component.text(" geändert.", NamedTextColor.GRAY))));

        return role;
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

    /**
     * Vergibt zusaetzliche Fremd-Plugin-Permissions an alle Rollen, deren
     * {@link Role#priority() Prioritaet} mindestens der uebergebenen
     * entspricht. So gilt "ab Stufe X" unabhaengig von der Eltern-Kette der
     * Rollen. Die Permissions werden bei jedem {@link #apply(Player)} neu
     * gesetzt und beim Quit ueber {@link #cleanup(Player)} entfernt.
     */
    public RoleManager grant(Role minimumRole, List<String> permissions) {
        if (minimumRole == null || permissions == null || permissions.isEmpty()) {
            return this;
        }

        priorityPermissions.merge(
                minimumRole,
                List.copyOf(permissions),
                (existing, added) -> {
                    List<String> merged = new ArrayList<>(existing);

                    for (String permission : added) {
                        if (!merged.contains(permission)) {
                            merged.add(permission);
                        }
                    }

                    return List.copyOf(merged);
                }
        );

        if (enabled) {
            applyAll();
        }

        return this;
    }

    public RoleManager grant(Role minimumRole, String... permissions) {
        return grant(
                minimumRole,
                permissions == null ? List.of() : Arrays.asList(permissions)
        );
    }

    public RoleManager apply(Player player) {
        if (player == null || !enabled) {
            return this;
        }

        server.scheduler().runEntity(player, () -> applyInternal(player));
        return this;
    }

    private void applyInternal(Player player) {
        if (player == null || !enabled || !player.isOnline()) {
            return;
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

        for (Map.Entry<Role, List<String>> entry : priorityPermissions.entrySet()) {
            if (role.priority() < entry.getKey().priority()) {
                continue;
            }

            for (String permission : entry.getValue()) {
                PermissionAttachment attachment =
                        player.addAttachment(server.plugin());

                attachment.setPermission(
                        permission,
                        true
                );

                added.add(attachment);
            }
        }

        attachments.put(
                player.getUniqueId(),
                added
        );

        player.setOp(role.inherits(opRole));
        player.recalculatePermissions();
        player.updateCommands();
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
        pendingRoles = new PendingRoleTable();

        table.register(server.plugin());
        rolePermissions.register(server.plugin());
        pendingRoles.register(server.plugin());

        table = server.plugin()
                .getDatabase()
                .getTable(RoleTable.class);

        rolePermissions = server.plugin()
                .getDatabase()
                .getTable(RolePermissionTable.class);

        pendingRoles = server.plugin()
                .getDatabase()
                .getTable(PendingRoleTable.class);

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

        if (pendingRoles == null) {
            throw new IllegalStateException(
                    "PendingRoleTable konnte nicht registriert werden."
            );
        }

        tablesRegistered = true;
    }

    private void notifyChange(Player player) {
        Consumer<Player> listener = changeListener;

        if (listener == null || player == null) {
            return;
        }

        // Bei Folia laeuft der Rollenwechsel auf dem Region-Thread. Der
        // Callback soll aber wie ein normaler Befehl auf dem Haupt-Thread
        // laufen (Tab-Refresh, Op-Sync), deshalb wird er dort geplant.
        server.scheduler().runEntity(player, () -> {
            if (player.isOnline()) {
                listener.accept(player);
            }
        });
    }
}
