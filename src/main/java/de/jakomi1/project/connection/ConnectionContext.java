package de.jakomi1.project.connection;

import io.papermc.paper.connection.PlayerConfigurationConnection;
import net.kyori.adventure.audience.Audience;

import java.util.UUID;

public final class ConnectionContext {

    private final PlayerConfigurationConnection connection;

    public ConnectionContext(PlayerConfigurationConnection connection) {
        this.connection = connection;
    }

    public PlayerConfigurationConnection connection() {
        return connection;
    }

    public UUID uniqueId() {
        return connection.getProfile().getId();
    }

    public String name() {
        return connection.getProfile().getName();
    }

    public Audience audience() {
        return connection.getAudience();
    }
}
