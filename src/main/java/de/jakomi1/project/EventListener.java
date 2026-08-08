package de.jakomi1.project;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class EventListener implements Listener, Registerable {
    protected ProjectPlugin plugin;

    @Override
    public void handleRegister(ProjectPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
    }
}
