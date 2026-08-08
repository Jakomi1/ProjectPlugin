package de.jakomi1.project;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import de.jakomi1.util.CenterMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class PingListener extends EventListener {

    private static CachedServerIcon cachedIcon;
    private static boolean iconChecked;

    private Component mainMotd = Component.empty();
    private Component subMotd = Component.empty();
    private int maxPlayers = 0;
    private int numPlayers = 0;
    private boolean hidePlayers = false;
    private boolean centerMotd = true;
    private String iconPath = "assets/icon.png";

    public PingListener() {
        ensureIconLoaded();
    }

    public PingListener setMainMotd(Component mainMotd) {
        this.mainMotd = mainMotd == null ? Component.empty() : mainMotd;
        return this;
    }

    public PingListener setSubMotd(Component subMotd) {
        this.subMotd = subMotd == null ? Component.empty() : subMotd;
        return this;
    }

    public PingListener setMaxPlayers(int maxPlayers) {
        this.maxPlayers = Math.max(0, maxPlayers);
        return this;
    }

    public PingListener setNumPlayers(int numPlayers) {
        this.numPlayers = Math.max(0, numPlayers);
        return this;
    }

    public PingListener setHidePlayers(boolean hidePlayers) {
        this.hidePlayers = hidePlayers;
        return this;
    }

    public PingListener setCenterMotd(boolean centerMotd) {
        this.centerMotd = centerMotd;
        return this;
    }

    public PingListener setIcon(CachedServerIcon icon) {
        cachedIcon = icon;
        iconChecked = true;
        return this;
    }

    public PingListener setIconPath(@NotNull String path) {
        this.iconPath = path;
        return this;
    }

    public PingListener loadIcon() {
        iconChecked = false;
        ensureIconLoaded();
        return this;
    }

    private void ensureIconLoaded() {
        if (iconChecked) return;
        if (plugin == null) return;

        File iconFile = new File(
                plugin.getDataFolder(),
                iconPath

        );

        if (!iconFile.exists()) {
            iconChecked = true;
            return;
        }

        try {
            cachedIcon = plugin.getServer().loadServerIcon(iconFile);
        } catch (Exception ignored) {
            cachedIcon = null;
        } finally {
            iconChecked = true;
        }
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {
        ensureIconLoaded();

        if (cachedIcon != null) {
            event.setServerIcon(cachedIcon);
        }

        event.setNumPlayers(numPlayers);
        event.setMaxPlayers(maxPlayers);
        event.setHidePlayers(hidePlayers);

        event.motd(mainMotd.append(subMotd));

        if (centerMotd) {
            centerMotd(event);
        }
    }

    private void centerMotd(ServerListPingEvent event) {
        String motd = event.getMotd();
        String[] lines = motd.split("\n", -1);

        String line1 = lines.length > 0
                ? CenterMessage.centerMotD(lines[0])
                : "";

        String line2 = lines.length > 1
                ? CenterMessage.centerMotD(lines[1])
                : "";

        event.setMotd(
                lines.length > 1
                        ? line1 + "\n" + line2
                        : line1
        );
    }
}