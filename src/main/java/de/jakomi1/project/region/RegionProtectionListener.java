package de.jakomi1.project.region;

import de.jakomi1.project.listener.EventListener;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.MoistureChangeEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

public final class RegionProtectionListener extends EventListener {

    private final RegionProtection protection;

    public RegionProtectionListener(RegionProtection protection) {
        this.protection = protection;
    }

    private boolean shouldCancel(Player player, Block block, boolean includeSecondary) {
        return protection.shouldCancel(player, block, includeSecondary);
    }

    private boolean shouldCancel(Block block, boolean includeSecondary) {
        return protection.shouldCancel(null, block, includeSecondary);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Block clickedBlock = e.getClickedBlock();
        Block target = clickedBlock != null ? clickedBlock : e.getPlayer().getLocation().getBlock();

        if (protection.handleGlassClick(e.getPlayer(), clickedBlock)) {
            return;
        }

        if (e.getAction().isRightClick() && shouldCancel(e.getPlayer(), target, false)) {
            boolean blockCancelled = clickedBlock != null
                    && protection.forbiddenBlocksByName().contains(clickedBlock.getType());

            ItemStack item = e.getItem();
            boolean itemCancelled = false;
            if (item != null) {
                Material type = item.getType();
                itemCancelled = protection.forbiddenInteractItems().contains(type)
                        || protection.forbiddenItemsByName().contains(type);
            }

            if (blockCancelled || itemCancelled) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void projectileLaunch(ProjectileLaunchEvent e) {
        Player player = e.getEntity().getShooter() instanceof Player p ? p : null;
        if (shouldCancel(player, e.getLocation().getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (shouldCancel(e.getPlayer(), e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (shouldCancel(e.getPlayer(), e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        Block block = e.getRightClicked().getLocation().getBlock();
        if (shouldCancel(e.getPlayer(), block, false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent e) {
        if (shouldCancel(e.getPlayer(), e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHangingBreak(HangingBreakEvent e) {
        Player player = null;
        if (e instanceof HangingBreakByEntityEvent byEntity && byEntity.getRemover() instanceof Player p) {
            player = p;
        }

        Block block = e.getEntity().getLocation().getBlock();
        if (shouldCancel(player, block, false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (shouldCancel(e.getPlayer(), e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (shouldCancel(e.getPlayer(), e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMultiPlace(BlockMultiPlaceEvent e) {
        if (shouldCancel(e.getPlayer(), e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCook(BlockCookEvent e) {
        if (shouldCancel(e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDispense(BlockDispenseEvent e) {
        if (shouldCancel(e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDropItem(BlockDropItemEvent e) {
        if (shouldCancel(e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpongeAbsorb(SpongeAbsorbEvent e) {
        if (shouldCancel(e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityPlace(EntityPlaceEvent e) {
        if (shouldCancel(e.getPlayer(), e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent e) {
        if (shouldCancel(e.getEntity().getLocation().getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onGrow(BlockGrowEvent e) {
        if (shouldCancel(e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpread(BlockSpreadEvent e) {
        if (shouldCancel(e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFade(BlockFadeEvent e) {
        if (shouldCancel(e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onForm(BlockFormEvent e) {
        if (shouldCancel(e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent e) {
        if (shouldCancel(e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent e) {
        if (shouldCancel(e.getBlock(), true)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBurn(BlockBurnEvent e) {
        if (shouldCancel(e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFertilize(BlockFertilizeEvent e) {
        if (shouldCancel(e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMoistureChange(MoistureChangeEvent e) {
        if (shouldCancel(e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCauldronChange(CauldronLevelChangeEvent e) {
        if (shouldCancel(e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFromTo(BlockFromToEvent e) {
        if (shouldCancel(e.getToBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityChange(EntityChangeBlockEvent e) {
        if (shouldCancel(e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityBlockForm(EntityBlockFormEvent e) {
        Player player = e.getEntity() instanceof Player p ? p : null;
        if (shouldCancel(player, e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onTNTPrime(TNTPrimeEvent e) {
        if (shouldCancel(e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent e) {
        if (shouldCancel(e.getPlayer(), e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        if (shouldCancel(e.getPlayer(), e.getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent e) {
        if (shouldCancel(e.getPlayer(), e.getFrom().getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityPortal(EntityPortalEvent e) {
        Player player = e.getEntity() instanceof Player p ? p : null;
        if (shouldCancel(player, e.getFrom().getBlock(), false)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block block : e.getBlocks()) {
            if (shouldCancel(block, false) || shouldCancel(block.getRelative(e.getDirection()), false)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        for (Block block : e.getBlocks()) {
            if (shouldCancel(block, false)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent e) {
        for (BlockState state : e.getBlocks()) {
            if (shouldCancel(state.getBlock(), false)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onStructureGrow(StructureGrowEvent e) {
        for (BlockState state : e.getBlocks()) {
            if (shouldCancel(state.getBlock(), false)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(block -> shouldCancel(block, true));
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(block -> shouldCancel(block, true));
    }
}
