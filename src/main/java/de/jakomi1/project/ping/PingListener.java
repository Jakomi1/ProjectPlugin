package de.jakomi1.project.ping;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import de.jakomi1.listener.EventListener;
import de.jakomi1.util.CenterMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class PingListener extends EventListener {

    private static CachedServerIcon cachedIcon;
    private static boolean iconChecked;

    private Component mainMotd = Component.empty();
    private Component subMotd = Component.empty();

    private int maxPlayers = -1;
    private int numPlayers = -1;
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
        this.maxPlayers = maxPlayers < 0 ? -1 : Math.max(0, maxPlayers);
        return this;
    }

    public PingListener setNumPlayers(int numPlayers) {
        this.numPlayers = numPlayers < 0 ? -1 : Math.max(0, numPlayers);
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
        if (path == null || path.isBlank()) return this;
        this.iconPath = path;
        return this;
    }

    public PingListener loadIcon() {
        iconChecked = false;
        ensureIconLoaded();
        return this;
    }

    /**
     * Laedt das Server-Icon einmalig und behaelt das fertig kodierte Ergebnis.
     * Eine eigene Datei im DataFolder hat Vorrang; liegt dort keine, wird auf
     * das gebuendelte {@code /iconPath}-Icon des Plugins zurueckgegriffen.
     * Erst wenn beides fehlt, bleibt die Serverliste ohne Favicon.
     */
    private void ensureIconLoaded() {
        if (iconChecked) return;
        if (plugin == null) return;

        try {
            BufferedImage image = readIcon();
            if (image != null) {
                cachedIcon = plugin.getServer().loadServerIcon(image);
            }
        } catch (Exception ignored) {
            cachedIcon = null;
        } finally {
            iconChecked = true;
        }
    }

    private BufferedImage readIcon() throws IOException {
        File override = new File(plugin.getDataFolder(), iconPath);
        if (override.exists()) {
            return ImageIO.read(override);
        }

        // Das gebuendelte Icon liegt im (Shadow-)Jar des Plugins selbst,
        // damit jedes Projekt sein eigenes Favicon mitliefern kann.
        try (InputStream bundled = plugin.getResource(iconPath)) {
            return bundled == null ? null : ImageIO.read(bundled);
        }
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {
        ensureIconLoaded();

        if (cachedIcon != null) {
            event.setServerIcon(cachedIcon);
        }

        if (numPlayers >= 0) {
            event.setNumPlayers(numPlayers);
        }
        if (maxPlayers >= 0) {
            event.setMaxPlayers(maxPlayers);
        }
        if (hidePlayers) {
            event.setHidePlayers(true);
        }

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
