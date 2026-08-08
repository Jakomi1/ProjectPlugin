package de.jakomi1.kingdoms.command;

import de.jakomi1.kingdoms.Registry;
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

import static de.jakomi1.kingdoms.Kingdoms.plugin;

public interface  CustomCommand extends CommandExecutor, TabCompleter {

    String name();

    default String permission() {
        return "";
    }

    default PermissionDefault permissionDefault() {
        return null;
    }

    default String generatedPermission() {
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

    default void register() {

        CommandMap commandMap = getCommandMap();

        if (commandMap == null) {
            plugin.getLogger()
                    .warning("CommandMap konnte nicht gefunden werden!");
            return;
        }

        String permission = permission();

        if (!permission.isEmpty() && permissionDefault() != null) {
            if (Bukkit.getPluginManager().getPermission(permission) == null) {
                Bukkit.getPluginManager().addPermission(
                        new Permission(
                                permission,
                                permissionDefault()
                        )
                );
            }
        } else if (permission.isEmpty() && permissionDefault() != null) {
            permission = generatedPermission();

            if (Bukkit.getPluginManager().getPermission(permission) == null) {
                Bukkit.getPluginManager().addPermission(
                        new Permission(
                                permission,
                                permissionDefault()
                        )
                );
            }
        }

        String finalPermission = permission;

        Command dynamicCommand = new Command(
                name(),
                name() + "-command",
                "",
                aliases()
        ) {

            @Override
            public boolean execute(
                    @NotNull CommandSender sender,
                    @NotNull String label,
                    String[] args
            ) {

                if (!finalPermission.isEmpty()
                        && !sender.hasPermission(finalPermission)) {
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

        if (!finalPermission.isEmpty()) {
            dynamicCommand.setPermission(finalPermission);
        }

        commandMap.register(
                plugin.getName(),
                dynamicCommand
        );
    }

    private static CommandMap getCommandMap() {

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

    static void registerAll() {
        Registry.getCommands()
                .forEach(CustomCommand::register);
    }
}
