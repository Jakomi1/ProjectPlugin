package de.jakomi1.project.connection;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryBuilderFactory;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SuppressWarnings("UnstableApiUsage")
public final class ServerDialogManager implements Manager {

    private final ProjectServer server;
    private final ServerDialogListener listener;
    private final Map<String, ConnectionCheck> checks = new LinkedHashMap<>();
    private final Map<UUID, PendingDialog> pending = new ConcurrentHashMap<>();
    private final Set<UUID> acceptedTerms = ConcurrentHashMap.newKeySet();

    private boolean enabled;
    private boolean termsEnabled;
    private Predicate<ConnectionContext> termsRequired = context -> true;
    private Consumer<ConnectionContext> termsAccepted = context -> {
    };
    private List<Component> termsMessages = List.of();

    public ServerDialogManager(ProjectServer server) {
        this.server = server;
        this.listener = new ServerDialogListener(this);
    }

    @Override
    public ServerDialogManager enable() {
        if (enabled) return this;
        enabled = true;
        listener.register(server.plugin());
        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;
        listener.unregister();
        pending.clear();
        acceptedTerms.clear();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public ServerDialogManager check(String id, ConnectionCheck check) {
        if (id == null || id.isBlank() || check == null) return this;

        synchronized (checks) {
            checks.put(id, check);
        }
        return this;
    }

    public ServerDialogManager removeCheck(String id) {
        if (id == null) return this;

        synchronized (checks) {
            checks.remove(id);
        }
        return this;
    }

    public ServerDialogManager terms(boolean enabled) {
        this.termsEnabled = enabled;
        return this;
    }

    public ServerDialogManager termsRequired(Predicate<ConnectionContext> predicate) {
        this.termsRequired = predicate == null ? context -> true : predicate;
        return this;
    }

    public ServerDialogManager termsAccepted(Consumer<ConnectionContext> consumer) {
        this.termsAccepted = consumer == null ? context -> {
        } : consumer;
        return this;
    }

    public ServerDialogManager termsMessages(List<Component> messages) {
        this.termsMessages = messages == null ? List.of() : List.copyOf(messages);
        return this;
    }

    public ServerDialogManager termsMessage(Component message) {
        if (message == null) return this;

        List<Component> updated = new ArrayList<>(termsMessages);
        updated.add(message);
        this.termsMessages = List.copyOf(updated);
        return this;
    }

    void handleConfigure(PlayerConfigurationConnection connection) {
        UUID uniqueId = connection.getProfile().getId();
        if (uniqueId == null) return;

        ConnectionContext context = new ConnectionContext(connection);

        for (ConnectionCheck check : checksSnapshot()) {
            ConnectionResult result = check.check(context);
            if (!handleResult(context, result)) return;
        }

        if (termsEnabled && !acceptedTerms.contains(uniqueId) && termsRequired.test(context)) {
            boolean accepted = showAndAwait(context, createTermsDialog());
            if (!accepted) return;

            acceptedTerms.add(uniqueId);
            termsAccepted.accept(context);
        }
    }

    public boolean showAndAwait(ConnectionContext context, ServerConnectionDialog dialog) {
        if (context == null || dialog == null || dialog.dialog() == null) return true;

        UUID uniqueId = context.uniqueId();
        if (uniqueId == null) return true;

        CompletableFuture<Boolean> response = new CompletableFuture<>();
        Duration timeout = dialog.timeout();
        response.completeOnTimeout(false, Math.max(1L, timeout.toMillis()), TimeUnit.MILLISECONDS);

        pending.put(uniqueId, new PendingDialog(dialog, response));
        context.audience().showDialog(dialog.dialog());

        boolean allowed = response.join();
        pending.remove(uniqueId);

        if (!allowed && dialog.disconnectMessage() != null) {
            context.connection().disconnect(dialog.disconnectMessage());
        }

        return allowed;
    }

    void handleClick(UUID uniqueId, Key key) {
        PendingDialog pendingDialog = pending.get(uniqueId);
        if (pendingDialog == null || key == null) return;

        Boolean result = pendingDialog.dialog().responses().get(key);
        if (result != null) {
            pendingDialog.response().complete(result);
        }
    }

    void clear(UUID uniqueId) {
        if (uniqueId == null) return;

        PendingDialog pendingDialog = pending.remove(uniqueId);
        if (pendingDialog != null) {
            pendingDialog.response().complete(false);
        }
    }

    private boolean handleResult(ConnectionContext context, ConnectionResult result) {
        if (result == null || result.allowed()) return true;

        if (result.dialog() != null) {
            return showAndAwait(context, result.dialog());
        }

        Component message = result.disconnectMessage();
        if (message != null) {
            context.connection().disconnect(message);
        }
        return false;
    }

    private List<ConnectionCheck> checksSnapshot() {
        synchronized (checks) {
            return List.copyOf(checks.values());
        }
    }

    private ServerConnectionDialog createTermsDialog() {
        Key agreeKey = key("terms/agree");
        Key disagreeKey = key("terms/disagree");

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(server.title())
                        .canCloseWithEscape(false)
                        .body(createTermsBody())
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Akzeptieren", NamedTextColor.WHITE))
                                .tooltip(Component.text("Klicke zum Zustimmen"))
                                .action(DialogAction.customClick(agreeKey, null))
                                .build(),
                        ActionButton.builder(Component.text("Abbrechen", NamedTextColor.WHITE))
                                .tooltip(Component.text("Klicke zum Abbrechen"))
                                .action(DialogAction.customClick(disagreeKey, null))
                                .build()
                )));

        return new ServerConnectionDialog(
                dialog,
                Map.of(agreeKey, true, disagreeKey, false),
                prefixed(Component.text("Du hast den Vorgang abgebrochen.", NamedTextColor.RED)),
                Duration.ofMinutes(1)
        );
    }

    private List<DialogBody> createTermsBody() {
        List<DialogBody> body = new ArrayList<>();

        if (server.links().hasDiscord()) {
            String url = "https://discord.gg/" + server.links().discordToken();
            body.add(DialogBody.plainMessage(
                    Component.text("Info zu Server und Projekten: ", NamedTextColor.GRAY)
                            .append(Component.text(url, NamedTextColor.BLUE)
                                    .decorate(TextDecoration.UNDERLINED)
                                    .clickEvent(ClickEvent.clickEvent(
                                            ClickEvent.Action.OPEN_URL,
                                            ClickEvent.Payload.string(url)
                                    )))
            ));
        }

        if (termsMessages.isEmpty()) {
            body.add(DialogBody.plainMessage(
                    Component.text("Durch das Klicken auf ", NamedTextColor.GRAY)
                            .append(Component.text("Akzeptieren ", TextColor.color(0x00BD09)))
                            .append(Component.text("stimmst du dem Regelwerk und den Nutzungsbedingungen zu.", NamedTextColor.GRAY))
            ));
        } else {
            for (Component message : termsMessages) {
                body.add(DialogBody.plainMessage(message));
            }
        }

        return body;
    }

    public Consumer<RegistryBuilderFactory<@NotNull Dialog, ? extends DialogRegistryEntry.@NotNull Builder>> noticeDialog(
            Component title,
            List<Component> messages,
            Key actionKey
    ) {
        return builder -> builder.empty()
                .base(DialogBase.builder(title == null ? server.title() : title)
                        .canCloseWithEscape(false)
                        .body(messages == null
                                ? List.of()
                                : messages.stream().map(DialogBody::plainMessage).toList())
                        .build())
                .type(DialogType.notice(
                        ActionButton.builder(Component.text("Verstanden", NamedTextColor.WHITE))
                                .action(DialogAction.customClick(actionKey, null))
                                .build()
                ));
    }

    public Key key(String value) {
        String namespace = server.plugin().getId();
        if (namespace == null || namespace.isBlank()) {
            namespace = "project";
        }

        namespace = namespace.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        String path = value == null || value.isBlank() ? "dialog" : value;

        return Key.key(namespace, path.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_"));
    }

    private Component prefixed(Component message) {
        Component prefix = server.plugin().getPrefix();
        return prefix == null ? message : prefix.append(message);
    }

    private record PendingDialog(ServerConnectionDialog dialog, CompletableFuture<Boolean> response) {
    }
}
