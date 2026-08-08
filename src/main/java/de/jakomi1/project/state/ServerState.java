package de.jakomi1.project.state;

public enum ServerState {
    STOPPED,
    STARTED,
    OPEN,
    CLOSED;

    public ServerState next() {
        ServerState[] values = values();
        int next = ordinal() + 1;
        return next < values.length ? values[next] : this;
    }
}
