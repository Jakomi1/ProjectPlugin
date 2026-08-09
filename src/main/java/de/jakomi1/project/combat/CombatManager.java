package de.jakomi1.project.combat;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.project.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatManager implements Manager {

    private static final long DEFAULT_DURATION_MS = 20 * 60 * 1000L;
    private static final long CHECK_PERIOD_TICKS = 20L;

    private final ProjectServer server;
    private final Map<UUID, Long> combatUntil = new ConcurrentHashMap<>();
    private final CombatListener listener;

    private boolean enabled;
    private long durationMs = DEFAULT_DURATION_MS;
    private Scheduler.Task expiryTask;

    public CombatManager(ProjectServer server) {
        this.server = server;
        this.listener = new CombatListener(this);
    }

    @Override
    public CombatManager enable() {
        if (enabled) return this;
        enabled = true;

        listener.register(server.plugin());
        expiryTask = server.scheduler().runTimer(this::checkExpiry, CHECK_PERIOD_TICKS, CHECK_PERIOD_TICKS);
        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;

        listener.unregister();
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }
        endAll(CombatEndEvent.Reason.DISABLED);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public CombatManager duration(Duration duration) {
        if (duration != null && !duration.isZero() && !duration.isNegative()) {
            this.durationMs = duration.toMillis();
        }
        return this;
    }

    public CombatManager durationMillis(long millis) {
        if (millis > 0) {
            this.durationMs = millis;
        }
        return this;
    }

    public long durationMillis() {
        return durationMs;
    }

    public CombatManager mark(Player player) {
        return mark(player, durationMs);
    }

    public CombatManager mark(Player player, long durationMillis) {
        if (player == null || durationMillis <= 0) return this;

        combatUntil.put(player.getUniqueId(), System.currentTimeMillis() + durationMillis);
        return this;
    }

    public CombatManager refresh(Player player) {
        if (player != null && isInCombat(player)) {
            mark(player);
        }
        return this;
    }

    public boolean isInCombat(Player player) {
        return player != null && isInCombat(player.getUniqueId());
    }

    public boolean isInCombat(UUID uuid) {
        if (uuid == null) return false;

        long until = combatUntil.getOrDefault(uuid, 0L);
        if (until == 0L) return false;

        if (until > System.currentTimeMillis()) return true;

        combatUntil.remove(uuid);
        return false;
    }

    public long remainingMillis(Player player) {
        return remainingMillis(player == null ? null : player.getUniqueId());
    }

    public long remainingMillis(UUID uuid) {
        if (uuid == null) return 0L;

        long until = combatUntil.getOrDefault(uuid, 0L);
        long remaining = until - System.currentTimeMillis();

        if (remaining <= 0) {
            if (until != 0L) combatUntil.remove(uuid);
            return 0L;
        }

        return remaining;
    }

    public long expiresAt(UUID uuid) {
        return uuid == null ? 0L : combatUntil.getOrDefault(uuid, 0L);
    }

    public CombatManager end(Player player, CombatEndEvent.Reason reason) {
        if (player == null) return this;

        if (combatUntil.remove(player.getUniqueId()) != null) {
            Bukkit.getPluginManager().callEvent(new CombatEndEvent(player, reason));
        }
        return this;
    }

    public void endAll(CombatEndEvent.Reason reason) {
        for (UUID uuid : new ArrayList<>(combatUntil.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                end(player, reason);
            } else {
                combatUntil.remove(uuid);
            }
        }
    }

    public void clear() {
        combatUntil.clear();
    }

    private void checkExpiry() {
        long now = System.currentTimeMillis();

        for (UUID uuid : new ArrayList<>(combatUntil.keySet())) {
            if (combatUntil.get(uuid) > now) continue;

            Player player = Bukkit.getPlayer(uuid);
            combatUntil.remove(uuid);

            if (player != null) {
                Bukkit.getPluginManager().callEvent(new CombatEndEvent(player, CombatEndEvent.Reason.EXPIRED));
            }
        }
    }
}
