/*
 * EcseeManager – Enderchest-Pendant zum InvseeManager.
 * Basiert auf dem Port des Plugins "Invsee" (at.noahb.invsee, Autor: MCmitNoah,
 * GNU General Public License v3.0, GPL-3.0). Siehe LICENSE.md in diesem Repository.
 *
 * Erstellt von Jakomi1 (10.10.2026) für ProjectPlugin: als Manager der
 * Library-Architektur (ProjectServer) umgesetzt.
 */
package de.jakomi1.project.ecsee;

import de.jakomi1.permission.Role;
import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.project.ecsee.listener.EcseeListener;
import de.jakomi1.project.ecsee.session.EcseeSessionManager;

import java.util.Locale;

public final class EcseeManager implements Manager {

    private final ProjectServer server;
    private final EcseeSessionManager sessionManager;
    private final EcseeListener listener;
    private final EcseeCommand command;

    private boolean enabled;
    private boolean registerCommand = true;
    private Role minimumRole = Role.ADMIN;
    private String permission;

    public EcseeManager(ProjectServer server) {
        this.server = server;
        this.sessionManager = new EcseeSessionManager(server);
        this.listener = new EcseeListener(this);
        this.command = new EcseeCommand(this);
    }

    @Override
    public EcseeManager enable() {
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

    public EcseeManager command(boolean registerCommand) {
        this.registerCommand = registerCommand;
        return this;
    }

    public boolean command() {
        return registerCommand;
    }

    public EcseeManager minimumRole(Role role) {
        this.minimumRole = role != null ? role : Role.ADMIN;
        this.permission = null;
        return this;
    }

    public Role minimumRole() {
        return minimumRole;
    }

    public EcseeManager permission(String permission) {
        this.permission = permission == null || permission.isBlank() ? null : permission;
        return this;
    }

    public String permission() {
        if (permission != null) return permission;

        return server.permissions().permissionPrefix()
                + "."
                + minimumRole.name().toLowerCase(Locale.ROOT);
    }

    public EcseeSessionManager sessions() {
        return sessionManager;
    }

    public ProjectServer server() {
        return server;
    }
}
