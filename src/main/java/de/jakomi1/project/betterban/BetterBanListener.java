package de.jakomi1.project.betterban;

import de.jakomi1.listener.EventListener;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

import java.util.Locale;
import java.util.UUID;

/**
 * Setzt Bans, Chat-Sperren und das {@code has_joined}-Tracking durch.
 *
 * <p>Der Ban wird bereits beim asynchronen Pre-Login geprueft, damit ein
 * gebannter Spieler den Server nicht betritt. Chat-Sperren greifen sowohl
 * im modernen {@link AsyncChatEvent} als auch bei privaten Nachrichten ueber
 * {@link PlayerCommandPreprocessEvent}.</p>
 */
public final class BetterBanListener extends EventListener {

    private static final String[] MESSAGE_COMMANDS = {
            "msg", "tell", "w", "whisper",
            "teammsg", "minecraft:teammsg",
            "minecraft:msg", "minecraft:tell", "minecraft:w", "minecraft:whisper"
    };

    private final BetterBanManager manager;

    public BetterBanListener(BetterBanManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();

        if (!manager.hasJoined(uuid)) {
            manager.markJoined(uuid, event.getName());
        }

        BanEntry entry = manager.banEntry(uuid);

        if (entry != null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    manager.messages().banMessage(entry));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!manager.isChatBanned(uuid)) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(manager.messages().chatBanMessage(manager.chatBanEntry(uuid)));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!manager.isChatBanned(uuid)) {
            return;
        }

        String message = event.getMessage().toLowerCase(Locale.ROOT);

        for (String command : MESSAGE_COMMANDS) {
            if (message.startsWith("/" + command + " ")) {
                event.setCancelled(true);
                player.sendMessage(manager.messages().chatBanMessage(manager.chatBanEntry(uuid)));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        manager.markJoined(player.getUniqueId(), player.getName());

        manager.server().scheduler().runLater(
                () -> manager.refreshVoiceDisabled(player.getUniqueId()),
                40L
        );
    }

    /**
     * voicechat kann je nach Lade-Reihenfolge erst nach ProjectPlugin
     * aktiviert werden. Dann wird die Integration hier nachgezogen.
     */
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equalsIgnoreCase("voicechat")) {
            manager.tryInstallVoiceChat();
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().getName().equalsIgnoreCase("voicechat")) {
            manager.onVoiceChatUnavailable();
        }
    }
}
