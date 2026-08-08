package de.jakomi1.project.state;

import de.jakomi1.project.ProjectServer;
import de.jakomi1.project.scheduler.Scheduler;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.Map;

public final class StateScheduler {

    public static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private final ProjectServer server;
    private final Map<ServerState, LocalDate> lastFired = new EnumMap<>(ServerState.class);

    private StateSchedule schedule = StateSchedule.builder().build();
    private Scheduler.Task task;

    public StateScheduler(ProjectServer server) {
        this.server = server;
    }

    public StateScheduler schedule(StateSchedule schedule) {
        this.schedule = schedule == null ? StateSchedule.builder().build() : schedule;
        return this;
    }

    /**
     * Startet den Repeating-Task. Pro Sekunde wird geprueft, ob ein geplanter
     * Uebergang (Uhrzeit/Datum, Europe/Berlin) erreicht wurde.
     */
    public StateScheduler start() {
        if (task != null && !task.isCancelled()) return this;
        if (schedule.isEmpty()) return this;

        ZonedDateTime now = ZonedDateTime.now(BERLIN);
        lastFired.clear();
        for (ScheduledTransition transition : schedule.transitions().values()) {
            lastFired.put(transition.state(), now.toLocalDate().minusDays(1));
        }

        task = server.scheduler().runTimer(this::check, 20, 20);
        return this;
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public boolean isRunning() {
        return task != null && !task.isCancelled();
    }

    private void check() {
        ZonedDateTime now = ZonedDateTime.now(BERLIN);

        for (ScheduledTransition transition : schedule.transitions().values()) {
            if (!transition.isDue(now)) continue;

            LocalDate fireDate = transition.fireDate(now);
            LocalDate fired = lastFired.get(transition.state());
            if (fireDate.equals(fired)) continue;

            lastFired.put(transition.state(), fireDate);
            server.stateManager().set(transition.state());
        }
    }
}
