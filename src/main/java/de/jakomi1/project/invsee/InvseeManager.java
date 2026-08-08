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

/**
 * Verwaltet die Invsee-Sessions und den zugehörigen Listener.
 *
 * <pre>
 *   server.invsee().enable();   // Listener aktivieren
 *   server.invsee().sessions()  // Zugriff auf die Sessions
 * </pre>
 */
public final class InvseeManager implements Manager {

    private final ProjectServer server;
    private final InvseeSessionManager sessionManager;
    private final InvseeListener listener;

    private boolean enabled;

    public InvseeManager(ProjectServer server) {
        this.server = server;
        this.sessionManager = new InvseeSessionManager(server);
        this.listener = new InvseeListener(this);
    }

    @Override
    public InvseeManager enable() {
        if (enabled) return this;
        enabled = true;

        listener.register(server.plugin());
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

    public InvseeSessionManager sessions() {
        return sessionManager;
    }

    public ProjectServer server() {
        return server;
    }
}
