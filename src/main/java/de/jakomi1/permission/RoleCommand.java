package de.jakomi1.permission;

import de.jakomi1.command.CustomCommand;
import de.jakomi1.player.PlayerLookup;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Verwaltet Rollen ueber {@code /role set|remove|list}.
 *
 * <p>Der Befehl greift ausschliesslich auf den {@link RoleManager} zu. Eine
 * Rollenaenderung loest dort den Aenderungs-Callback aus, ueber den ein
 * Projekt Tab-Sortierung oder Op-Status nachziehen kann.
 */
public final class RoleCommand implements CustomCommand {

    private static final List<String> ROLE_NAMES = List.of(
            "BOT", "MEMBER", "CONTENT_CREATOR", "BOOSTER", "VIP",
            "DEVELOPER", "BUILDER", "SUPPORTER", "MODERATOR", "ADMIN"
    );

    private final RoleManager manager;

    public RoleCommand(RoleManager manager) {
        this.manager = manager;
    }

    @Override
    public String name() {
        return "role";
    }

    @Override
    public String description() {
        return "Vergibt und entfernt Rollen";
    }

    @Override
    public String permission() {
        return manager.commandPermission();
    }

    @Override
    public List<String> aliases() {
        return List.of("setrole");
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "set" -> handleSet(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(prefix().append(
                    Component.text("Usage: /role set <player> [role]", NamedTextColor.RED)));
            return;
        }

        String roleName = args.length >= 3 ? args[2].toUpperCase(Locale.ROOT) : "MEMBER";
        Role role = parseRole(roleName);

        if (role == null) {
            sender.sendMessage(prefix().append(
                    Component.text("Unbekannte Rolle: " + roleName, NamedTextColor.RED)));
            return;
        }

        if (role.isOwner()) {
            sender.sendMessage(prefix().append(
                    Component.text("Die OWNER-Rolle kann nicht vergeben werden.", NamedTextColor.RED)));
            return;
        }

        PlayerLookup.ResolvedPlayer target = PlayerLookup.resolve(manager.server(), args[1]);

        if (target == null) {
            assignPending(sender, args[1], role);
            return;
        }

        manager.setRole(target.uuid(), role);
        storeName(target);

        sender.sendMessage(prefix().append(
                Component.text(target.name() + " ist jetzt ", NamedTextColor.GRAY)
                        .append(role.coloredDisplay())
                        .append(Component.text(".", NamedTextColor.GRAY))));

        Player online = Bukkit.getPlayer(target.uuid());
        if (online == null) return;

        online.sendMessage(prefix().append(
                Component.text("Deine Rolle wurde zu ", NamedTextColor.GRAY)
                        .append(role.coloredDisplay())
                        .append(Component.text(" geändert.", NamedTextColor.GRAY))));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(prefix().append(
                    Component.text("Usage: /role remove <player>", NamedTextColor.RED)));
            return;
        }

        PlayerLookup.ResolvedPlayer target = PlayerLookup.resolve(manager.server(), args[1]);

        if (target == null) {
            removePending(sender, args[1]);
            return;
        }

        manager.clearOverride(target.uuid());

        Role defaultRole = manager.roleOf(target.uuid());

        sender.sendMessage(prefix().append(
                Component.text(target.name() + " wurde auf ", NamedTextColor.GRAY)
                        .append(defaultRole.coloredDisplay())
                        .append(Component.text(" zurückgesetzt.", NamedTextColor.GRAY))));

        Player online = Bukkit.getPlayer(target.uuid());
        if (online == null) return;

