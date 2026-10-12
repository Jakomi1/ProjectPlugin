package de.jakomi1.project.nickname;

import de.jakomi1.command.CustomCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Setzt oder entfernt den Anzeigenamen eines Spielers.
 *
 * <p>Nutzung: {@code /nickname <Spieler> <Anzeigename>}. Mit
 * {@code /nickname <Spieler> reset} wird der Nickname wieder entfernt.
 */
public final class NicknameCommand implements CustomCommand {

    private static final int MAX_LENGTH = 48;

    private final NicknameManager manager;

    public NicknameCommand(NicknameManager manager) {
        this.manager = manager;
    }

    @Override
    public String name() {
        return "nickname";
    }

    @Override
    public String description() {
        return "Setzt den Anzeigenamen eines Spielers";
    }

    @Override
    public String usage() {
        return "/nickname <Spieler> <Anzeigename|reset>";
    }

    @Override
    public String permission() {
        return manager.permission();
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return hasAccess(sender);
    }

    @Override
    public PermissionDefault permissionDefault() {
        return null;
    }

    @Override
    public List<String> aliases() {
        return List.of("nick");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!hasAccess(sender)) {
            sender.sendMessage(prefix().append(Component.text(
                    "Dafür fehlt dir die Berechtigung. Benötigt wird die Rolle "
                            + manager.minimumRole().display() + " oder höher.",
                    NamedTextColor.RED
            )));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(prefix().append(Component.text(usage(), NamedTextColor.RED)));
            return true;
        }

        String targetName = args[0];
        OfflinePlayer target = resolve(targetName);

        if (target == null) {
            sender.sendMessage(prefix().append(Component.text(
                    "Spieler \"" + targetName + "\" ist unbekannt.",
                    NamedTextColor.RED
            )));
            return true;
        }

        String realName = target.getName() != null ? target.getName() : targetName;
        String display = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();

        if (isReset(display)) {
            manager.clear(target.getUniqueId());
            sender.sendMessage(prefix().append(Component.text(
                    "Nickname von " + realName + " wurde entfernt.",
                    NamedTextColor.GRAY
            )));
            return true;
        }

        if (display.isBlank()) {
            sender.sendMessage(prefix().append(Component.text(usage(), NamedTextColor.RED)));
            return true;
        }

        if (display.length() > MAX_LENGTH) {
            sender.sendMessage(prefix().append(Component.text(
                    "Der Anzeigename darf höchstens " + MAX_LENGTH + " Zeichen lang sein.",
                    NamedTextColor.RED
            )));
            return true;
        }

        manager.set(target.getUniqueId(), realName, display);

        sender.sendMessage(prefix().append(Component.text("Nickname von ", NamedTextColor.GRAY))
                .append(Component.text(realName, NamedTextColor.WHITE))
                .append(Component.text(" ist jetzt ", NamedTextColor.GRAY))
                .append(manager.name(target.getUniqueId(), realName))
                .append(Component.text(".", NamedTextColor.GRAY)));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);

            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return List.of("reset");
        }

        return List.of();
    }

    private static boolean isReset(String display) {
        return display.equalsIgnoreCase("reset")
                || display.equalsIgnoreCase("off")
                || display.equalsIgnoreCase("clear")
                || display.equals("-");
    }

    @Nullable
    private static OfflinePlayer resolve(String name) {
        if (name == null || name.isBlank()) return null;

        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;

        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached == null || cached.getUniqueId() == null) return null;

        return cached;
    }

    private boolean hasAccess(CommandSender sender) {
        if (sender.hasPermission(manager.permission())) return true;

        return sender instanceof Player player && manager.canUse(player.getUniqueId());
    }

    private Component prefix() {
        Component prefix = manager.server().plugin().getPrefix();
        return prefix != null ? prefix : Component.empty();
    }
}
