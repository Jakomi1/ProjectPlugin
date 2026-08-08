/*
 * InvseeSessionManager – Port aus dem Plugin "Invsee".
 * Original: at.noahb.invsee (Autor: MCmitNoah), GNU General Public License v3.0 (GPL-3.0).
 * Siehe LICENSE.md in diesem Repository.
 *
 * Geändert von Jakomi1 (08.08.2026) für ProjectPlugin: an die Library-Architektur
 * (ProjectServer/Manager) angepasst, auf Paper 26.2 aktualisiert.
 */
package de.jakomi1.project.invsee.session;

import de.jakomi1.project.ProjectServer;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public final class InvseeSessionManager extends SessionManager {

    public InvseeSessionManager(ProjectServer server) {
        super(server.plugin());
    }

    @Override
    protected InvseeSession createSession(OfflinePlayer offlinePlayer, UUID subscriber) {
        InvseeSession invseeSession = new InvseeSession(plugin(), offlinePlayer, subscriber);
        addSession(invseeSession);
        return invseeSession;
    }
}
