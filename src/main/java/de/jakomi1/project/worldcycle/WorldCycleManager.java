package de.jakomi1.project.worldcycle;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.project.state.ServerState;
import de.jakomi1.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.GameRules;
import org.bukkit.World;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Haelt die Weltzeit fest und schaltet Tag-Nacht- und Wetterzyklus ab,
 * solange der Server nicht laeuft.
 *
 * <p>Ein gestoppter Server soll nicht langsam in die Nacht wandern. Die
 * konfigurierte {@link #lockedTime() Zeit} wird deshalb jeden Tick wieder
 * gesetzt - ein {@code /time set} oder eine Command-Block-Kette koennen die
 * Uhr so nicht verstellen. Zusaetzlich werden Regen und Gewitter entfernt,
 * weil das Wetter ohne Zyklus sonst einfach stehen bleiben wuerde.
 *
 * <p>Freigegeben wird erst mit {@link ServerState#STARTED}, also nach dem
 * Start-Countdown des Projekts. Der Zustand wird dabei nicht vom Manager
 * gesetzt, sondern nur abgefragt: So gilt die Sperre auch fuer die
 * OPEN-Phase und fuer Zustandswechsel aus der State-Schedule der Library.
 *
 * <p>Die vorherigen Game-Rules werden beim Sperren gesichert und beim
 * Freigeben wiederhergestellt, damit eine bewusst abgeschaltete
 * Wetter-Animation nicht ungefragt wieder aktiviert wird.
 *
 * <p>Der Manager ist opt-in ({@link #defaultState()} ist {@code false}):
 * Erst ein {@link #enable()} startet den Tick-Timer.
 */
public final class WorldCycleManager implements Manager {

    /** Vorgabe fuer die feste Uhrzeit im geschlossenen Zustand: 06:00 Uhr. */
    public static final long DEFAULT_LOCKED_TIME = 6000L;

    private final ProjectServer server;
    private final Map<World, Boolean> previousAdvanceTime = new WeakHashMap<>();
    private final Map<World, Boolean> previousAdvanceWeather = new WeakHashMap<>();

    private long lockedTime = DEFAULT_LOCKED_TIME;
    private boolean enabled;
    private Scheduler.Task task;

    public WorldCycleManager(ProjectServer server) {
        this.server = server;
    }

    @Override
    public WorldCycleManager enable() {
        if (enabled) {
            return this;
        }

        enabled = true;

        task = server.scheduler().runTimer(
                () -> sync(server.stateManager().currentState()),
                1L,
                1L
        );

        return this;
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }

        enabled = false;

        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean defaultState() {
        return false;
    }

    public ProjectServer server() {
        return server;
    }

    /** Setzt die Uhrzeit, auf die die Welt im geschlossenen Zustand fixiert wird. */
    public WorldCycleManager lockedTime(long lockedTime) {
        this.lockedTime = lockedTime;
        return this;
    }

    public long lockedTime() {
        return lockedTime;
    }

    /**
     * Haelt die Sperre dem aktuellen Serverzustand entsprechend aktuell. Muss
     * regelmaessig aufgerufen werden, sonst greift sie erst beim naechsten
     * Zustandswechsel.
     */
    public void sync(ServerState state) {
        if (state == ServerState.STARTED) {
            release();
            return;
        }

        lock();
    }

    /** Setzt die Uhrzeit und schaltet beide Zyklen ab. */
    public void lock() {
        runOnGlobal(this::lockNow);
    }

    /** Stellt die zuvor gesicherten Game-Rules wieder her. */
    public void release() {
        if (previousAdvanceTime.isEmpty() && previousAdvanceWeather.isEmpty()) {
            return;
        }

        runOnGlobal(this::releaseNow);
    }

    /**
     * Weltzeit und Game-Rules duerfen nur vom Global-Tick geaendert werden.
     * Der Timer kommt ohnehin dorthin, ein Befehl wie {@code /csmp5 stop}
     * laeuft dagegen im Region-Thread des Spielers - dorthin wird der ganze
     * Block dann verschickt.
     */
    private void runOnGlobal(Runnable task) {
        if (Bukkit.isGlobalTickThread()) {
            task.run();
            return;
        }

        server.scheduler().runGlobal(task);
    }

    private void lockNow() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }

            remember(world);

            if (world.getTime() != lockedTime) {
                world.setTime(lockedTime);
            }

            setRule(world, GameRules.ADVANCE_TIME, false);
            setRule(world, GameRules.ADVANCE_WEATHER, false);

            if (world.hasStorm()) {
                world.setStorm(false);
            }

            if (world.isThundering()) {
                world.setThundering(false);
            }
        }
    }

    private void releaseNow() {
        for (World world : Bukkit.getWorlds()) {
            Boolean advanceTime = previousAdvanceTime.remove(world);
            Boolean advanceWeather = previousAdvanceWeather.remove(world);

            if (advanceTime != null) {
                setRule(world, GameRules.ADVANCE_TIME, advanceTime);
            }

            if (advanceWeather != null) {
                setRule(world, GameRules.ADVANCE_WEATHER, advanceWeather);
            }
        }
    }

    private void remember(World world) {
        if (!previousAdvanceTime.containsKey(world)) {
            previousAdvanceTime.put(world, world.getGameRuleValue(GameRules.ADVANCE_TIME));
        }

        if (!previousAdvanceWeather.containsKey(world)) {
            previousAdvanceWeather.put(world, world.getGameRuleValue(GameRules.ADVANCE_WEATHER));
        }
    }

    private void setRule(World world, GameRule<Boolean> rule, boolean value) {
        if (world.getGameRuleValue(rule) == null) {
            return;
        }

        world.setGameRule(rule, value);
    }
}