        online.sendMessage(prefix().append(
                Component.text("Deine Rolle wurde auf ", NamedTextColor.GRAY)
                        .append(defaultRole.coloredDisplay())
                        .append(Component.text(" zurückgesetzt.", NamedTextColor.GRAY))));
    }

    /**
     * Eine Rolle laesst sich auch offline vergeben. Gesucht wird deshalb
     * zuerst lokal in den Tabellen des Projekts und nicht ueber den
     * blockierenden {@code Bukkit.getOfflinePlayer}-Aufruf, der den
     * Server-Tick anhalten wuerde.
     */
    private void assignPending(CommandSender sender, String name, Role role) {
        manager.pendingRoles().set(name, role);

        sender.sendMessage(prefix().append(
                Component.text(name + " ist dem Server noch unbekannt. Die Rolle ", NamedTextColor.GRAY)
                        .append(role.coloredDisplay())
                        .append(Component.text(" wird beim ersten Beitritt vergeben.", NamedTextColor.GRAY))));
    }

    private void removePending(CommandSender sender, String name) {
        if (manager.pendingRoles().clear(name)) {
            sender.sendMessage(prefix().append(
                    Component.text("Die offene Rollen-Vergabe für " + name + " wurde entfernt.",
                            NamedTextColor.GRAY)));
            return;
        }

        sender.sendMessage(prefix().append(
                Component.text(name + " ist unbekannt. Der Spieler muss mindestens einmal "
                        + "auf dem Server gewesen sein.", NamedTextColor.RED)));
    }

    /**
     * Schreibt den Namen nach, damit /role list ihn anzeigt. Der Name
     * wird auch dann gesetzt, wenn schon einer steht - eine Rollen-
     * Veraenderung ist der Moment, in dem der juengste bekannte Name
     * erfasst wird.
     */
    private void storeName(PlayerLookup.ResolvedPlayer target) {
        var table = manager.table();

        if (target.name().equals(table.getName(target.uuid()))) return;

        table.setName(target.uuid(), target.name());
    }

    private void handleList(CommandSender sender) {
        var table = manager.table();
        var entries = table.entrySet();

        if (entries.isEmpty()) {
            sender.sendMessage(prefix().append(
                    Component.text("Keine Rollen vergeben.", NamedTextColor.YELLOW)));
            return;
        }

        Map<UUID, String> names = knownNames();

        sender.sendMessage(prefix().append(
                Component.text("Rollenübersicht:", NamedTextColor.GOLD)));

        for (var entry : entries) {
            UUID uuid = parseUuid(entry.getKey());
            if (uuid == null) continue;

            var roleEntry = entry.getValue();
            Role role = roleEntry.role() != null ? roleEntry.role() : Role.MEMBER;

            // Ein Rolleneintrag kann ohne Namen angelegt worden sein
            // (z. B. durch setRole). Statt einer UUID wird dann der lokal
            // bekannte Name aufgeloest und gleich mit zurueckgeschrieben.
            String name = roleEntry.name();
            if (name == null || name.isBlank()) {
                name = names.get(uuid);
            }
            if (name == null || name.isBlank()) {
                OfflinePlayer cached = Bukkit.getOfflinePlayer(uuid);
                name = cached.getName();
            }
            if (name == null || name.isBlank()) {
                name = "<unbekannt " + uuid.toString().substring(0, 8) + ">";
            } else if (roleEntry.name() == null || roleEntry.name().isBlank()) {
                table.setName(uuid, name);
            }

            sender.sendMessage(Component.text("  " + name + " → ", NamedTextColor.GRAY)
                    .append(role.coloredDisplay()));
        }

        for (var entry : manager.pendingRoles().entrySet()) {
            Role role = Role.role(entry.getValue());
            if (role == null) continue;

            sender.sendMessage(Component.text("  " + entry.getKey() + " → ", NamedTextColor.GRAY)
                    .append(role.coloredDisplay())
                    .append(Component.text(" (ausstehend)", NamedTextColor.YELLOW)));
        }
    }

    /**
     * Alle Namen, die der Server lokal kennt - ohne Netzwerkzugriff.
     * Online-Spieler zuerst, danach die Spielzeit-Tabelle, danach die
     * Rollen-Tabelle selbst.
     */
    private Map<UUID, String> knownNames() {
        Map<UUID, String> names = new HashMap<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            names.put(player.getUniqueId(), player.getName());
        }

        for (var entry : manager.server().playtime().table().entrySet()) {
            if (entry.getValue().name() == null || entry.getValue().name().isBlank()) continue;

            names.putIfAbsent(entry.getKey(), entry.getValue().name());
        }

        for (var entry : manager.table().entrySet()) {
            String stored = entry.getValue().name();
            if (stored == null || stored.isBlank()) continue;

            UUID uuid = parseUuid(entry.getKey());
            if (uuid != null) names.putIfAbsent(uuid, stored);
        }

        return names;
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(prefix().append(
                Component.text("Usage:", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("  /role set <player> [role]", NamedTextColor.YELLOW)
                .append(Component.text(" - Rolle setzen (default: MEMBER)", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /role remove <player>", NamedTextColor.YELLOW)
                .append(Component.text(" - Rolle zurücksetzen", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /role list", NamedTextColor.YELLOW)
                .append(Component.text(" - Alle Rollen anzeigen", NamedTextColor.GRAY)));
    }

    private Role parseRole(String name) {
        return switch (name) {
            case "BOT" -> Role.role("BOT");
            case "MEMBER" -> Role.MEMBER;
            case "CONTENT_CREATOR" -> Role.CONTENT_CREATOR;
            case "BOOSTER" -> Role.BOOSTER;
            case "VIP" -> Role.VIP;
            case "DEVELOPER" -> Role.DEVELOPER;
            case "BUILDER" -> Role.BUILDER;
            case "SUPPORTER" -> Role.SUPPORTER;
            case "MODERATOR" -> Role.MODERATOR;
            case "ADMIN" -> Role.ADMIN;
            default -> null;
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return Arrays.asList("set", "remove", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && !args[0].equalsIgnoreCase("list")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);

            // Offline-Spieler sind sinnvolle Ziele, also muessen sie auch
            // vorgeschlagen werden - sonst ist die Vervollstaendigung genau
            // fuer die wichtigsten Faelle leer.
            Stream<String> names = Stream.concat(
                    Bukkit.getOnlinePlayers().stream().map(Player::getName),
                    manager.server().playtime().knownNames().stream()
            );

            return names.distinct()
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return ROLE_NAMES.stream()
                    .filter(r -> r.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private Component prefix() {
        Component prefix = manager.server().plugin().getPrefix();
        return prefix != null ? prefix : Component.empty();
    }
}
