package de.jakomi1.project.connection;

import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.util.Map;

public final class ServerConnectionDialog {

    private final Dialog dialog;
    private final Map<Key, Boolean> responses;
    private final Component disconnectMessage;
    private final Duration timeout;

    public ServerConnectionDialog(
            Dialog dialog,
            Map<Key, Boolean> responses,
            Component disconnectMessage,
            Duration timeout
    ) {
        this.dialog = dialog;
        this.responses = responses == null ? Map.of() : Map.copyOf(responses);
        this.disconnectMessage = disconnectMessage;
        this.timeout = timeout == null ? Duration.ofMinutes(1) : timeout;
    }

    public Dialog dialog() {
        return dialog;
    }

    public Map<Key, Boolean> responses() {
        return responses;
    }

    public Component disconnectMessage() {
        return disconnectMessage;
    }

    public Duration timeout() {
        return timeout;
    }
}
