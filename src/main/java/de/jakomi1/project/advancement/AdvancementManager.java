package de.jakomi1.project.advancement;

import de.jakomi1.permission.Role;
import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Verhindert das Freischalten von Errungenschaften, wahlweise in
 * bestimmten Gamemodes oder abhaengig von der Rolle des Spielers.
 *
 * <p>Es gibt zwei unabhaengige Regeln, die beide das
 * {@code PlayerAdvancementCriterionGrantEvent} abbrechen:
 *
 * <ul>
 *   <li><b>Gamemodes:</b> In {@link #blockedGameModes() konfigurierten}
 *       Spielmodi wird kein Fortschritt gesammelt. Vorgabe sind Creative und
 *       Spectator: Ein Creative-Spieler baut am Spawn und soll dabei nicht
 *       versehentlich Titel sammeln, im Spectator kann niemand etwas
 *       erreichen.</li>
 *   <li><b>Rollen:</b> Entweder werden konkrete Rollen ueber
 *       {@link #blockedRoles(Role...)} gesperrt (und nur die laufen in
 *       einen Fortschritts-Stau), oder es wird eine
 *       {@link #minimumRole(Role) Mindest-Rolle} gesetzt, ab der
 *       Errungenschaften ueberhaupt erst verdient werden koennen. Fuer einen
 *       SMP ist das zweite Muster typisch: erst ab einer bestimmten Rolle
 *       gibt es Erfolge.</li>
 * </ul>
 *
 * <p>Das {@code PlayerAdvancementCriterionGrantEvent} ist der einzige Punkt,
 * an dem sich der Advancement-Fortschritt wirklich verhindern laesst: Es
 * feuert vor jedem einzelnen Kriterium. Das spaete
 * {@code PlayerAdvancementDoneEvent} ist dagegen nicht abbestellbar und nur
 * ein nachtraeglicher Hinweis.
 *
 * <p>Der Manager ist opt-in ({@link #defaultState()} ist {@code false}):
 * Erst ein {@link #enable()} registriert den Listener.
 */
public final class AdvancementManager implements Manager {

    private static final Set<GameMode> DEFAULT_BLOCKED_GAME_MODES =
            EnumSet.of(GameMode.CREATIVE, GameMode.SPECTATOR);

    private final ProjectServer server;
    private final Set<GameMode> blockedGameModes = EnumSet.noneOf(GameMode.class);
    private final Set<String> blockedRoles = new LinkedHashSet<>();

    private Role minimumRole;
    private AdvancementListener listener;
    private boolean enabled;

    public AdvancementManager(ProjectServer server) {
        this.server = server;
        this.blockedGameModes.addAll(DEFAULT_BLOCKED_GAME_MODES);
    }

    @Override
    public AdvancementManager enable() {
        if (enabled) {
            return this;
        }

        enabled = true;

        listener = new AdvancementListener(this);
        listener.register(server.plugin());

        return this;
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }

        enabled = false;

        if (listener != null) {
            listener.unregister();
            listener = null;
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

    /**
     * Ersetzt die Gamemodes, in denen Errungenschaften verhindert werden.
     * Eine leere Liste hebt die gamemode-abhaengige Sperre komplett auf,
     * die Rollen-Regeln bleiben davon unberuehrt.
     */
    public AdvancementManager blockedGameModes(GameMode... gameModes) {
        blockedGameModes.clear();

        if (gameModes != null) {
            for (GameMode gameMode : gameModes) {
                if (gameMode != null) {
                    blockedGameModes.add(gameMode);
                }
            }
        }

        return this;
    }

    /** Unmodifizierbare Sicht auf die gesperrten Gamemodes. */
    public Set<GameMode> blockedGameModes() {
        return Set.copyOf(blockedGameModes);
    }

    /**
     * Sperrt konkret genannte Rollen: Ein Spieler mit einer dieser Rollen
     * erhaelt keine Errungenschaften, unabhaengig von der Mindest-Rolle.
     * Eine leere Liste hebt die Rollen-Sperre auf.
     */
    public AdvancementManager blockedRoles(Role... roles) {
        blockedRoles.clear();

        if (roles != null) {
            for (Role role : roles) {
                if (role != null) {
                    blockedRoles.add(role.name());
                }
            }
        }

        return this;
    }

    /**
     * Setzt eine Mindest-Rolle. Ein Spieler, dessen Rolle diese
     * Mindest-Rolle nicht erbt, erhaelt generell keine Errungenschaften -
     * erst ab dieser Rolle gibt es Fortschritt. {@code null} hebt die
     * Schwelle auf.
     */
    public AdvancementManager minimumRole(Role minimumRole) {
        this.minimumRole = minimumRole;
        return this;
    }

    public Role minimumRole() {
        return minimumRole;
    }

    /** Unmodifizierbare Sicht auf die gesperrten Rollen (nach Namen). */
    public Set<String> blockedRoles() {
        return Set.copyOf(blockedRoles);
    }

    /** @return true, wenn der Spieler derzeit keine Errungenschaften erhaelt. */
    public boolean shouldBlock(Player player) {
        if (player == null) {
            return false;
        }

        if (blockedGameModes.contains(player.getGameMode())) {
            return true;
        }

        Role role = server.permissions().roleOf(player.getUniqueId());

        if (blockedRoles.contains(role.name())) {
            return true;
        }

        if (minimumRole != null && !role.inherits(minimumRole)) {
            return true;
        }

        return false;
    }
}