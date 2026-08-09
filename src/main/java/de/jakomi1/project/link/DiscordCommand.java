package de.jakomi1.project.link;

import de.jakomi1.command.CustomCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class DiscordCommand implements CustomCommand {

    private final LinkManager manager;

    public DiscordCommand(LinkManager manager) {
        this.manager = manager;
    }

    @Override
    public String name() {
        return "discord";
    }

    @Override
    public String description() {
        return "Zeigt den Discord-Link";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String token = manager.discordToken();

        if (token == null) {
            sender.sendMessage(prefix().append(Component.text("Discord-Link ist nicht konfiguriert.", NamedTextColor.RED)));
            return true;
        }

        sender.sendMessage(prefix().append(Component.text("Trete unserem Discord bei: ")
                .append(Component.text("discord.gg/" + token)
                        .color(NamedTextColor.BLUE)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl("https://discord.gg/" + token)))));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }

    private Component prefix() {
        Component prefix = manager.server().plugin().getPrefix();
        return prefix != null ? prefix : Component.empty();
    }
}
