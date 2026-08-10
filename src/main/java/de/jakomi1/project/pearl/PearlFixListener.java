package de.jakomi1.project.pearl;

import de.jakomi1.listener.EventListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class PearlFixListener extends EventListener {

    private final PearlFixManager manager;
    private final Map<UUID, Set<UUID>> playerPearls = new ConcurrentHashMap<>();
    private final Map<UUID, WeakReference<EnderPearl>> pearlEntities = new ConcurrentHashMap<>();
    private final Map<UUID, Set<SavedPearlState>> offlinePearls = new ConcurrentHashMap<>();

    PearlFixListener(PearlFixManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getType() != EntityType.ENDER_PEARL) return;

        EnderPearl pearl = (EnderPearl) event.getEntity();
        ProjectileSource source = pearl.getShooter();
        if (!(source instanceof Player player)) return;

        playerPearls.computeIfAbsent(player.getUniqueId(), key -> ConcurrentHashMap.newKeySet())
                .add(pearl.getUniqueId());
        pearlEntities.put(pearl.getUniqueId(), new WeakReference<>(pearl));
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveEvent event) {
        if (event.getEntity().getType() != EntityType.ENDER_PEARL) return;

        UUID pearlId = event.getEntity().getUniqueId();
        pearlEntities.remove(pearlId);
        playerPearls.values().forEach(set -> set.remove(pearlId));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Set<UUID> pearlIds = playerPearls.remove(playerId);
        if (pearlIds == null || pearlIds.isEmpty()) return;

        Set<SavedPearlState> saved = ConcurrentHashMap.newKeySet();

        for (UUID pearlId : pearlIds) {
            WeakReference<EnderPearl> reference = pearlEntities.remove(pearlId);
            if (reference == null) continue;

            EnderPearl pearl = reference.get();
            if (pearl == null || !pearl.isValid()) continue;

            Location location = pearl.getLocation();
            World world = location.getWorld();
            if (world == null) continue;

            Vector velocity = pearl.getVelocity();
            saved.add(new SavedPearlState(
                    pearlId,
                    playerId,
                    world.getUID(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    velocity.getX(),
                    velocity.getY(),
                    velocity.getZ()
            ));

            manager.scheduler().runEntity(pearl, pearl::remove);
        }

        if (!saved.isEmpty()) {
            offlinePearls.put(playerId, saved);
            manager.savePearls(playerId, saved);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        Set<SavedPearlState> fromDisk = manager.loadPearls(playerId);
        Set<SavedPearlState> inMemory = offlinePearls.remove(playerId);

        Set<SavedPearlState> allStates = new HashSet<>();
        if (fromDisk != null) allStates.addAll(fromDisk);
        if (inMemory != null) allStates.addAll(inMemory);

        if (allStates.isEmpty()) return;

        for (SavedPearlState state : allStates) {
            spawnOrAdoptPearl(player, state);
        }

        manager.deletePearls(playerId);
    }

    private void spawnOrAdoptPearl(Player player, SavedPearlState state) {
        World world = Bukkit.getWorld(state.worldId());
        if (world == null) {
            manager.server().plugin().getLogger().warning(
                    "Cannot restore pearl for " + player.getName()
                            + " - world " + state.worldId() + " is not loaded."
            );
            return;
        }

        Location location = new Location(world, state.x(), state.y(), state.z());
        Vector velocity = new Vector(state.vx(), state.vy(), state.vz());

        manager.scheduler().runRegion(location, () -> {
            EnderPearl pearl = world.getEntities().stream()
                    .filter(entity -> entity.getUniqueId().equals(state.pearlId()))
                    .map(entity -> (EnderPearl) entity)
                    .findFirst()
                    .orElseGet(() -> {
                        EnderPearl fresh = (EnderPearl) world.spawnEntity(location, EntityType.ENDER_PEARL);
                        fresh.setVelocity(velocity);
                        return fresh;
                    });

            pearl.setShooter(player);
            pearlEntities.put(pearl.getUniqueId(), new WeakReference<>(pearl));
            playerPearls.computeIfAbsent(player.getUniqueId(), key -> ConcurrentHashMap.newKeySet())
                    .add(pearl.getUniqueId());
        });
    }

    void saveAllPearlStates() {
        for (Map.Entry<UUID, Set<UUID>> entry : playerPearls.entrySet()) {
            UUID playerId = entry.getKey();
            Set<SavedPearlState> saved = new HashSet<>();

            for (UUID pearlId : entry.getValue()) {
                WeakReference<EnderPearl> reference = pearlEntities.get(pearlId);
                if (reference == null) continue;

                EnderPearl pearl = reference.get();
                if (pearl == null || !pearl.isValid()) continue;

                Location location = pearl.getLocation();
                World world = location.getWorld();
                if (world == null) continue;

                Vector velocity = pearl.getVelocity();
                saved.add(new SavedPearlState(
                        pearlId,
                        playerId,
                        world.getUID(),
                        location.getX(),
                        location.getY(),
                        location.getZ(),
                        velocity.getX(),
                        velocity.getY(),
                        velocity.getZ()
                ));
            }

            if (!saved.isEmpty()) {
                manager.savePearls(playerId, saved);
            }
        }
    }
}
