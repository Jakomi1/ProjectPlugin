package de.jakomi1.project;

public interface Manager {

    Manager enable();

    default void disable() {
    }

    boolean isEnabled();
}
