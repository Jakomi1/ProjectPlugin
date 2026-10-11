/*
 * EcseeListener – Enderchest-Pendant zum InvseeListener.
 * Basiert auf dem Port des Plugins "Invsee" (at.noahb.invsee, Autor: MCmitNoah,
 * GNU General Public License v3.0, GPL-3.0). Siehe LICENSE.md in diesem Repository.
 *
 * Erstellt von Jakomi1 (10.10.2026) für ProjectPlugin. Die Enderchest hat keine
 * Rüstungs-/Offhand-/Cursor-Slots, daher entfällt die Platzhalter-Logik.
 */
package de.jakomi1.project.ecsee.listener;

import de.jakomi1.listener.EventListener;
import de.jakomi1.project.ecsee.EcseeManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public final class EcseeListener extends EventListener {

    private final EcseeManager manager;

    public EcseeListener(EcseeManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        this.manager.sessions().removeSubscriberFromSession(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (InventoryAction.NOTHING == event.getAction()) {
            return;
        }
        handle(event.getWhoClicked());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        handle(event.getWhoClicked());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        handle(event.getPlayer());
    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        handle(event.getEntity());
    }

    private void handle(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        player.getScheduler().run(plugin, scheduledTask -> this.manager.sessions().updateContent(player), null);
    }
}
