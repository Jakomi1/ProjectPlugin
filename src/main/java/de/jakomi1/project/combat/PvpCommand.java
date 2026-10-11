package de.jakomi1.project.combat;

import de.jakomi1.command.CustomCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Schaltet den PvP-Schalter des Spielers um.
 *
 * <p>Solange die Aenderungshinderung laeuft, laesst sich nichts aendern - weder
 * ein- noch ausschalten. Das ist bewusst nur eine Aenderungshinderung;
 * kaempfen darf weiterhin.
 */
public final class PvpCommand implements CustomCommand {

    private final CombatManager combat;

    public PvpCommand(CombatManager combat) {
        this.combat = combat;
    }

    @Override
    public String name() {
        return "pvp";
    }

    @Override
    public String description() {
        return "Schaltet PvP fuer dich ein oder aus";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Component prefix = combat.server().plugin().getPrefix();
            sender.sendMessage((prefix != null ? prefix : Component.empty())
                    .append(combat.message(PvpMessage.PLAYERS_ONLY, null)));
            return true;
        }

        if (args.length != 0) {
            combat.chatMessage(player, PvpMessage.USAGE);
            return true;
        }

        if (combat.pvpLocked(player)) {
            combat.actionBar(player, PvpMessage.TOGGLE_LOCKED);
            return true;
        }

        if (combat.pvpEnabled(player)) {
            combat.deactivatePvp(player);
            combat.actionBar(player, PvpMessage.TOGGLE_DISABLED);
        } else {
            combat.activatePvp(player);
            combat.actionBar(player, PvpMessage.TOGGLE_ENABLED);
        }

        combat.server().tab().update(player);

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
