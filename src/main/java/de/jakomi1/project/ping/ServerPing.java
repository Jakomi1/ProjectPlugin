package de.jakomi1.project.ping;

import de.jakomi1.project.ProjectPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;

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

    /** Liefert die maximale Spieleranzahl dynamisch pro Ping. */
    public ServerPing maxPlayers(IntSupplier maxPlayersProvider) {
        pingListener.setMaxPlayersProvider(maxPlayersProvider);
        return this;
    }

    public ServerPing setMotd(Component motd, Component subMotd) {
        pingListener.setMainMotd(motd);
        pingListener.setSubMotd(subMotd);
        return this;
    }

    /** Liefert die MOTD dynamisch pro Ping. */
    public ServerPing motd(Function<ServerListPingEvent, Component> motdProvider) {
        pingListener.setMotdProvider(motdProvider);
        return this;
    }

    public ServerPing hideOnlinePlayers(boolean hideOnlinePlayers) {
        pingListener.setHidePlayers(hideOnlinePlayers);
        return this;
    }

    /** Entscheidet dynamisch pro Ping, ob die Online-Spieler versteckt werden. */
    public ServerPing hideOnlinePlayers(BooleanSupplier hideOnlinePlayersProvider) {
        pingListener.setHidePlayersProvider(hideOnlinePlayersProvider);
        return this;
    }
}
