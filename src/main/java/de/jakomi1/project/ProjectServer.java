package de.jakomi1.project;

import de.jakomi1.database.table.GlobalSettingsTable;
import de.jakomi1.database.table.SkinTable;
import de.jakomi1.biome.BiomeManager;
import de.jakomi1.dimension.DimensionManager;
import de.jakomi1.project.combat.CombatManager;
import de.jakomi1.project.connection.ServerDialogManager;
import de.jakomi1.project.invsee.InvseeManager;
import de.jakomi1.project.link.LinkManager;
import de.jakomi1.project.pearl.PearlFixManager;
import de.jakomi1.permission.RoleManager;
import de.jakomi1.project.ping.ServerPing;
import de.jakomi1.project.playtime.PlaytimeManager;
import de.jakomi1.region.RegionProtection;
import de.jakomi1.scheduler.Scheduler;
import de.jakomi1.project.scoreboard.ScoreboardManager;
import de.jakomi1.project.sidebar.SidebarManager;
import de.jakomi1.project.bossbar.BossBarManager;
import de.jakomi1.project.skin.SkinManager;
import de.jakomi1.project.state.StateManager;
import de.jakomi1.project.tab.NametagManager;
import de.jakomi1.project.tab.TabManager;
import de.jakomi1.project.team.TeamManager;
import de.jakomi1.project.whitelist.WhitelistManager;
import de.jakomi1.world.WorldPerformance;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
    private final SkinTable skinTable;
    private final SkinManager skinManager;
    private final CombatManager combatManager;
    private final SidebarManager sidebarManager;
    private final BossBarManager bossBarManager;
    private final BiomeManager biomeManager;
    private final DimensionManager dimensionManager;
    private final WhitelistManager whitelistManager;
    private final ScoreboardManager scoreboardManager;
    private final TeamManager teamManager;
    private final PlaytimeManager playtimeManager;
    private final LinkManager linkManager;
    private final ServerDialogManager dialogManager;
    private final PearlFixManager pearlFixManager;

    private final List<Manager> autoManagers;

    public ProjectServer(ProjectPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = new Scheduler(plugin);

        this.serverPing = new ServerPing(plugin);

        this.globalSettings = new GlobalSettingsTable();
        this.globalSettings.register(plugin);

        this.dialogManager = new ServerDialogManager(this);
        this.stateManager = new StateManager(this, globalSettings);
        this.roleManager = new RoleManager(this);

        this.nametagManager = new NametagManager(this);
        this.tabManager = new TabManager(this);

        this.regionProtection = new RegionProtection(this);
        this.worldPerformance = new WorldPerformance(this);
        this.invseeManager = new InvseeManager(this);

        this.skinTable = new SkinTable();
        this.skinTable.register(plugin);
        this.skinManager = new SkinManager(this, skinTable);

        this.combatManager = new CombatManager(this);

        this.biomeManager = new BiomeManager(this);
        this.dimensionManager = new DimensionManager(this);

        this.whitelistManager = new WhitelistManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.teamManager = new TeamManager(this);
        this.playtimeManager = new PlaytimeManager(this);
        this.linkManager = new LinkManager(this);
        this.sidebarManager = new SidebarManager(this);
        this.bossBarManager = new BossBarManager(this);
        this.pearlFixManager = new PearlFixManager(this);

        this.autoManagers = List.of(
                dialogManager,
                invseeManager,
                whitelistManager,
                scoreboardManager,
                teamManager,
                biomeManager,
                dimensionManager,
                playtimeManager,
                linkManager,
                sidebarManager,
                bossBarManager,
                pearlFixManager
        );
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

    public SkinManager skins() {
        return skinManager;
    }

    public CombatManager combat() {
        return combatManager;
    }

    public BiomeManager biomes() {
        return biomeManager;
    }

    public DimensionManager dimensions() {
        return dimensionManager;
    }

    public WhitelistManager whitelist() {
        return whitelistManager;
    }

    public ScoreboardManager scoreboards() {
        return scoreboardManager;
    }

    public TeamManager teams() {
        return teamManager;
    }

    public PlaytimeManager playtime() {
        return playtimeManager;
    }

    public LinkManager links() {
        return linkManager;
    }

    public ServerDialogManager dialogs() {
        return dialogManager;
    }

    public PearlFixManager pearls() {
        return pearlFixManager;
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

    public SidebarManager sidebars() {
        return sidebarManager;
    }

    public BossBarManager bossBars() {
        return bossBarManager;
    }

    public void broadcast(Component message) {
        if (message == null) return;
        Bukkit.broadcast(message);
    }

    public @Nullable World world(String name) {
        return name == null ? null : Bukkit.getWorld(name);
    }

    public CommandSender console() {
        return Bukkit.getConsoleSender();
    }

    public void registerEverything() {
        plugin.getRegistry().getRegisterable().forEach(registerable -> registerable.register(plugin));

        for (Manager manager : autoManagers) {
            if (manager.defaultState()) {
                manager.enable();
            }
        }
    }

    public void disable() {
        for (Manager manager : List.of(
                nametagManager,
                tabManager,
                worldPerformance,
                regionProtection,
                invseeManager,
                roleManager,
                dialogManager,
                combatManager,
                biomeManager,
                dimensionManager,
                whitelistManager,
                scoreboardManager,
                teamManager,
                playtimeManager,
                linkManager,
                sidebarManager,
                bossBarManager,
                pearlFixManager
        )) {
            if (manager.isEnabled()) {
                manager.disable();
            }
        }
    }
}
