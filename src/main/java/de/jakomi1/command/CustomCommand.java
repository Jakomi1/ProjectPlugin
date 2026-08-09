package de.jakomi1.command;

import de.jakomi1.project.ProjectPlugin;
import de.jakomi1.project.Registerable;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

public interface CustomCommand extends CommandExecutor, TabCompleter, Registerable {

    String name();

    default String description() {
        return name() + "-command";
    }

    default String usage() {
        return "/" + name();
    }

    default String permission() {
        return "";
    }

    default PermissionDefault permissionDefault() {
        return null;
    }

    default String generatedPermission(ProjectPlugin plugin) {
        return plugin.getName().toLowerCase()
                + ".command."
                + name().toLowerCase();
    }

    default List<String> aliases() {
        return Collections.emptyList();
    }

    @Override
    boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    );

    @Override
    default @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        return Collections.emptyList();
    }

    @Override
    default void register(ProjectPlugin plugin) {
        CommandMap commandMap = getCommandMap(plugin);

        if (commandMap == null) {
            plugin.getLogger()
                    .warning("CommandMap konnte nicht gefunden werden!");
            return;
        }

        String permission = permission();

        if (permissionDefault() != null) {
            if (permission.isEmpty()) {
                permission = generatedPermission(plugin);
            }

            if (Bukkit.getPluginManager().getPermission(permission) == null) {
                Bukkit.getPluginManager().addPermission(
                        new Permission(
                                permission,
                                permissionDefault()
                        )
                );
            }
        }

        Command dynamicCommand = getCommand(permission);

        commandMap.register(
                plugin.getName(),
                dynamicCommand
        );
    }

    private @NotNull Command getCommand(String permission) {

        Command dynamicCommand = new Command(
                name(),
                description(),
                usage(),
                aliases()
        ) {

            @Override
            public boolean execute(
                    @NotNull CommandSender sender,
                    @NotNull String label,
                    String[] args
            ) {

                if (!permission.isEmpty()
                        && !sender.hasPermission(permission)) {
                    return true;
                }

                return CustomCommand.this.onCommand(
                        sender,
                        this,
                        label,
                        args
                );
            }

            @Override
            public @NotNull List<String> tabComplete(
                    @NotNull CommandSender sender,
                    @NotNull String alias,
                    String[] args
            ) {

                List<String> result = CustomCommand.this.onTabComplete(
                        sender,
                        this,
                        alias,
                        args
                );

                return result != null
                        ? result
                        : Collections.emptyList();
            }
        };

        if (!permission.isEmpty()) {
            dynamicCommand.setPermission(permission);
        }
        return dynamicCommand;
    }

    private static CommandMap getCommandMap(ProjectPlugin plugin) {

        try {
            return Bukkit.getCommandMap();
        } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
        }

        try {
            Field field = Bukkit.getServer()
                    .getClass()
                    .getDeclaredField("commandMap");

            field.setAccessible(true);

            return (CommandMap) field.get(Bukkit.getServer());

        } catch (Exception exception) {

            plugin.getLogger()
                    .warning("CommandMap konnte nicht geladen werden!");

            return null;
        }
    }
}
