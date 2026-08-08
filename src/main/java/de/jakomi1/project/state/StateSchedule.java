package de.jakomi1.project.state;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StateSchedule {

    private final Map<ServerState, ScheduledTransition> transitions;

    private StateSchedule(Map<ServerState, ScheduledTransition> transitions) {
        this.transitions = transitions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<ServerState, ScheduledTransition> transitions() {
        return Map.copyOf(transitions);
    }

    public ScheduledTransition transition(ServerState state) {
        return transitions.get(state);
    }

    public boolean isEmpty() {
        return transitions.isEmpty();
    }

    public static final class Builder {

        private final Map<ServerState, ScheduledTransition> transitions = new EnumMap<>(ServerState.class);

        public Builder at(ServerState state, LocalTime time) {
            transitions.put(state, new ScheduledTransition(state, time, Set.of(), null));
            return this;
        }

        public Builder at(ServerState state, LocalTime time, DayOfWeek... days) {
            transitions.put(state, new ScheduledTransition(state, time, Set.copyOf(List.of(days)), null));
            return this;
        }

        public Builder at(ServerState state, LocalTime time, LocalDate date) {
            transitions.put(state, new ScheduledTransition(state, time, Set.of(), date));
            return this;
        }

        public StateSchedule build() {
            return new StateSchedule(transitions);
        }
    }
}
