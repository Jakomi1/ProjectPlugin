package de.jakomi1.projectplugin;

import de.jakomi1.projectplugin.betterban.command.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class BetterBanPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Registriere alle BetterBan Commands
        getCommand("ban").setExecutor(new BanCommand());
        getCommand("ban").setTabCompleter(new BanCommand());

        getCommand("unban").setExecutor(new UnbanCommand());
        getCommand("unban").setTabCompleter(new UnbanCommand());

        getCommand("tempban").setExecutor(new TempBanCommand());
        getCommand("tempban").setTabCompleter(new TempBanCommand());

        getCommand("kick").setExecutor(new KickCommand());
        getCommand("kick").setTabCompleter(new KickCommand());

        getCommand("chatban").setExecutor(new ChatBanCommand());
        getCommand("chatban").setTabCompleter(new ChatBanCommand());

        getCommand("chatunban").setExecutor(new ChatUnbanCommand());
        getCommand("chatunban").setTabCompleter(new ChatUnbanCommand());

        getLogger().info("BetterBan Commands registriert");
    }

    @Override
    public void onDisable() {
        // Plugin deaktiviert
    }
}