package de.jakomi1.project.state;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class StateSettings {

    private final Component motd;
    private final Component subMotd;
    private final boolean joinAllowed;
    private final Component kickMessage;
    private final boolean hidePlayers;
    private final BorderSettings border;
    private final StateRule movement;
    private final StateRule damage;
    private final StateRule blocks;

    private StateSettings(Builder builder) {
        this.motd = builder.motd;
        this.subMotd = builder.subMotd;
        this.joinAllowed = builder.joinAllowed;
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
                    .joinAllowed(false)
                    .hidePlayers(true)
                    .border(BorderSettings.of(100, 0, 0))
                    .subMotd(Component.text("Der Server ist derzeit gestoppt.", NamedTextColor.RED))
                    .kickMessage(Component.text("Der Server ist derzeit gestoppt.", NamedTextColor.RED))
                    .build();

            case STARTED -> builder()
                    .joinAllowed(true)
                    .hidePlayers(false)
                    .border(BorderSettings.of(500, 0, 0))
                    .subMotd(Component.text("Der Server startet gleich...", NamedTextColor.GREEN))
                    .build();

            case OPEN -> builder()
                    .joinAllowed(true)
                    .hidePlayers(false)
                    .border(BorderSettings.of(8000, 0, 0))
                    .build();

            case CLOSED -> builder()
                    .joinAllowed(false)
                    .hidePlayers(true)
                    .border(BorderSettings.of(1000, 0, 0))
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

    public boolean joinAllowed() {
        return joinAllowed;
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
        private boolean joinAllowed = true;
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
                    .joinAllowed(settings.joinAllowed)
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
            this.joinAllowed = joinAllowed;
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
