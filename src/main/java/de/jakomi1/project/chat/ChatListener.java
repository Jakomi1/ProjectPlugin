package de.jakomi1.project.chat;

import de.jakomi1.listener.EventListener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ChatListener extends EventListener {

    private final ChatManager manager;

    public ChatListener(ChatManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!manager.isEnabled()) return;

        Player sender = event.getPlayer();
        Component result = manager.format(sender, event.message());

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.equals(sender) && !manager.visible(target)) {
                continue;
            }

            manager.server().scheduler().runEntity(target, () -> target.sendMessage(result));
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!manager.isEnabled()) return;

        Component message = manager.joinMessage(event.getPlayer());
        if (message != null) {
            event.joinMessage(message);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!manager.isEnabled()) return;

        Component message = manager.quitMessage(event.getPlayer());
        if (message != null) {
            event.quitMessage(message);
        }
    }
}
