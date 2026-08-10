package de.jakomi1.project.whitelist;

import de.jakomi1.project.AutoManager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.project.connection.ConnectionContext;
import de.jakomi1.project.connection.ConnectionResult;
import de.jakomi1.project.connection.ServerConnectionDialog;
import de.jakomi1.permission.Role;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public final class WhitelistManager implements AutoManager {

    private static final String CHECK_ID = "whitelist";

    private final ProjectServer server;
    private final WhitelistTable table;
    private final WhitelistCommand command;

    private boolean enabled;
    private boolean auto = true;
    private boolean registerCommand = true;
    private boolean tableRegistered;
    private Role minimumRole = Role.SUPPORTER;
    private String permission;
    private Role bypassRole = Role.OWNER;
    private Component kickMessage;
    private boolean dialog = true;
    private BooleanSupplier closed = () -> false;
    private List<Component> whitelistMessages = List.of();
    private List<Component> closedWhitelistMessages = List.of();

    public WhitelistManager(ProjectServer server) {
        this.server = server;
        this.table = new WhitelistTable();
        this.command = new WhitelistCommand(this);
    }

    @Override
    public WhitelistManager enable() {
        if (enabled) return this;
        enabled = true;

        whitelistTable();
        server.dialogs().check(CHECK_ID, this::checkConnection);

        if (registerCommand) {
            command.register(server.plugin());
        }
        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;

        server.dialogs().removeCheck(CHECK_ID);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean auto() {
        return auto;
    }

    @Override
    public WhitelistManager auto(boolean auto) {
        this.auto = auto;
        return this;
    }

    public WhitelistManager command(boolean registerCommand) {
        this.registerCommand = registerCommand;
        return this;
    }

    public boolean command() {
        return registerCommand;
    }

    public WhitelistManager minimumRole(Role role) {
        this.minimumRole = role != null ? role : Role.SUPPORTER;
        this.permission = null;
        return this;
    }

    public Role minimumRole() {
        return minimumRole;
    }

    public WhitelistManager permission(String permission) {
        this.permission = permission == null || permission.isBlank() ? null : permission;
        return this;
    }

    public String permission() {
        if (permission != null) return permission;

        return server.permissions().permissionPrefix()
                + "."
                + minimumRole.name().toLowerCase(Locale.ROOT);
    }

    public WhitelistManager bypassRole(Role role) {
        this.bypassRole = role;
        return this;
    }

    public Role bypassRole() {
        return bypassRole;
    }

    public boolean bypass(UUID uuid) {
        if (bypassRole == null || uuid == null) return false;

        return server.permissions().roleOf(uuid).inherits(bypassRole);
    }

    public WhitelistManager kickMessage(Component message) {
        this.kickMessage = message;
        return this;
    }

    public Component kickMessage() {
        if (kickMessage != null) return kickMessage;

        Component prefix = server.plugin().getPrefix();
        Component base = Component.text("Du bist nicht gewhitelistet.", NamedTextColor.RED);

        return prefix != null ? prefix.append(base) : base;
    }

    public WhitelistManager dialog(boolean dialog) {
        this.dialog = dialog;
        return this;
    }

    public boolean dialog() {
        return dialog;
    }

    public WhitelistManager closedWhen(BooleanSupplier supplier) {
        this.closed = supplier == null ? () -> false : supplier;
        return this;
    }

    public WhitelistManager whitelistMessages(List<Component> messages) {
        this.whitelistMessages = messages == null ? List.of() : List.copyOf(messages);
        return this;
    }

    public WhitelistManager closedWhitelistMessages(List<Component> messages) {
        this.closedWhitelistMessages = messages == null ? List.of() : List.copyOf(messages);
        return this;
    }

    public WhitelistTable table() {
        return whitelistTable();
    }

    private WhitelistTable whitelistTable() {
        if (!tableRegistered) {
            table.register(server.plugin());
            tableRegistered = true;
        }
        return table;
    }

    public ProjectServer server() {
        return server;
    }

    public boolean isWhitelisted(UUID uuid) {
        return whitelistTable().isWhitelisted(uuid);
    }

    public boolean isWhitelisted(String name) {
        return whitelistTable().isWhitelisted(name);
    }

    public boolean isWhitelisted(UUID uuid, String name) {
        return whitelistTable().isWhitelisted(uuid, name);
    }

    public WhitelistManager add(String name) {
        whitelistTable().add(name);
        return this;
    }

    public WhitelistManager add(UUID uuid) {
        whitelistTable().add(uuid);
        return this;
    }

    public WhitelistManager add(UUID uuid, String name) {
        whitelistTable().add(uuid, name);
        return this;
    }

    public WhitelistManager remove(String name) {
        whitelistTable().removeEntry(name);
        return this;
    }

    public WhitelistManager remove(UUID uuid) {
        whitelistTable().removeEntry(uuid);
        return this;
    }

    public Collection<WhitelistTable.WhitelistEntry> all() {
        return whitelistTable().all();
    }

    public int count() {
        return whitelistTable().count();
    }

    private ConnectionResult checkConnection(ConnectionContext context) {
        UUID uniqueId = context.uniqueId();
        String name = context.name();

        if (bypass(uniqueId)) return ConnectionResult.allow();

        if (whitelistTable().isWhitelisted(uniqueId, name)) {
            whitelistTable().ensure(uniqueId, name);
            return ConnectionResult.allow();
        }

        if (!dialog) {
            return ConnectionResult.disconnect(kickMessage());
        }

        boolean isClosed = closed.getAsBoolean();
        return ConnectionResult.dialog(createDialog(isClosed));
    }

    private ServerConnectionDialog createDialog(boolean closed) {
        Key actionKey = server.dialogs().key("whitelist/understood");
        Component disconnect = closed
                ? Component.text("Bis bald!", NamedTextColor.RED)
                : kickMessage();

        Dialog dialog = Dialog.create(server.dialogs().noticeDialog(
                server.title(),
                closed ? closedWhitelistBody() : whitelistBody(),
                actionKey
        ));

        return new ServerConnectionDialog(
                dialog,
                Map.of(actionKey, false),
                disconnect,
                Duration.ofMinutes(1)
        );
    }

    private List<Component> whitelistBody() {
        if (!whitelistMessages.isEmpty()) {
            return whitelistMessages;
        }

        List<Component> body = new ArrayList<>();
        body.add(Component.text("Du bist aktuell nicht gewhitelistet!", NamedTextColor.RED));

        if (server.links().hasDiscord()) {
            body.add(Component.text("Whitelist-Anfragen nur per Discord:", NamedTextColor.GRAY)
                    .appendNewline()
                    .append(discordLink()));
        }

        return body;
    }

    private List<Component> closedWhitelistBody() {
        if (!closedWhitelistMessages.isEmpty()) {
            return closedWhitelistMessages;
        }

        List<Component> body = new ArrayList<>();
        body.add(Component.text("Du hast leider die Anmelde Frist verpasst!", NamedTextColor.RED));

        if (server.links().hasDiscord()) {
            body.add(Component.text("Schau auf unserem Discord für zukünftige Projekte vorbei:", NamedTextColor.GRAY)
                    .appendNewline()
                    .append(discordLink()));
        }

        return body;
    }

    private Component discordLink() {
        String url = "https://discord.gg/" + server.links().discordToken();
        return Component.text(url, NamedTextColor.BLUE)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.clickEvent(
                        ClickEvent.Action.OPEN_URL,
                        ClickEvent.Payload.string(url)
                ));
    }
}
