package de.jakomi1.region;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public final class RegionProtection implements Manager {

    private final ProjectServer server;

    private boolean enabled;

    private RegionProtectionListener protectionListener;

    private final Set<Biome> primaryBiomes = new HashSet<>();
    private final Set<Biome> secondaryBiomes = new HashSet<>();

    private Predicate<Player> bypass = player -> player.getScoreboardTags().contains("adminmode");

    private final Set<Material> forbiddenInteractItems = EnumSet.of(Material.LEAD, Material.END_CRYSTAL);
    private final Set<Material> forbiddenItemsByName = EnumSet.noneOf(Material.class);
    private final Set<Material> forbiddenBlocksByName = EnumSet.noneOf(Material.class);

    private Location glassTeleportCenter;
    private Location spawnTeleport;
    private boolean glassTeleportEnabled = true;

    public RegionProtection(ProjectServer server) {
        this.server = server;
        seedForbiddenByName();
    }

    private void seedForbiddenByName() {
        for (Material material : Material.values()) {
            String name = material.name();
            if (name.contains("DOOR") || name.contains("VINES")) {
                forbiddenBlocksByName.add(material);
            }
            if (name.contains("BUCKET") || name.contains("EGG") || name.contains("CART")
                    || name.contains("BOAT") || name.contains("FRAME") || name.contains("GLASS")
                    || name.contains("PAINT") || name.contains("BONE")) {
                forbiddenItemsByName.add(material);
            }
        }
    }

    @Override
    public RegionProtection enable() {
        if (enabled) return this;
        enabled = true;
        this.protectionListener = new RegionProtectionListener(this);
        protectionListener.register(server.plugin());
        return this;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;

        if (protectionListener != null) {
            protectionListener.unregister();
        }
    }

    public RegionProtection protect(Biome biome) {
        if (biome != null) primaryBiomes.add(biome);
        return this;
    }

    public RegionProtection protect(String key) {
        if (key == null) return this;

        try {
            return protect(Key.key(key));
        } catch (IllegalArgumentException ignored) {
            return this;
        }
    }

    public RegionProtection protect(Key key) {
        Biome biome = biome(key);
        if (biome != null) primaryBiomes.add(biome);
        return this;
    }

    public RegionProtection protectSecondary(Biome biome) {
        if (biome != null) secondaryBiomes.add(biome);
        return this;
    }

    public RegionProtection protectSecondary(String key) {
        if (key == null) return this;

        try {
            return protectSecondary(Key.key(key));
        } catch (IllegalArgumentException ignored) {
            return this;
        }
    }

    public RegionProtection protectSecondary(Key key) {
        Biome biome = biome(key);
        if (biome != null) secondaryBiomes.add(biome);
        return this;
    }

    public RegionProtection bypass(Predicate<Player> bypass) {
        this.bypass = bypass == null ? player -> false : bypass;
        return this;
    }

    public RegionProtection glassTeleportCenter(Location center) {
        this.glassTeleportCenter = center;
        return this;
    }

    public RegionProtection spawnTeleport(Location location) {
        this.spawnTeleport = location;
        return this;
    }

    public RegionProtection glassTeleport(boolean enabled) {
        this.glassTeleportEnabled = enabled;
        return this;
    }

    public boolean isProtected(Biome biome) {
        return primaryBiomes.contains(biome);
    }

    public boolean isProtected(Biome biome, boolean includeSecondary) {
        return primaryBiomes.contains(biome) || (includeSecondary && secondaryBiomes.contains(biome));
    }

    public boolean shouldCancel(@Nullable Player player, Block block, boolean includeSecondary) {
        if (block == null || !isProtected(block.getBiome(), includeSecondary)) return false;
        return player == null || !bypass.test(player);
    }

    public boolean handleGlassClick(Player player, Block clickedBlock) {
        if (!glassTeleportEnabled || spawnTeleport == null || glassTeleportCenter == null) return false;
        if (clickedBlock == null || !clickedBlock.getType().name().contains("GLASS")) return false;
        if (!isProtected(clickedBlock.getBiome())) return false;
        if (clickedBlock.getLocation().distanceSquared(glassTeleportCenter) > 16) return false;

        player.teleportAsync(spawnTeleport);
        return true;
    }

    public Set<Material> forbiddenInteractItems() {
        return forbiddenInteractItems;
    }

    public Set<Material> forbiddenItemsByName() {
        return forbiddenItemsByName;
    }

    public Set<Material> forbiddenBlocksByName() {
        return forbiddenBlocksByName;
    }

    public RegionProtection forbidInteraction(Material material) {
        if (material != null) forbiddenInteractItems.add(material);
        return this;
    }

    public RegionProtection forbidItem(Material material) {
        if (material != null) forbiddenItemsByName.add(material);
        return this;
    }

    public RegionProtection forbidBlock(Material material) {
        if (material != null) forbiddenBlocksByName.add(material);
        return this;
    }

    @Nullable
    private static Biome biome(Key key) {
        try {
            return RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.BIOME)
                    .get(key);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
