package de.jakomi1.project.state;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;

public final class BorderSettings {

    private static final double DEFAULT_CENTER = 0.0;

    private final double size;
    private final double centerX;
    private final double centerZ;

    private BorderSettings(double size, double centerX, double centerZ) {
        this.size = size;
        this.centerX = centerX;
        this.centerZ = centerZ;
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
        return new BorderSettings(size, centerX, centerZ);
    }

    public BorderSettings center(double centerX, double centerZ) {
        return new BorderSettings(size, centerX, centerZ);
    }

    public BorderSettings center(Location center) {
        if (center == null) return this;
        return center(center.getX(), center.getZ());
    }

    public double size() {
        return size;
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
        if (world != null) apply(world.getWorldBorder());
    }
}
