package de.jakomi1.project;

import net.kyori.adventure.text.Component;

public class ServerPing {
    private final PingListener pingListener;

    public ServerPing(ProjectPlugin plugin) {
        this.pingListener = new PingListener();
        pingListener.register(plugin);
    }

    protected ServerPing maxPlayers(int maxPlayers) {
        pingListener.setMaxPlayers(maxPlayers);
        return this;
    }

    protected ServerPing setMotd(Component motd, Component subMotd) {
        pingListener.setMainMotd(motd);
        pingListener.setSubMotd(subMotd);
        return this;
    }

    protected ServerPing hideOnlinePlayers(boolean hideOnlinePlayers) {
        pingListener.setHidePlayers(hideOnlinePlayers);
        return this;
    }


}
