package de.jakomi1.project;

public interface Registerable {

    default void register(ProjectPlugin plugin) {
        handleRegister(plugin);

    }

     void handleRegister(ProjectPlugin plugin);
}
