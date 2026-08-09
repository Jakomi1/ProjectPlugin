package de.jakomi1.project.combat;

import de.jakomi1.project.listener.EventListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class CombatListener extends EventListener {

    private final CombatManager combat;

    public CombatListener(CombatManager combat) {
        this.combat = combat;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Player victim = event.getEntity() instanceof Player ? (Player) event.getEntity() : null;
        Player attacker = resolveAttacker(event.getDamager());
        if (victim == null || attacker == null) return;

        combat.mark(attacker);
        combat.mark(victim);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        combat.refresh(event.getPlayer());
    }

    private Player resolveAttacker(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }

        if (damager instanceof org.bukkit.projectiles.ProjectileSource source) {
            return source instanceof Player ? (Player) source : null;
        }

        return null;
    }
}
