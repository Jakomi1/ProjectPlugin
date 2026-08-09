package de.jakomi1.project.link;

import de.jakomi1.project.AutoManager;
import de.jakomi1.project.ProjectServer;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LinkManager implements AutoManager {

    private static final String DISCORD_COMMAND = "discord";
    private static final String DISCORD_GG = "discord.gg/";
    private static final String DISCORD_INVITE = "discord.com/invite/";

    private final ProjectServer server;
    private final Map<String, LinkDefinition> links = new LinkedHashMap<>();
    private final DiscordCommand discordCommand;

    private String discordToken;
    private boolean enabled;
    private boolean auto = true;
    private boolean registerCommand = true;
    private final Set<String> registered = new java.util.HashSet<>();

    public LinkManager(ProjectServer server) {
        this.server = server;
        this.discordCommand = new DiscordCommand(this);
    }

    @Override
    public LinkManager enable() {
        if (enabled) return this;
        enabled = true;

        if (registerCommand) {
            if (discordToken != null) {
                discordCommand.register(server.plugin());
                registered.add(DISCORD_COMMAND);
            }

            for (LinkDefinition link : links.values()) {
                registerLinkCommand(link);
            }
        }
        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean auto() {
        return auto;
    }

    @Override
    public LinkManager auto(boolean auto) {
        this.auto = auto;
        return this;
    }

    public LinkManager command(boolean registerCommand) {
        this.registerCommand = registerCommand;
        return this;
    }

    public boolean command() {
        return registerCommand;
    }

    public LinkManager discord(String inviteToken) {
        String token = sanitizeToken(inviteToken);
        this.discordToken = token;

        if (token != null && enabled && registerCommand && !registered.contains(DISCORD_COMMAND)) {
            discordCommand.register(server.plugin());
            registered.add(DISCORD_COMMAND);
        }
        return this;
    }

    public String discordToken() {
        return discordToken;
    }

    public boolean hasDiscord() {
        return discordToken != null;
    }

    public LinkManager link(String name, String url) {
        if (name == null || name.isBlank() || url == null || url.isBlank()) return this;

        String normalized = normalizeName(name);

        if (normalized.equals(DISCORD_COMMAND)) {
            return discord(url);
        }

        LinkDefinition definition = new LinkDefinition(normalized, url);
        links.put(normalized, definition);

        if (enabled && registerCommand && !registered.contains(normalized)) {
            registerLinkCommand(definition);
        }
        return this;
    }

    public LinkDefinition get(String name) {
        return name == null ? null : links.get(normalizeName(name));
    }

    public boolean has(String name) {
        return get(name) != null;
    }

    public LinkManager remove(String name) {
        if (name != null) {
            links.remove(normalizeName(name));
        }
        return this;
    }

    public Collection<LinkDefinition> links() {
        return java.util.List.copyOf(links.values());
    }

    public int size() {
        return links.size();
    }

    public ProjectServer server() {
        return server;
    }

    private void registerLinkCommand(LinkDefinition definition) {
        new LinkCommand(this, definition).register(server.plugin());
        registered.add(definition.name());
    }

    private static String sanitizeToken(String input) {
        if (input == null) return null;

        String token = input.trim();

        if (token.startsWith("http://")) {
            token = token.substring("http://".length());
        } else if (token.startsWith("https://")) {
            token = token.substring("https://".length());
        }

        if (token.startsWith(DISCORD_GG)) {
            token = token.substring(DISCORD_GG.length());
        } else if (token.startsWith(DISCORD_INVITE)) {
            token = token.substring(DISCORD_INVITE.length());
        }

        return token.isEmpty() ? null : token;
    }

    private static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }
}
