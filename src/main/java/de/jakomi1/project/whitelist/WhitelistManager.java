package de.jakomi1.project.whitelist;

import de.jakomi1.project.AutoManager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.project.permission.Role;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

public final class WhitelistManager implements AutoManager {

    private final ProjectServer server;
    private final WhitelistTable table;
    private final WhitelistListener listener;
    private final WhitelistCommand command;

    private boolean enabled;
    private boolean auto = true;
    private boolean registerCommand = true;
    private boolean tableRegistered;
    private Role minimumRole = Role.SUPPORTER;
    private String permission;
    private Role bypassRole = Role.OWNER;
    private Component kickMessage;

    public WhitelistManager(ProjectServer server) {
        this.server = server;
        this.table = new WhitelistTable();
        this.listener = new WhitelistListener(this);
        this.command = new WhitelistCommand(this);
    }

    @Override
    public WhitelistManager enable() {
        if (enabled) return this;
        enabled = true;

        whitelistTable();
        listener.register(server.plugin());

        if (registerCommand) {
            command.register(server.plugin());
        }
        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;

        listener.unregister();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean auto() {
        return auto;
    }

    @Override
    public WhitelistManager auto(boolean auto) {
        this.auto = auto;
        return this;
    }

    public WhitelistManager command(boolean registerCommand) {
        this.registerCommand = registerCommand;
        return this;
    }

    public boolean command() {
        return registerCommand;
    }

    public WhitelistManager minimumRole(Role role) {
        this.minimumRole = role != null ? role : Role.SUPPORTER;
        this.permission = null;
        return this;
    }

    public Role minimumRole() {
        return minimumRole;
    }

    public WhitelistManager permission(String permission) {
        this.permission = permission == null || permission.isBlank() ? null : permission;
        return this;
    }

    public String permission() {
        if (permission != null) return permission;

        return server.permissions().permissionPrefix()
                + "."
                + minimumRole.name().toLowerCase(Locale.ROOT);
    }

    public WhitelistManager bypassRole(Role role) {
        this.bypassRole = role;
        return this;
    }

    public Role bypassRole() {
        return bypassRole;
    }

    public boolean bypass(UUID uuid) {
        if (bypassRole == null || uuid == null) return false;

        return server.permissions().roleOf(uuid).inherits(bypassRole);
    }

    public WhitelistManager kickMessage(Component message) {
        this.kickMessage = message;
        return this;
    }

    public Component kickMessage() {
        if (kickMessage != null) return kickMessage;

        Component prefix = server.plugin().getPrefix();
        Component base = Component.text("Du bist nicht gewhitelistet.", NamedTextColor.RED);

        return prefix != null ? prefix.append(base) : base;
    }

    public WhitelistTable table() {
        return table;
    }

    public ProjectServer server() {
        return server;
    }

    public boolean isWhitelisted(UUID uuid) {
        return table.isWhitelisted(uuid);
    }

    public boolean isWhitelisted(String name) {
        return table.isWhitelisted(name);
    }

    public boolean isWhitelisted(UUID uuid, String name) {
        return table.isWhitelisted(uuid, name);
    }

    public WhitelistManager add(String name) {
        table.add(name);
        return this;
    }

    public WhitelistManager add(UUID uuid) {
        table.add(uuid);
        return this;
    }

    public WhitelistManager add(UUID uuid, String name) {
        table.add(uuid, name);
        return this;
    }

    public WhitelistManager remove(String name) {
        table.removeEntry(name);
        return this;
    }

    public WhitelistManager remove(UUID uuid) {
        table.removeEntry(uuid);
        return this;
    }

    public Collection<WhitelistTable.WhitelistEntry> all() {
        return table.all();
    }

    public int count() {
        return table.count();
    }
}
