package de.jakomi1.project.state;

import de.jakomi1.database.table.GlobalSettingsTable;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class StateManager {

    /** Sekunden, die der Countdown vor dem Start laeuft. */
    public static final int START_COUNTDOWN_SECONDS = 10;

    private final ProjectServer server;
    private final GlobalSettingsTable settingsTable;
    private final Map<ServerState, StateSettings> settings = new EnumMap<>(ServerState.class);
    private final StateRestrictionListener restrictionListener;
    private final StateScheduler scheduler;

    private final AtomicBoolean starting = new AtomicBoolean(false);
    private volatile Scheduler.Task startCountdownTask;

    public StateManager(ProjectServer server, GlobalSettingsTable settingsTable) {
        this.server = server;
        this.settingsTable = settingsTable;
        this.restrictionListener = new StateRestrictionListener(this);
        restrictionListener.register(server.plugin());
        this.scheduler = new StateScheduler(server);

        for (ServerState state : ServerState.values()) {
            settings.put(state, StateSettings.defaults(state));
        }

        refresh();
        server.dialogs().check("state", context -> allowsJoin(context.uniqueId())
                ? de.jakomi1.project.connection.ConnectionResult.allow()
                : de.jakomi1.project.connection.ConnectionResult.disconnect(kickMessage()));
    }

    public StateManager settings(ServerState state, StateSettings stateSettings) {
        settings.put(state, stateSettings);
        if (state == currentState()) refresh();
        return this;
    }

    public StateSettings settings(ServerState state) {
        return settings.getOrDefault(state, StateSettings.defaults(state));
    }

    public ServerState currentState() {
        return settingsTable.getServerState();
    }

    public StateManager set(ServerState state) {
        // Ein laufender Countdown darf einen absichtlichen Zustandswechsel
        // nicht ueberrollen: wer den Server stoppt, will nicht Sekunden
        // spaeter doch noch den Start durchlaufen sehen.
        cancelStartCountdown();

        settingsTable.setServerState(state);
        refresh();
        return this;
    }

    public StateManager advance() {
        settingsTable.advanceServerState();
        refresh();
        return this;
    }

    /** Laeuft gerade ein Start-Countdown? */
    public boolean isStarting() {
        return starting.get();
    }

    /**
     * Startet den Countdown aus CrackedAttack: einmal pro Sekunde ein Countdown
     * im Titel, danach wechselt der Server auf {@link ServerState#STARTED} -
     * und erst damit greift die grosse Worldborder.
     *
     * <p>Die Border wird nicht weich eingeblendet, sondern am Ende des
     * Countdowns gesetzt, genau wie bei CrackedAttack. Wer waehrenddessen
     * {@code /csmp5 stop} oder {@code /csmp5 open} benutzt, bricht den
     * Countdown ab.
     *
     * @return true wenn der Countdown tatsaechlich gestartet wurde
     */
    public boolean startCountdown() {
        if (currentState() == ServerState.STARTED) return false;
        if (!starting.compareAndSet(false, true)) return false;

        AtomicInteger secondsLeft = new AtomicInteger(START_COUNTDOWN_SECONDS);
        AtomicReference<Scheduler.Task> ref = new AtomicReference<>();

        Scheduler.Task task = server.scheduler().runTimer(() -> {
            if (!starting.get()) {
                Scheduler.Task current = ref.get();
                if (current != null) current.cancel();
                return;
            }

            int seconds = secondsLeft.getAndDecrement();

            if (seconds > 0) {
                sendCountdownTick(seconds);
                return;
            }

            Scheduler.Task current = ref.get();
            if (current != null) current.cancel();

            startCountdownTask = null;
            starting.set(false);

            set(ServerState.STARTED);
            sendToOnlinePlayers(this::sendStartFinished);
        }, 1L, 20L);

        ref.set(task);
        startCountdownTask = task;
        return true;
    }

    /**
     * Bricht einen laufenden Countdown ab.
     *
     * @return true wenn ein Countdown aktiv war
     */
    public boolean cancelStartCountdown() {
        boolean wasStarting = starting.compareAndSet(true, false);

        Scheduler.Task task = startCountdownTask;
        startCountdownTask = null;

        if (task != null) {
            task.cancel();
            return true;
        }

        return wasStarting;
    }

    private void sendCountdownTick(int seconds) {
        NamedTextColor color;

        if (seconds >= 6) {
            color = NamedTextColor.GREEN;
        } else if (seconds >= 3) {
            color = NamedTextColor.YELLOW;
        } else {
            color = NamedTextColor.RED;
        }

        Component countdown = Component.text(String.valueOf(seconds), color);

        sendToOnlinePlayers(player -> {
            player.showTitle(Title.title(
                    countdown,
                    Component.empty(),
                    Title.Times.times(
                            Duration.ofMillis(250),
                            Duration.ofMillis(750),
                            Duration.ofMillis(250)
                    )
            ));
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        });
    }

    private void sendStartFinished(Player player) {
        Component subtitle = Component.text("Viel Spaß!", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);

        player.showTitle(Title.title(
                server.title(),
                subtitle,
                Title.Times.times(
                        Duration.ofMillis(250),
                        Duration.ofMillis(2000),
                        Duration.ofMillis(250)
                )
        ));
        player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    /**
     * Folia: Titel und Sounds laufen ueber den Scheduler des jeweiligen
     * Spielers, nie ueber dessen Region-Thread hinweg.
     */
    private void sendToOnlinePlayers(Consumer<Player> consumer) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            server.scheduler().runEntity(player, () -> consumer.accept(player));
        }
    }

    public StateManager border(ServerState state, BorderSettings border) {
        StateSettings current = settings(state);
        StateSettings updated = StateSettings.builder()
                .from(current)
                .border(border)
                .build();

        settings.put(state, updated);
        if (state == currentState()) refresh();
        return this;
    }

    public StateManager movement(ServerState state, StateRule rule) {
        return rules(state, rule, settings(state).damage(), settings(state).blocks());
    }

    public StateManager damage(ServerState state, StateRule rule) {
        return rules(state, settings(state).movement(), rule, settings(state).blocks());
    }

    public StateManager blocks(ServerState state, StateRule rule) {
        return rules(state, settings(state).movement(), settings(state).damage(), rule);
    }

    public StateManager rules(ServerState state, StateRule movement, StateRule damage, StateRule blocks) {
        StateSettings current = settings(state);
        StateSettings updated = StateSettings.builder()
                .from(current)
                .movement(movement)
                .damage(damage)
                .blocks(blocks)
                .build();

        settings.put(state, updated);
        if (state == currentState()) refresh();
        return this;
    }

    public boolean allowsMovement(Player player) {
        return player != null && settings(currentState()).movement().allows(server.permissions(), player.getUniqueId());
    }

    public boolean allowsDamage(Player player) {
        return player != null && settings(currentState()).damage().allows(server.permissions(), player.getUniqueId());
    }

    public boolean allowsBreaking(Player player) {
        return player != null && settings(currentState()).blocks().allows(server.permissions(), player.getUniqueId());
    }

    public StateManager schedule(StateSchedule schedule) {
        scheduler.schedule(schedule).start();
        return this;
    }

    public StateScheduler scheduler() {
        return scheduler;
    }

    public boolean allowsJoin() {
        return allowsJoin(null);
    }

    public boolean allowsJoin(UUID uuid) {
        return settings(currentState()).join().allows(server.permissions(), uuid);
    }

    public Component kickMessage() {
        return settings(currentState()).kickMessage();
    }

    public void refresh() {
        StateSettings stateSettings = settings(currentState());

        Component mainMotd = stateSettings.motd() != null
                ? stateSettings.motd()
                : server.title();

        server.serverPing().setMotd(mainMotd, stateSettings.subMotd());
        server.serverPing().hideOnlinePlayers(stateSettings.hidePlayers());
        applyBorder(stateSettings.border());
    }

    private void applyBorder(BorderSettings border) {
        if (border == null) return;
        for (World world : Bukkit.getWorlds()) {
            border.applyToWorld(world);
        }
    }
}
