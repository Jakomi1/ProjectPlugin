/*
 * InvseeManager – Port aus dem Plugin "Invsee".
 * Original: at.noahb.invsee (Autor: MCmitNoah), GNU General Public License v3.0 (GPL-3.0).
 * Siehe LICENSE.md in diesem Repository.
 *
 * Geändert von Jakomi1 (08.08.2026) für ProjectPlugin: als Manager der
 * Library-Architektur (ProjectServer) umgesetzt, auf Paper 26.2 aktualisiert.
 */
package de.jakomi1.project.invsee;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.project.invsee.listener.InvseeListener;
import de.jakomi1.project.invsee.session.InvseeSessionManager;
import de.jakomi1.permission.Role;

import java.util.Locale;

public final class InvseeManager implements Manager {

    private final ProjectServer server;
    private final InvseeSessionManager sessionManager;
    private final InvseeListener listener;
    private final InvseeCommand command;

    private boolean enabled;
    private boolean registerCommand = true;
    private Role minimumRole = Role.ADMIN;
    private String permission;

    public InvseeManager(ProjectServer server) {
        this.server = server;
        this.sessionManager = new InvseeSessionManager(server);
        this.listener = new InvseeListener(this);
        this.command = new InvseeCommand(this);
    }

    @Override
    public InvseeManager enable() {
        if (enabled) return this;
        enabled = true;

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

    public InvseeManager command(boolean registerCommand) {
        this.registerCommand = registerCommand;
        return this;
    }

    public boolean command() {
        return registerCommand;
    }

    public InvseeManager minimumRole(Role role) {
        this.minimumRole = role != null ? role : Role.ADMIN;
        this.permission = null;
        return this;
    }

    public Role minimumRole() {
        return minimumRole;
    }

    public InvseeManager permission(String permission) {
        this.permission = permission == null || permission.isBlank() ? null : permission;
        return this;
    }

    public String permission() {
        if (permission != null) return permission;

        return server.permissions().permissionPrefix()
                + "."
                + minimumRole.name().toLowerCase(Locale.ROOT);
    }

    public InvseeSessionManager sessions() {
        return sessionManager;
    }

    public ProjectServer server() {
        return server;
    }
}
