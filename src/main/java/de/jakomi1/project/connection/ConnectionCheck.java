package de.jakomi1.project.connection;

@FunctionalInterface
public interface ConnectionCheck {

    ConnectionResult check(ConnectionContext context);
}
