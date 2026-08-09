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

public final class LinkCommand implements CustomCommand {

    private final LinkManager manager;
    private final LinkDefinition definition;

    public LinkCommand(LinkManager manager, LinkDefinition definition) {
        this.manager = manager;
        this.definition = definition;
    }

    @Override
    public String name() {
        return definition.name();
    }

    @Override
    public String description() {
        return "Öffnet den " + definition.name() + "-Link";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        sender.sendMessage(prefix().append(Component.text(definition.name() + ": ")
                .append(Component.text(definition.url())
                        .color(NamedTextColor.BLUE)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(definition.url())))));
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
