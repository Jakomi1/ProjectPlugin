package de.jakomi1.project.ping;

import de.jakomi1.project.ProjectPlugin;
import net.kyori.adventure.text.Component;

public class ServerPing {
    private final PingListener pingListener;

    public ServerPing(ProjectPlugin plugin) {
        this.pingListener = new PingListener();
        pingListener.register(plugin);
    }

    public ServerPing maxPlayers(int maxPlayers) {
        pingListener.setMaxPlayers(maxPlayers);
        return this;
    }

    public ServerPing setMotd(Component motd, Component subMotd) {
        pingListener.setMainMotd(motd);
        pingListener.setSubMotd(subMotd);
        return this;
    }

    public ServerPing hideOnlinePlayers(boolean hideOnlinePlayers) {
        pingListener.setHidePlayers(hideOnlinePlayers);
        return this;
    }
}
