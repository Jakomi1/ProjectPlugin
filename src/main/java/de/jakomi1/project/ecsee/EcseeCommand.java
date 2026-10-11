/*
 * EcseeCommand – Enderchest-Pendant zum InvseeCommand.
 * Basiert auf dem Port des Plugins "Invsee" (at.noahb.invsee, Autor: MCmitNoah,
 * GNU General Public License v3.0, GPL-3.0). Siehe LICENSE.md in diesem Repository.
 *
 * Erstellt von Jakomi1 (10.10.2026) für ProjectPlugin.
 */
package de.jakomi1.project.ecsee;

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

import java.util.List;

public final class EcseeCommand implements CustomCommand {

    private final EcseeManager manager;

    public EcseeCommand(EcseeManager manager) {
        this.manager = manager;
    }

    @Override
    public String name() {
        return "ecsee";
    }

    @Override
    public String description() {
        return "Öffnet die Enderchest eines Spielers";
    }

    @Override
    public String usage() {
        return "/ecsee <Spieler>";
    }

    @Override
    public List<String> aliases() {
        return List.of("esee", "enderchestsee");
    }

    @Override
    public String permission() {
        return manager.permission();
    }

    @Override
    public PermissionDefault permissionDefault() {
        return PermissionDefault.OP;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur für Spieler", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text(usage(), NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (target == null || !target.hasPlayedBefore()) {
            player.sendMessage(Component.text("Spieler " + args[0] + " wurde nicht gefunden.", NamedTextColor.RED));
            return true;
        }

        manager.sessions().addSubscriberToSession(target, player.getUniqueId());
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) return List.of();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .toList();
    }
}
