package de.jakomi1.project.connection;

import net.kyori.adventure.text.Component;

public final class ConnectionResult {

    private static final ConnectionResult ALLOW = new ConnectionResult(true, null, null);

    private final boolean allowed;
    private final Component disconnectMessage;
    private final ServerConnectionDialog dialog;

    private ConnectionResult(boolean allowed, Component disconnectMessage, ServerConnectionDialog dialog) {
        this.allowed = allowed;
        this.disconnectMessage = disconnectMessage;
        this.dialog = dialog;
    }

    public static ConnectionResult allow() {
        return ALLOW;
    }

    public static ConnectionResult disconnect(Component message) {
        return new ConnectionResult(false, message, null);
    }

    public static ConnectionResult dialog(ServerConnectionDialog dialog) {
        return new ConnectionResult(false, null, dialog);
    }

    public boolean allowed() {
        return allowed;
    }

    public Component disconnectMessage() {
        return disconnectMessage;
    }

    public ServerConnectionDialog dialog() {
        return dialog;
    }
}
