package de.jakomi1.project.combat;

import de.jakomi1.listener.EventListener;
import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Fuellt die Luecken, die reine Schadensereignisse nicht abdecken koennen.
 *
 * <ul>
 *   <li>End Crystals und Respawn Anchors bekommen beim Explodieren ein
 *       Zeitfenster, in dem nur der Ausloeser getroffen werden darf.</li>
 *   <li>Crystal- und Anker-Explosionen entzuenden Spieler ueber
 *       {@link EntityCombustByEntityEvent} - dieser Weg wird hier abgefangen.</li>
 *   <li>TNT-Minecarts geben ueber die API keine Quelle preis, deshalb wird
 *       der Zuender selbst gemerkt.</li>
 * </ul>
 */
public final class ExplosionGuardListener extends EventListener {

    /**
     * Ein Respawn Anchor oder Bett explodiert als Block, ein End Crystal als
     * Entitaet. Beides oeffnet dasselbe Zeitfenster.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (SelfOnlyExplosions.isRespawnBlock(event.getBlock())) {
            SelfOnlyExplosions.openBlast(event.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof EnderCrystal crystal) {
            SelfOnlyExplosions.openCrystalBlast(crystal);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        boolean crystal = event.getCombuster() instanceof EnderCrystal;

        SelfOnlyExplosions.Blast blast = SelfOnlyExplosions.nearBlast(player.getLocation());
        if (!crystal && blast == null) return;

        if (blast != null && blast.belongsTo(player)) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity clicked = event.getRightClicked();

        trackPrime(player, clicked, event.getHand());

        if (clicked instanceof ExplosiveMinecart cart && isFlintAndSteel(player, event.getHand())) {
            DamageAttribution.rememberMinecartIgniter(cart, player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        trackPrime(event.getPlayer(), event.getRightClicked(), event.getHand());
    }

    private void trackPrime(Player player, Entity clicked, EquipmentSlot hand) {
        if (!(clicked instanceof EnderCrystal crystal)) return;
        if (!isFlintAndSteel(player, hand)) return;

        SelfOnlyExplosions.trackCrystalOwner(player, crystal);
    }

    private boolean isFlintAndSteel(Player player, EquipmentSlot hand) {
        ItemStack stack = hand == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();

        return stack.getType() == Material.FLINT_AND_STEEL;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        Entity target = event.getEntity();

        if (target instanceof TNTPrimed || target instanceof ExplosiveMinecart) {
            DamageAttribution.rememberMinecartIgniter(target, player);
        }
    }
}
