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

    private StateSettings(Builder builder) {
        this.motd = builder.motd;
        this.subMotd = builder.subMotd;
        this.joinAllowed = builder.joinAllowed;
        this.kickMessage = builder.kickMessage;
        this.hidePlayers = builder.hidePlayers;
        this.border = builder.border;
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

    public static final class Builder {

        private Component motd;
        private Component subMotd = Component.empty();
        private boolean joinAllowed = true;
        private Component kickMessage = Component.empty();
        private boolean hidePlayers;
        private BorderSettings border;

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

        public StateSettings build() {
            return new StateSettings(this);
        }
    }
}
