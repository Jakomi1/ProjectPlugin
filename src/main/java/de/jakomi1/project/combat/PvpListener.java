package de.jakomi1.project.combat;

import de.jakomi1.listener.EventListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Setzt die PvP- und Schadensregeln des {@link CombatManager} durch.
 *
 * <p>Deckt normale Treffer, Projektile, TNT, Kristalle und Folgeschaden ab.
 * Geschuetzte Zonen und PvP-Schalter stammen aus dem Manager, ebenso die
 * Hinweistexte.
 */
public final class PvpListener extends EventListener {

    private final CombatManager combat;
    private final Map<UUID, UUID> crystalTriggers = new ConcurrentHashMap<>();

    public PvpListener(CombatManager combat) {
        this.combat = combat;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        combat.handlePvpJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        combat.forgetPvp(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        DamageAttribution.forget(player);

        if (combat.resetAfterDeath(player)) {
            combat.actionBar(player, PvpMessage.DEATH_DISABLED);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof EnderCrystal crystal) {
            crystalTriggers.put(crystal.getUniqueId(), event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        Entity victim = event.getEntity();
        Player player = victim instanceof Player target ? target : null;

        boolean explosion = event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION;

        if (player != null && combat.isSafeZone(player.getLocation())) {
            event.setCancelled(true);

            Player attacker = resolveAttacker(event);
            if (attacker != null && !attacker.equals(player)) {
                combat.actionBar(attacker, PvpMessage.SPAWN_PROTECTED);
            }

            return;
        }

        if (handleSelfOnlyBlast(event, victim, player, explosion) && event.isCancelled()) {
            return;
        }

        if (player == null) {
            if (explosion && combat.isProtectedZone(victim.getLocation())) {
                event.setCancelled(true);
            }
            return;
        }

        Player attacker = resolveAttacker(event);

        if (attacker == null) {
            if (combat.pvpEnabled(player)) return;

            Player recent = DamageAttribution.recentAttacker(player);
            if (recent == null) return;

            if (!pvpAllowed(player, recent, false)) {
                event.setCancelled(true);
            }
            return;
        }

        if (attacker.equals(player)) return;

        if (!pvpAllowed(player, attacker, true)) {
            event.setCancelled(true);
            return;
        }

        combat.mark(attacker);
        combat.mark(player);
        combat.markPvp(attacker);
        combat.markPvp(player);

        DamageAttribution.remember(attacker, player);
    }

    private boolean handleSelfOnlyBlast(EntityDamageEvent event, Entity victim, Player player, boolean explosion) {
        boolean crystal = SelfOnlyExplosions.isCrystalDamage(event);
        if (!crystal && !explosion) return false;

        SelfOnlyExplosions.Blast blast = SelfOnlyExplosions.nearBlast(victim.getLocation());

        if (blast == null && crystal) {
            blast = SelfOnlyExplosions.crystalBlast(event);
        }

        if (blast == null && crystal) {
            blast = new SelfOnlyExplosions.Blast(null);
        }

        if (blast == null) return false;

        if (player == null) {
            if (combat.isProtectedZone(victim.getLocation())) {
                event.setCancelled(true);
            }
            return true;
        }

        if (blast.belongsTo(player)) return false;

        Player trackedAttacker = resolveAttacker(event);
        if (trackedAttacker != null && trackedAttacker.equals(player)) return false;

        boolean selfPrimed = blast.trigger() == null && SelfOnlyExplosions.primedNearby(player);
        if (selfPrimed) return false;

        event.setCancelled(true);
        return true;
    }

    private boolean pvpAllowed(Player victim, Player attacker, boolean notify) {
        if (combat.isProtectedZone(victim.getLocation())) {
            return reject(notify, attacker, PvpMessage.SPAWN_PROTECTED);
        }

        if (!combat.pvpEnabled(attacker)) {
            return reject(notify, attacker, PvpMessage.SELF_PVP_OFF);
        }

        if (!combat.pvpEnabled(victim)) {
            return reject(notify, attacker, PvpMessage.TARGET_PVP_OFF);
        }

        return true;
    }

    private boolean reject(boolean notify, Player attacker, PvpMessage reason) {
        if (notify) {
            combat.actionBar(attacker, reason);
        }
        return false;
    }

    private Player resolveAttacker(EntityDamageEvent event) {
        var source = event.getDamageSource();

        if (source.getCausingEntity() instanceof Player player) {
            return player;
        }

        if (event instanceof EntityDamageByEntityEvent entityEvent) {
            Entity damager = entityEvent.getDamager();

            if (damager instanceof Player player) {
                return player;
            }

            if (damager instanceof EnderCrystal crystal) {
                UUID shooterId = crystalTriggers.remove(crystal.getUniqueId());
                if (shooterId != null) {
                    Player tracked = Bukkit.getPlayer(shooterId);
                    if (tracked != null) return tracked;
                }
                if (source.getCausingEntity() instanceof Player fromSource) {
                    return fromSource;
                }
            }

            if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
                return player;
            }

            if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) {
                return player;
            }
        }

        return DamageAttribution.resolve(event);
    }
}
