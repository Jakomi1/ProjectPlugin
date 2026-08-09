/*
 * InvseeListener – Port des InventoryListener aus dem Plugin "Invsee".
 * Original: at.noahb.invsee (Autor: MCmitNoah), GNU General Public License v3.0 (GPL-3.0).
 * Siehe LICENSE.md in diesem Repository.
 *
 * Geändert von Jakomi1 (08.08.2026) für ProjectPlugin: an die Library-Architektur
 * angepasst, auf Paper 26.2 aktualisiert, MaterialTags durch ItemCategories ersetzt.
 */
package de.jakomi1.project.invsee.listener;

import de.jakomi1.project.invsee.InvseeManager;
import de.jakomi1.project.invsee.ItemCategories;
import de.jakomi1.project.invsee.session.InvseeSession;
import de.jakomi1.listener.EventListener;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public final class InvseeListener extends EventListener {

    private final InvseeManager manager;

    public InvseeListener(InvseeManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        this.manager.sessions().removeSubscriberFromSession(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        boolean setEmpty = false;

        if (event.getClickedInventory() != null && event.getClickedInventory().getSize() == 45 && event.getSlot() > 41) {
            event.setCancelled(true);
            return;
        }

        if (InvseeSession.Placeholders.isPlaceholder(event.getCurrentItem())) {
            if (InvseeSession.Placeholders.isCursorPlaceholder(event.getCurrentItem())
                    || InvseeSession.Placeholders.isCursorPlaceholder(event.getCursor())) {
                event.setCancelled(true);
                return;
            }
            if (ItemCategories.isArmor(event.getCursor())
                    || InvseeSession.Placeholders.isOffHandPlaceholder(event.getCurrentItem())) {
                setEmpty = true;
            } else {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getClickedInventory() != null && event.getClickedInventory().getSize() == 45
                && event.getSlot() >= 36 && event.getSlot() < 40) {
            if (!ItemCategories.isArmor(event.getCursor()) && !event.getCursor().isEmpty()) {
                event.setCancelled(true);
                return;
            }

            if (!InvseeSession.ArmorSlot.values()[39 - event.getSlot()].checkIfItemFitsSlot(event.getCursor())
                    && !event.getCursor().isEmpty()) {
                event.setCancelled(true);
                return;
            }
        }

        if (InventoryAction.NOTHING == event.getAction()) {
            return;
        }

        if (setEmpty) event.setCurrentItem(ItemStack.empty());
        handle(event.getWhoClicked());
    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        handle(event.getEntity());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (InvseeSession.Placeholders.isPlaceholder(event.getOldCursor())) {
            event.setCancelled(true);
            return;
        }
        handle(event.getWhoClicked());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        handle(event.getPlayer());
    }

    private void handle(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        player.getScheduler().run(plugin, scheduledTask -> this.manager.sessions().updateContent(player), null);
    }
}
