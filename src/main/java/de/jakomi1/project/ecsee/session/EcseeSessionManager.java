/*
 * EcseeSessionManager – Enderchest-Pendant zum InvseeSessionManager.
 * Basiert auf dem Port des Plugins "Invsee" (at.noahb.invsee, Autor: MCmitNoah,
 * GNU General Public License v3.0, GPL-3.0). Siehe LICENSE.md in diesem Repository.
 *
 * Erstellt von Jakomi1 (10.10.2026) für ProjectPlugin.
 */
package de.jakomi1.project.ecsee.session;

import de.jakomi1.project.ProjectServer;
import de.jakomi1.project.invsee.session.SessionManager;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public final class EcseeSessionManager extends SessionManager {

    public EcseeSessionManager(ProjectServer server) {
        super(server.plugin());
    }

    @Override
    protected EcseeSession createSession(OfflinePlayer offlinePlayer, UUID subscriber) {
        EcseeSession session = new EcseeSession(plugin(), offlinePlayer, subscriber);
        addSession(session);
        return session;
    }
}
