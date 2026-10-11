package de.jakomi1.project.respawn;

import de.jakomi1.listener.EventListener;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Haelt den gespeicherten Respawn-Punkt aktuell.
 *
 * <p>Gespeichert wird beim erfolgreichen Bettbezug, beim Entzuenden eines
 * Respawn Anchors und beim tatsaechlichen Respawn auf Bett oder Anker.
 * Fehlt der Punkt beim Respawn - Bett abgebaut, Welt nicht geladen - wird er
 * entfernt, damit der Spieler beim naechsten Join wieder sauber zum Spawn
 * geschickt wird.
 */
public final class RespawnListener extends EventListener {

    private final RespawnManager manager;

    public RespawnListener(RespawnManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedEnter(PlayerBedEnterEvent event) {
        if (event.isCancelled()) return;

        manager.remember(event.getPlayer(), event.getBed());
    }

    /**
     * Ein Bett speichert bereits beim Schlafen einen Punkt, ein Respawn Anchor
     * erst beim Entzuenden - vorher existiert dort gar kein Respawn-Punkt.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.FLINT_AND_STEEL) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.RESPAWN_ANCHOR) return;

        Player player = event.getPlayer();
        manager.remember(player, block);

        // Fuer die Explosion festhalten, wer den Anker ausgeloest hat.
        manager.server().combat().trackAnchor(player, block);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (event.isBedSpawn() || event.isAnchorSpawn()) {
            manager.remember(player, event.getRespawnLocation());
            return;
        }

        if (event.isMissingRespawnBlock()) {
            manager.forget(player);
        }

        // Ohne Bett landet der Spieler exakt auf dem Weltspawn. Der
        // Spawn-Radius der Welt darf dabei nicht eingreifen - nur so
        // steht der erste Spawn wirklich bei 0/0.
        Location respawn = event.getRespawnLocation();
        Location override = manager.applyRespawn(player, respawn);

        if (override != null) {
            event.setRespawnLocation(override);
        }
    }
}
