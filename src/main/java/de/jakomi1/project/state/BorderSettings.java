package de.jakomi1.project.state;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;

import java.util.EnumMap;
import java.util.Map;

public final class BorderSettings {

    private static final double DEFAULT_CENTER = 0.0;

    private final double size;
    private final double centerX;
    private final double centerZ;
    private final Map<World.Environment, Double> environmentSizes;

    private BorderSettings(double size, double centerX, double centerZ, Map<World.Environment, Double> environmentSizes) {
        this.size = size;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.environmentSizes = (environmentSizes == null || environmentSizes.isEmpty())
                ? Map.of()
                : Map.copyOf(environmentSizes);
    }

    private BorderSettings(double size, double centerX, double centerZ) {
        this(size, centerX, centerZ, Map.of());
    }

    public static BorderSettings of(double size) {
        return new BorderSettings(size, DEFAULT_CENTER, DEFAULT_CENTER);
    }

    public static BorderSettings of(double size, double centerX, double centerZ) {
        return new BorderSettings(size, centerX, centerZ);
    }

    public static BorderSettings of(double size, Location center) {
        if (center == null) return of(size);
        return new BorderSettings(size, center.getX(), center.getZ());
    }

    public BorderSettings size(double size) {
        return new BorderSettings(size, centerX, centerZ, environmentSizes);
    }

    public BorderSettings center(double centerX, double centerZ) {
        return new BorderSettings(size, centerX, centerZ, environmentSizes);
    }

    public BorderSettings center(Location center) {
        if (center == null) return this;
        return center(center.getX(), center.getZ());
    }

    public BorderSettings environmentSize(World.Environment environment, double size) {
        if (environment == null) return this;
        Map<World.Environment, Double> copy = new EnumMap<>(World.Environment.class);
        copy.putAll(environmentSizes);
        copy.put(environment, size);
        return new BorderSettings(this.size, centerX, centerZ, copy);
    }

    public BorderSettings endSize(double endSize) {
        return environmentSize(World.Environment.THE_END, endSize);
    }

    public BorderSettings netherSize(double netherSize) {
        return environmentSize(World.Environment.NETHER, netherSize);
    }

    public BorderSettings overworldSize(double overworldSize) {
        return environmentSize(World.Environment.NORMAL, overworldSize);
    }

    public double size() {
        return size;
    }

    public double sizeFor(World.Environment environment) {
        if (environment != null && environmentSizes.containsKey(environment)) {
            return environmentSizes.get(environment);
        }
        return size;
    }

    public double sizeFor(World world) {
        if (world == null) return size;
        return sizeFor(world.getEnvironment());
    }

    public Map<World.Environment, Double> environmentSizes() {
        return environmentSizes;
    }

    public double centerX() {
        return centerX;
    }

    public double centerZ() {
        return centerZ;
    }

    public void apply(WorldBorder border) {
        if (border == null) return;
        border.setCenter(centerX, centerZ);
        border.setSize(size);
    }

    public void applyToWorld(World world) {
        if (world == null) return;
        WorldBorder border = world.getWorldBorder();
        if (border == null) return;
        border.setCenter(centerX, centerZ);
        border.setSize(sizeFor(world));
    }
}
