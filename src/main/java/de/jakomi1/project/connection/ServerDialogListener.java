package de.jakomi1.project.connection;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import de.jakomi1.listener.EventListener;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.event.EventHandler;

import java.util.UUID;

public final class ServerDialogListener extends EventListener {

    private final ServerDialogManager manager;

    public ServerDialogListener(ServerDialogManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onConfigure(AsyncPlayerConnectionConfigureEvent event) {
        manager.handleConfigure(event.getConnection());
    }

    @EventHandler
    public void onDialogClick(PlayerCustomClickEvent event) {
        if (!(event.getCommonConnection() instanceof PlayerConfigurationConnection connection)) {
            return;
        }

        UUID uniqueId = connection.getProfile().getId();
        Key key = event.getIdentifier();

        if (uniqueId != null) {
            manager.handleClick(uniqueId, key);
        }
    }

    @EventHandler
    public void onConnectionClose(PlayerConnectionCloseEvent event) {
        manager.clear(event.getPlayerUniqueId());
    }
}
