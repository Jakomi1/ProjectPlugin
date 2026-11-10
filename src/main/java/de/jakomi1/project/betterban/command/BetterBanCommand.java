package de.jakomi1.project.betterban.command;

import de.jakomi1.command.CustomCommand;
import de.jakomi1.permission.Role;
import de.jakomi1.project.betterban.BetterBanManager;
import de.jakomi1.project.betterban.BetterBanMessages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Gemeinsame Basis aller betterban-Befehle.
 *
 * <p>Die Freigabe laeuft nicht ueber Bukkit-Permissions, sondern ueber die
 * Rollen des {@link de.jakomi1.permission.RoleManager}. Die Konsole darf
 * immer zugreifen, Spieler benoetigen mindestens die konfigurierte Rolle
 * (standardmaessig {@link Role#SUPPORTER_PLUS}).</p>
 */
public abstract class BetterBanCommand implements CustomCommand {

    protected final BetterBanManager manager;

    protected BetterBanCommand(BetterBanManager manager) {
        this.manager = manager;
    }

    public BetterBanManager manager() {
        return manager;
    }

    protected BetterBanMessages msg() {
        return manager.messages();
    }

    @Override
    public boolean canUse(@NotNull CommandSender sender) {
        return manager.canModerate(sender);
    }

    /** Name eines Spielers mit UUID-Fallback. */
    protected String nameOf(OfflinePlayer player) {
        if (player == null) {
            return "Unbekannt";
        }

        String name = player.getName();

        if (name != null && !name.isBlank()) {
            return name;
        }

        return manager.nameOf(player.getUniqueId());
    }

    protected String nameOf(UUID uuid) {
        return manager.nameOf(uuid);
    }

    /** Alle je verbundenen Spielernamen, gefiltert nach dem Tab-Prefix. */
    protected List<String> joinedNames(String prefix) {
        return filter(manager.joinedNames(), prefix);
    }

    /** Namen aller aktuell gebannten Spieler. */
    protected List<String> bannedNames(String prefix) {
        List<String> names = new ArrayList<>();

        for (UUID uuid : manager.allBans().keySet()) {
            names.add(manager.nameOf(uuid));
        }

        return filter(names, prefix);
    }

    /** Namen aller aktuell chat-gebannten Spieler. */
    protected List<String> chatBannedNames(String prefix) {
        List<String> names = new ArrayList<>();

        for (UUID uuid : manager.allChatBans().keySet()) {
            names.add(manager.nameOf(uuid));
        }

        return filter(names, prefix);
    }

    /** Namen aller aktuell voice-gebannten Spieler. */
    protected List<String> voiceBannedNames(String prefix) {
        List<String> names = new ArrayList<>();

        for (UUID uuid : manager.allVoiceBans().keySet()) {
            names.add(manager.nameOf(uuid));
        }

        return filter(names, prefix);
    }

    /** Online-Spielernamen, gefiltert nach dem Tab-Prefix. */
    protected List<String> onlineNames(String prefix) {
        List<String> names = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }

        return filter(names, prefix);
    }

    protected static List<String> filter(Collection<String> values, String prefix) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        String needle = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();

        for (String value : values) {
            if (value == null) {
                continue;
            }

            if (needle.isEmpty() || value.toLowerCase(Locale.ROOT).startsWith(needle)) {
                result.add(value);
            }
        }

        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    /** Verbindet Argumente ab {@code from} zu einem Grund. */
    protected static String joinArgs(String[] args, int from) {
        if (args == null || from >= args.length) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        for (int index = from; index < args.length; index++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(args[index]);
        }

        String reason = builder.toString().trim();
        return reason.isEmpty() ? null : reason;
    }

    /** Informiert einen online Spieler, falls vorhanden. */
    protected void notifyOnline(UUID uuid, net.kyori.adventure.text.Component message) {
        if (uuid == null || message == null) {
            return;
        }

        Player player = Bukkit.getPlayer(uuid);

        if (player != null && player.isOnline()) {
            player.sendMessage(message);
        }
    }
}
