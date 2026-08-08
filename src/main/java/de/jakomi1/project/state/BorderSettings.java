package de.jakomi1.project.state;

import org.bukkit.World;
import org.bukkit.WorldBorder;

public final class BorderSettings {

    private final double size;
    private final double centerX;
    private final double centerZ;

    private BorderSettings(double size, double centerX, double centerZ) {
        this.size = size;
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    public static BorderSettings of(double size, double centerX, double centerZ) {
        return new BorderSettings(size, centerX, centerZ);
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
