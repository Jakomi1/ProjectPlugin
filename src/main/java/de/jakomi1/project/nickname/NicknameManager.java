package de.jakomi1.project.nickname;

import de.jakomi1.permission.Role;
import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

/**
 * Anzeigenamen (Nicknames).
 *
 * <p>Ein Nickname ersetzt den echten Namen in Tab-Liste, Chat und im
 * Ingame-Nametag. Die Aufloesung passiert zur Anzeigezeit, damit Aenderungen
 * sofort wirken. Der Manager ist standardmaessig <b>deaktiviert</b>
 * ({@link #defaultState()}) und muss bewusst eingeschaltet werden.
 *
 * <p>Der Nickname-Text wird wie die uebrigen Texte des Projekts als Markup
 * geparst, es sind also Farben und Gradients moeglich (z. B.
 * {@code <gradient:#00cfe4:#8000e4>Name</gradient>}).
 */
public final class NicknameManager implements Manager {

    private final ProjectServer server;
    private final NicknameTable table;
    private final NicknameCommand command;

    private boolean enabled;
    private boolean registerCommand = true;
    private boolean tableRegistered;
    private Role minimumRole = Role.OWNER;
    private String permission;

    public NicknameManager(ProjectServer server) {
        this.server = server;
        this.table = new NicknameTable();
        this.command = new NicknameCommand(this);
    }

    @Override
    public NicknameManager enable() {
        if (enabled) return this;
        enabled = true;

        nicknameTable();

        if (registerCommand) {
            command.register(server.plugin());
        }

        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean defaultState() {
        return false;
    }

    public NicknameManager command(boolean registerCommand) {
        this.registerCommand = registerCommand;
        return this;
    }

    public boolean command() {
        return registerCommand;
    }

    public NicknameManager minimumRole(Role role) {
        this.minimumRole = role != null ? role : Role.OWNER;
        this.permission = null;
        return this;
    }

    public Role minimumRole() {
        return minimumRole;
    }

    public NicknameManager permission(String permission) {
        this.permission = permission == null || permission.isBlank() ? null : permission;
        return this;
    }

    public String permission() {
        if (permission != null) return permission;

        return server.permissions().permissionPrefix()
                + "."
                + minimumRole.name().toLowerCase(Locale.ROOT);
    }

    /** Rollenbasierte Freigabe - Rolle {@link #minimumRole()} oder hoeher. */
    public boolean canUse(UUID uuid) {
        if (uuid == null) return false;

        return server.permissions().roleOf(uuid).inherits(minimumRole);
    }

    public NicknameManager set(UUID uuid, String playerName, String nickname) {
        nicknameTable().set(uuid, playerName, nickname);
        refresh(uuid);
        return this;
    }

    public NicknameManager clear(UUID uuid) {
        nicknameTable().clear(uuid);
        refresh(uuid);
        return this;
    }

    public boolean hasNickname(UUID uuid) {
        return nicknameTable().has(uuid);
    }

    public boolean hasNickname(String name) {
        return nicknameTable().has(name);
    }

    /** Roher Nickname (Markup-Text) oder null. */
    public String nickname(UUID uuid) {
        return nicknameTable().nickname(uuid);
    }

    public String nickname(String name) {
        return nicknameTable().nickname(name);
    }

    public Collection<NicknameTable.NicknameEntry> all() {
        return nicknameTable().values();
    }

    /**
     * Anzeigename des Spielers: Nickname, sonst der echte Name in Weiss.
     * Nie null.
     */
    public Component name(Player player) {
        if (player == null) return Component.empty();

        return name(player.getUniqueId(), player.getName());
    }

    public Component name(UUID uuid, String fallback) {
        String raw = uuid == null ? null : nickname(uuid);

        if (raw == null || raw.isBlank()) {
            return Component.text(fallback == null ? "" : fallback, NamedTextColor.WHITE);
        }

        return render(raw);
    }

    /**
     * Wie {@link #name(Player)}, aber {@code null}, wenn kein Nickname gesetzt
     * ist. Fuer das Nametag, das ohne Nickname den Vanilla-Namen behalten soll.
     */
    public Component customName(Player player) {
        if (player == null) return null;

        String raw = nickname(player.getUniqueId());
        if (raw == null || raw.isBlank()) return null;

        return render(raw);
    }

    private Component render(String raw) {
        Component rendered = ComponentUtils.createMarkupLine(raw, NamedTextColor.WHITE);
        return rendered == null ? Component.text(raw, NamedTextColor.WHITE) : rendered;
    }

    /** Tab und Nametag sofort aktualisieren, falls der Zielspieler online ist. */
    private void refresh(UUID uuid) {
        Player player = uuid == null ? null : Bukkit.getPlayer(uuid);
        if (player == null) return;

        if (server.tab().isEnabled()) {
            server.tab().updateAll();
        }

        if (server.nametags().isEnabled()) {
            server.nametags().requestSync();
        }
    }

    public NicknameTable table() {
        return nicknameTable();
    }

    public ProjectServer server() {
        return server;
    }

    private NicknameTable nicknameTable() {
        if (!tableRegistered) {
            table.register(server.plugin());
            tableRegistered = true;
        }
        return table;
    }
}
