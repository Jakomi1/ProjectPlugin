package de.jakomi1.project.pearl;

import java.util.UUID;

public record SavedPearlState(
        UUID pearlId,
        UUID ownerId,
        UUID worldId,
        double x,
        double y,
        double z,
        double vx,
        double vy,
        double vz
) {
}
