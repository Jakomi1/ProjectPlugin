package de.jakomi1.project.region;

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

/**
 * Region-Schutz basierend auf Biomen (übernommen aus dem RegionChangeListener).
 *
 * Änderungen an Blöcken in geschützten Biomen werden abgefangen. Optional lassen
 * sich sekundäre Biome registrieren, die nur gegen Feuer/Explosionen geschützt
 * sind. Spieler mit Bypass (Standard: Scoreboard-Tag {@code adminmode}) dürfen
 * weiterhin bauen.
 */
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

    /**
     * Aktiviert den Schutz und registriert den Listener.
     */
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

    /**
     * Deaktiviert den Schutz: Listener abmelden.
     */
    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;

        if (protectionListener != null) {
            protectionListener.unregister();
        }
    }

    /**
     * Schützt ein Biome vollständig (wie Spawn).
     */
    public RegionProtection protect(Biome biome) {
        if (biome != null) primaryBiomes.add(biome);
        return this;
    }

    /**
     * Schützt ein Biome per Registry-Key (z.B. {@code "crackedattack:spawn"}).
     */
    public RegionProtection protect(String key) {
        if (key == null) return this;

        try {
            return protect(Key.key(key));
        } catch (IllegalArgumentException ignored) {
            return this;
        }
    }

    /**
     * Schützt ein Biome per Registry-Key.
     */
    public RegionProtection protect(Key key) {
        Biome biome = biome(key);
        if (biome != null) primaryBiomes.add(biome);
        return this;
    }

    /**
     * Schützt ein Biome nur gegen Feuer/Explosionen (wie Shopping District).
     */
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

    /**
     * Legt fest, wann ein Spieler vom Schutz ausgenommen ist.
     * Standard: Spieler mit Scoreboard-Tag {@code adminmode}.
     */
    public RegionProtection bypass(Predicate<Player> bypass) {
        this.bypass = bypass == null ? player -> false : bypass;
        return this;
    }

    /**
     * Setzt das Zentrum, um das Glasblöcke beim Rechtsklick zum Spawn teleportieren.
     * Deaktiviert, solange es {@code null} ist.
     */
    public RegionProtection glassTeleportCenter(Location center) {
        this.glassTeleportCenter = center;
        return this;
    }

    /**
     * Ziel des Glas-Teleports (z.B. der Spawn).
     */
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

    /**
     * Prüft, ob eine Aktion an einem Block unterbunden werden muss.
     */
    public boolean shouldCancel(@Nullable Player player, Block block, boolean includeSecondary) {
        if (block == null || !isProtected(block.getBiome(), includeSecondary)) return false;
        return player == null || !bypass.test(player);
    }

    /**
     * Behandelt den Glas-Teleport. Liefert {@code true}, wenn der Klick
     * konsumiert wurde.
     */
    public boolean handleGlassClick(Player player, Block clickedBlock) {
        if (!glassTeleportEnabled || spawnTeleport == null || glassTeleportCenter == null) return false;
        if (clickedBlock == null || !clickedBlock.getType().name().contains("GLASS")) return false;
        if (!isProtected(clickedBlock.getBiome())) return false;
        if (clickedBlock.getLocation().distanceSquared(glassTeleportCenter) > 16) return false;

        player.teleportAsync(spawnTeleport);
        return true;
    }

    /**
     * Items, die per Rechtsklick im geschützten Bereich geblockt werden.
     */
    public Set<Material> forbiddenInteractItems() {
        return forbiddenInteractItems;
    }

    /**
     * Items, die über ihren Namen im geschützten Bereich geblockt werden.
     */
    public Set<Material> forbiddenItemsByName() {
        return forbiddenItemsByName;
    }

    /**
     * Blöcke, die über ihren Namen im geschützten Bereich geblockt werden.
     */
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
