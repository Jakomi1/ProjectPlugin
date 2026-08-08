package de.jakomi1.project;

import de.jakomi1.database.table.GlobalSettingsTable;
import net.kyori.adventure.text.Component;

public class ProjectServer {
    private final ProjectPlugin plugin;
    private final ServerPing serverPing;
    private Component title = Component.empty();
    private Component prefix = Component.empty();
    private final GlobalSettingsTable globalSettings;
    public ProjectServer(ProjectPlugin plugin) {
        this.plugin = plugin;
        Scheduler.init(plugin);

        this.serverPing = new ServerPing(plugin);

        this.globalSettings = new GlobalSettingsTable();
        this.globalSettings.register(plugin);
    }

    public ServerPing serverPing() {
        return serverPing;
    }

    public ProjectServer prefix(Component prefix) {
        this.prefix = prefix;
        plugin.setPrefix(prefix);
        return this;
    }

    public ProjectServer title(Component title) {
        this.title = title;
        plugin.setTitle(title);
        serverPing.setMotd(title, Component.empty());
        return this;
    }

    public void registerEverything() {
        plugin.getRegistry().getRegisterable().forEach(registerable -> registerable.register(plugin));
    }
}
