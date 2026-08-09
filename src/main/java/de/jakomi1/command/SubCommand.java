package de.jakomi1.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public interface SubCommand {

    @NotNull
    String name();

    @NotNull
    default List<String> aliases() {
        return Collections.emptyList();
    }

    default String permission() {
        return "";
    }

    @NotNull
    default String usage() {
        return "/<cmd> " + name();
    }

    boolean execute(@NotNull CommandSender sender, @NotNull String[] args);

    default @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
