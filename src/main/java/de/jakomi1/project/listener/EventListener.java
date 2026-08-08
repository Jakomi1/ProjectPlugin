package de.jakomi1.project.listener;

import de.jakomi1.project.ProjectPlugin;
import de.jakomi1.project.Registerable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class EventListener implements Listener, Registerable {
    protected ProjectPlugin plugin;

    @Override
    public void register(ProjectPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
    }

    /**
     * Meldet diesen Listener wieder ab (wird beim {@code disable()} von
     * Managern verwendet).
     */
    public void unregister() {
        HandlerList.unregisterAll(this);
    }
}
