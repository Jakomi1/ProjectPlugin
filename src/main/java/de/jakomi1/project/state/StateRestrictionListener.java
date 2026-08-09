package de.jakomi1.project.state;

import de.jakomi1.project.listener.EventListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class StateRestrictionListener extends EventListener {

    private final StateManager manager;

    public StateRestrictionListener(StateManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!manager.allowsMovement(player)) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!manager.allowsDamage(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!manager.allowsBreaking(player)) {
            event.setCancelled(true);
        }
    }
}
