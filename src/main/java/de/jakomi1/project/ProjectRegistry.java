package de.jakomi1.project;

import org.bukkit.plugin.java.JavaPlugin;

public abstract class ProjectPlugin extends JavaPlugin {

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    public abstract void onEnable(ProjectServer server);
    public abstract void onDisable(ProjectServer server);
}
