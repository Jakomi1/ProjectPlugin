package de.jakomi1.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SubCommandRegistry {

    private final String commandName;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    private Component missingPermissionMessage = Component.text(
            "Dazu hast du keine Berechtigung!",
            NamedTextColor.RED
    );
    private Component helpTitle = Component.text("Befehle:", NamedTextColor.GOLD);
    private NamedTextColor helpLineColor = NamedTextColor.GRAY;

    public SubCommandRegistry(String commandName) {
        this.commandName = commandName;
    }

    public SubCommandRegistry sub(SubCommand subCommand) {
        if (subCommand == null) return this;

        subCommands.put(subCommand.name().toLowerCase(Locale.ROOT), subCommand);
        for (String alias : subCommand.aliases()) {
            subCommands.putIfAbsent(alias.toLowerCase(Locale.ROOT), subCommand);
        }
        return this;
    }


    public boolean dispatch(CommandSender sender, String[] args) {
        if (args == null || args.length == 0) {
            sendHelp(sender);
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            sendHelp(sender);
            return true;
        }

        String permission = subCommand.permission();
        if (!permission.isEmpty() && !sender.hasPermission(permission)) {
            sender.sendMessage(missingPermissionMessage);
            return true;
        }

        String[] rest = new String[args.length - 1];
        System.arraycopy(args, 1, rest, 0, rest.length);
        return subCommand.execute(sender, rest);
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args == null || args.length == 0) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();

            for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
                SubCommand subCommand = entry.getValue();
                if (!allowed(sender, subCommand)) continue;

                String candidate = entry.getKey();
                if (prefix.isEmpty() || candidate.startsWith(prefix)) {
                    result.add(candidate);
                }
            }
            return result;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null || !allowed(sender, subCommand)) {
            return Collections.emptyList();
        }

        List<String> result = subCommand.tabComplete(sender, args);
        return result != null ? result : Collections.emptyList();
    }

    public void sendHelp(CommandSender sender) {
        sender.sendMessage(helpTitle);
        for (SubCommand subCommand : subCommands.values()) {
            sender.sendMessage(Component.text(
                    "  " + subCommand.usage().replace("<cmd>", "/" + commandName),
                    helpLineColor
            ));
        }
    }

    public SubCommandRegistry missingPermissionMessage(Component message) {
        if (message != null) this.missingPermissionMessage = message;
        return this;
    }

    public SubCommandRegistry helpTitle(Component helpTitle) {
        if (helpTitle != null) this.helpTitle = helpTitle;
        return this;
    }

    public SubCommandRegistry helpLineColor(NamedTextColor helpLineColor) {
        if (helpLineColor != null) this.helpLineColor = helpLineColor;
        return this;
    }

    public @Nullable SubCommand get(String name) {
        return name == null ? null : subCommands.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean contains(String name) {
        return get(name) != null;
    }

    public int size() {
        return subCommands.size();
    }

    public Map<String, SubCommand> subCommands() {
        return Collections.unmodifiableMap(subCommands);
    }

    private boolean allowed(CommandSender sender, SubCommand subCommand) {
        String permission = subCommand.permission();
        return permission.isEmpty() || sender.hasPermission(permission);
    }
}
