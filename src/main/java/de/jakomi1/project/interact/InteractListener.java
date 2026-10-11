package de.jakomi1.project.interact;

import de.jakomi1.listener.EventListener;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Reicht Interaktionen mit Item Frames an den {@link InteractManager} weiter.
 *
 * <p>Getoggelt wird nur mit der Haupt-Hand, dem konfigurierten Werkzeug und -
 * falls aktiviert - waehrend des Anschleichens.
 */
public final class InteractListener extends EventListener {

    private final InteractManager manager;

    public InteractListener(InteractManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame frame)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (manager.requireSneaking() && !event.getPlayer().isSneaking()) return;
        if (event.getPlayer().getInventory().getItemInMainHand().getType() != manager.tool()) return;

        manager.toggle(frame);
    }
}
