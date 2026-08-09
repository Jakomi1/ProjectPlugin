package de.jakomi1.scheduler;

import de.jakomi1.project.ProjectPlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

public final class Scheduler {

    private static final boolean isFolia;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
        }
        isFolia = folia;
    }

    private final ProjectPlugin plugin;

    public Scheduler(ProjectPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin != null && plugin.isEnabled();
    }

    private boolean scheduleable(Location location) {
        if (!enabled()) {
            Bukkit.getLogger().warning("Plugin ist deaktiviert, Task wurde nicht ausgeführt.");
            return false;
        }

        if (location == null || location.getWorld() == null) {
            Bukkit.getLogger().warning("Location/World ist null, Task wurde nicht ausgeführt.");
            return false;
        }

        return true;
    }

    private static long safeDelay(long ticks) {
        return Math.max(ticks, 1L);
    }

    private static long safePeriod(long ticks) {
        return Math.max(ticks, 1L);
    }

    public void runAsync(Runnable runnable) {
        if (!enabled()) return;

        Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
    }

    public void runGlobal(Runnable runnable) {
        if (!enabled()) return;

        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public void runRegion(Location location, Runnable runnable) {
        if (!scheduleable(location)) return;

        if (isFolia) {
            Bukkit.getRegionScheduler().execute(plugin, location, runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public void runEntity(Entity entity, Runnable runnable) {
        if (!enabled() || entity == null || !entity.isValid()) return;

        try {
            entity.getScheduler().run(plugin, task -> runnable.run(), () -> {
            });
        } catch (Throwable ignored) {
        }
    }

    public void runEntityLater(Entity entity, Runnable runnable, long delayTicks) {
        if (!enabled() || entity == null || !entity.isValid()) return;

        long delay = safeDelay(delayTicks);

        try {
            entity.getScheduler().runDelayed(plugin, task -> runnable.run(), () -> {
            }, delay);
        } catch (Throwable ignored) {
        }
    }

    public Task runLater(Runnable runnable, long delayTicks) {
        if (!enabled()) return Task.noop();

        long delay = safeDelay(delayTicks);

        if (isFolia) {
            ScheduledTask foliaTask = Bukkit.getGlobalRegionScheduler()
                    .runDelayed(plugin, task -> runnable.run(), delay);
            return new Task(foliaTask);
        } else {
            BukkitTask bukkitTask = Bukkit.getScheduler()
                    .runTaskLater(plugin, runnable, delay);
            return new Task(bukkitTask);
        }
    }

    public Task runTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (!enabled()) return Task.noop();

        long delay = safeDelay(delayTicks);
        long period = safePeriod(periodTicks);

        if (isFolia) {
            ScheduledTask foliaTask = Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(plugin, task -> runnable.run(), delay, period);
            return new Task(foliaTask);
        } else {
            BukkitTask bukkitTask = Bukkit.getScheduler()
                    .runTaskTimer(plugin, runnable, delay, period);
            return new Task(bukkitTask);
        }
    }

    public static boolean isFolia() {
        return isFolia;
    }

    public static final class Task {
        private final ScheduledTask foliaTask;
        private final BukkitTask bukkitTask;
        private final boolean noOp;

        private Task(boolean noOp) {
            this.foliaTask = null;
            this.bukkitTask = null;
            this.noOp = noOp;
        }

        Task(ScheduledTask foliaTask) {
            this.foliaTask = foliaTask;
            this.bukkitTask = null;
            this.noOp = false;
        }

        Task(BukkitTask bukkitTask) {
            this.bukkitTask = bukkitTask;
            this.foliaTask = null;
            this.noOp = false;
        }

        public static Task noop() {
            return new Task(true);
        }

        public void cancel() {
            if (noOp) return;

            if (foliaTask != null) {
                foliaTask.cancel();
            } else if (bukkitTask != null) {
                bukkitTask.cancel();
            }
        }

        public boolean isCancelled() {
            if (noOp) return true;

            if (foliaTask != null) {
                return foliaTask.isCancelled();
            }

            if (bukkitTask != null) {
                return bukkitTask.isCancelled();
            }

            return true;
        }

        public Object getHandle() {
            if (noOp) return null;
            return foliaTask != null ? foliaTask : bukkitTask;
        }
    }
}
