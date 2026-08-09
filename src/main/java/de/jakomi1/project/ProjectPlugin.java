package de.jakomi1.project;

import de.jakomi1.database.Database;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class ProjectPlugin extends JavaPlugin {
    private ProjectServer server;
    private Database database;
    private Component prefix;
    private Component title;
    private String pluginId;

    @Override
    public final void onEnable() {
        this.database = new Database(this);
        this.server = new ProjectServer(this);

        this.onEnable(this.server);
        server.registerEverything();
    }

    @Override
    public final void onDisable() {
        if (server != null) {
            server.disable();
        }
        if (database != null) {
            database.shutdown();
        }
        if (server != null) {
            this.onDisable(server);
        }
    }

    public final Database getDatabase() {
        return database;
    }


    protected final void setPrefix(Component prefix) {
        this.prefix = prefix;
    }

    protected final void setPluginId(String id) {
        this.pluginId = id;
    }

    protected final void setTitle(Component title) {
        this.title = title;
    }

    public final String getId() {
        return pluginId;
    }

    public final Component getPrefix() {
        return prefix;
    }

    public final Component getTitle() {
        return title;
    }

    protected abstract void onEnable(ProjectServer server);
    protected abstract void onDisable(ProjectServer server);
    protected abstract ProjectRegistry getRegistry();
}
