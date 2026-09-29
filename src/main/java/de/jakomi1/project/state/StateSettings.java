package de.jakomi1.project.state;

import de.jakomi1.permission.Role;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class StateSettings {

    /**
     * Border-Durchmesser, solange der Server nicht laeuft. Der Wert ist
     * bewusst winzig: wer im Zustand STOPPED oder OPEN auf die Welt kommt, ist
     * auf den Spawn eingesperrt, bis der Start durch ist.
     */
    public static final double LOCKED_BORDER_SIZE = 20.0D;

    /**
     * Border-Durchmesser im laufenden Server. Wird erst gesetzt, wenn der
     * Start-Countdown durchgelaufen ist.
     */
    public static final double RUNNING_BORDER_SIZE = 10000.0D;

    private final Component motd;
    private final Component subMotd;
    private final StateRule join;
    private final Component kickMessage;
    private final boolean hidePlayers;
    private final BorderSettings border;
    private final StateRule movement;
    private final StateRule damage;
    private final StateRule blocks;

    private StateSettings(Builder builder) {
        this.motd = builder.motd;
        this.subMotd = builder.subMotd;
        this.join = builder.join;
        this.kickMessage = builder.kickMessage;
        this.hidePlayers = builder.hidePlayers;
        this.border = builder.border;
        this.movement = builder.movement;
        this.damage = builder.damage;
        this.blocks = builder.blocks;
    }

    public static StateSettings defaults(ServerState state) {
        return switch (state) {
            case STOPPED -> builder()
                    .join(StateRule.roles(Role.ADMIN, Role.OWNER))
                    .hidePlayers(true)
                    .border(BorderSettings.of(LOCKED_BORDER_SIZE))
                    .subMotd(Component.text("Der Server ist derzeit gestoppt.", NamedTextColor.RED))
                    .kickMessage(Component.text("Der Server ist derzeit gestoppt.", NamedTextColor.RED))
                    .build();

            case OPEN -> builder()
                    .join(StateRule.all())
                    .hidePlayers(false)
                    .border(BorderSettings.of(LOCKED_BORDER_SIZE))
                    .subMotd(Component.text("Der Server startet bald...", NamedTextColor.YELLOW))
                    .build();

            case STARTED -> builder()
                    .join(StateRule.all())
                    .hidePlayers(false)
                    .border(BorderSettings.of(RUNNING_BORDER_SIZE))
                    .build();

            case CLOSED -> builder()
                    .join(StateRule.roles(Role.ADMIN, Role.OWNER))
                    .hidePlayers(true)
                    .border(BorderSettings.of(LOCKED_BORDER_SIZE))
                    .subMotd(Component.text("Der Server ist geschlossen.", NamedTextColor.RED))
                    .kickMessage(Component.text("Der Server ist geschlossen.", NamedTextColor.RED))
                    .build();
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public Component motd() {
        return motd;
    }

    public Component subMotd() {
        return subMotd;
    }

    public StateRule join() {
        return join;
    }

    public boolean joinAllowed() {
        return join.type() == StateRule.Type.ALL;
    }

    public Component kickMessage() {
        return kickMessage;
    }

    public boolean hidePlayers() {
        return hidePlayers;
    }

    public BorderSettings border() {
        return border;
    }

    public StateRule movement() {
        return movement;
    }

    public StateRule damage() {
        return damage;
    }

    public StateRule blocks() {
        return blocks;
    }

    public static final class Builder {

        private Component motd;
        private Component subMotd = Component.empty();
        private StateRule join = StateRule.all();
        private Component kickMessage = Component.empty();
        private boolean hidePlayers;
        private BorderSettings border;
        private StateRule movement = StateRule.all();
        private StateRule damage = StateRule.all();
        private StateRule blocks = StateRule.all();

        public Builder from(StateSettings settings) {
            if (settings == null) return this;
            return motd(settings.motd)
                    .subMotd(settings.subMotd)
                    .join(settings.join)
                    .kickMessage(settings.kickMessage)
                    .hidePlayers(settings.hidePlayers)
                    .border(settings.border)
                    .movement(settings.movement)
                    .damage(settings.damage)
                    .blocks(settings.blocks);
        }

        public Builder motd(Component motd) {
            this.motd = motd;
            return this;
        }

        public Builder subMotd(Component subMotd) {
            this.subMotd = subMotd == null ? Component.empty() : subMotd;
            return this;
        }

        public Builder joinAllowed(boolean joinAllowed) {
            this.join = joinAllowed ? StateRule.all() : StateRule.none();
            return this;
        }

        public Builder join(StateRule rule) {
            this.join = rule == null ? StateRule.all() : rule;
            return this;
        }

        public Builder kickMessage(Component kickMessage) {
            this.kickMessage = kickMessage == null ? Component.empty() : kickMessage;
            return this;
        }

        public Builder hidePlayers(boolean hidePlayers) {
            this.hidePlayers = hidePlayers;
            return this;
        }

        public Builder border(BorderSettings border) {
            this.border = border;
            return this;
        }

        public Builder movement(StateRule rule) {
            this.movement = rule == null ? StateRule.all() : rule;
            return this;
        }

        public Builder damage(StateRule rule) {
            this.damage = rule == null ? StateRule.all() : rule;
            return this;
        }

        public Builder blocks(StateRule rule) {
            this.blocks = rule == null ? StateRule.all() : rule;
            return this;
        }

        public StateSettings build() {
            return new StateSettings(this);
        }
    }
}
