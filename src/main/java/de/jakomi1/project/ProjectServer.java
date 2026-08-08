package de.jakomi1.project;

import de.jakomi1.database.table.GlobalSettingsTable;
import de.jakomi1.project.invsee.InvseeManager;
import de.jakomi1.project.permission.RoleManager;
import de.jakomi1.project.ping.ServerPing;
import de.jakomi1.project.region.RegionProtection;
import de.jakomi1.project.scheduler.Scheduler;
import de.jakomi1.project.state.StateManager;
import de.jakomi1.project.tab.NametagManager;
import de.jakomi1.project.tab.TabManager;
import de.jakomi1.project.world.WorldPerformance;
import net.kyori.adventure.text.Component;

public class ProjectServer {
    private final ProjectPlugin plugin;
    private final Scheduler scheduler;
    private final ServerPing serverPing;
    private Component title = Component.empty();
    private Component prefix = Component.empty();
    private final GlobalSettingsTable globalSettings;
    private final StateManager stateManager;
    private final RoleManager roleManager;
    private final NametagManager nametagManager;
    private final TabManager tabManager;
    private final RegionProtection regionProtection;
    private final WorldPerformance worldPerformance;
    private final InvseeManager invseeManager;
    public ProjectServer(ProjectPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = new Scheduler(plugin);

        this.serverPing = new ServerPing(plugin);

        this.globalSettings = new GlobalSettingsTable();
        this.globalSettings.register(plugin);

        this.stateManager = new StateManager(this, globalSettings);
        this.roleManager = new RoleManager(this);

        this.nametagManager = new NametagManager(this);
        this.tabManager = new TabManager(this);
        this.regionProtection = new RegionProtection(this);
        this.worldPerformance = new WorldPerformance(this);
        this.invseeManager = new InvseeManager(this);
    }

    public ProjectPlugin plugin() {
        return plugin;
    }

    public Scheduler scheduler() {
        return scheduler;
    }

    public ServerPing serverPing() {
        return serverPing;
    }

    public StateManager stateManager() {
        return stateManager;
    }

    public RoleManager permissions() {
        return roleManager;
    }

    public NametagManager nametags() {
        return nametagManager;
    }

    public TabManager tab() {
        return tabManager;
    }

    public RegionProtection regionProtection() {
        return regionProtection;
    }

    public WorldPerformance worldPerformance() {
        return worldPerformance;
    }

    public InvseeManager invsee() {
        return invseeManager;
    }

    public ProjectServer prefix(Component prefix) {
        this.prefix = prefix;
        plugin.setPrefix(prefix);
        return this;
    }

    public ProjectServer title(Component title) {
        this.title = title;
        plugin.setTitle(title);
        stateManager.refresh();
        return this;
    }

    public Component title() {
        return title;
    }

    public void registerEverything() {
        plugin.getRegistry().getRegisterable().forEach(registerable -> registerable.register(plugin));
    }
}
