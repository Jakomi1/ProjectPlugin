package de.jakomi1.project;

import de.jakomi1.database.Database;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class ProjectPlugin extends JavaPlugin {
    private ProjectServer server;
    //private static ProjectPlugin instance;
    private Database database;
    private Component prefix;
    private Component title;

    @Override
    public final void onEnable() {
        //instance = this;
        this.database = new Database(this);
        this.server = new ProjectServer(this);

        this.onEnable(this.server);
        server.registerEverything();

    }

    @Override
    public final void onDisable() {

        this.onDisable(this.server);
    }


    /*public static ProjectPlugin getInstance() {
        return instance;
    }*/

    public final Database getDatabase() {
        return database;
    }


    protected final void setPrefix(Component prefix) {
        this.prefix = prefix;
    };

    protected final void setTitle(Component title) {
        this.title = title;
    };

    public final Component getPrefix() {
        return prefix;
    };

    public final Component getTitle() {
        return title;
    };

    protected abstract void onEnable(ProjectServer server);
    protected abstract void onDisable(ProjectServer server);
    protected abstract ProjectRegistry getRegistry();
}
