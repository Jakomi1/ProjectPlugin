package de.jakomi1.project.state;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Set;

public final class ScheduledTransition {

    private final ServerState state;
    private final LocalTime time;
    private final Set<DayOfWeek> days;
    private final LocalDate date;

    ScheduledTransition(ServerState state, LocalTime time, Set<DayOfWeek> days, LocalDate date) {
        this.state = state;
        this.time = time;
        this.days = days;
        this.date = date;
    }

    public ServerState state() {
        return state;
    }

    public LocalTime time() {
        return time;
    }

    public Set<DayOfWeek> days() {
        return days;
    }

    public LocalDate date() {
        return date;
    }

    /**
     * Gibt den Tag zurueck, fuer den der Uebergang geplant ist.
     * Bei einem festen Datum ist das das Datum selbst, sonst der aktuelle Tag.
     */
    public LocalDate fireDate(ZonedDateTime now) {
        return date != null ? date : now.toLocalDate();
    }

    /**
     * Ob der Uebergang zum gegebenen Zeitpunkt (Europe/Berlin) faellig ist.
     */
    public boolean isDue(ZonedDateTime now) {
        if (date != null && !now.toLocalDate().equals(date)) return false;
        if (!days.isEmpty() && !days.contains(now.getDayOfWeek())) return false;

        LocalTime nowTime = now.toLocalTime();
        return !nowTime.isBefore(time);
    }
}
