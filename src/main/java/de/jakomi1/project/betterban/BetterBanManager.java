package de.jakomi1.project.betterban;

import de.jakomi1.command.CustomCommand;
import de.jakomi1.permission.Role;
import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.project.betterban.command.BanCommand;
import de.jakomi1.project.betterban.command.BanListCommand;
import de.jakomi1.project.betterban.command.ChatBanCommand;
import de.jakomi1.project.betterban.command.ChatBanListCommand;
import de.jakomi1.project.betterban.command.ChatUnbanCommand;
import de.jakomi1.project.betterban.command.KickCommand;
import de.jakomi1.project.betterban.command.TempBanCommand;
import de.jakomi1.project.betterban.command.UnbanCommand;
import de.jakomi1.project.betterban.command.VoiceBanCommand;
import de.jakomi1.project.betterban.command.VoiceBanListCommand;
import de.jakomi1.project.betterban.command.VoiceUnbanCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BetterBan als Manager im ProjectPlugin-System.
 *
 * <p>Es gibt keine eigene Datenbank und keine eigene Permission-Verwaltung:
 * Bans liegen in ProjectPlugin-{@code Table}s, Befehle sind
 * {@link CustomCommand}s und die Freigabe laeuft ueber den
 * {@link de.jakomi1.permission.RoleManager}. Standardmaessig darf ab
 * {@link Role#SUPPORTER_PLUS} moderiert werden; die Konsole immer.</p>
 *
 * <p>Der Manager ist standardmaessig aktiv ({@link #defaultState()} true),
 * damit die Aufnahme in {@code ProjectServer.autoManagers} ausreicht.</p>
 */
public final class BetterBanManager implements Manager {

    private final ProjectServer server;
    private final BanTable banTable = new BanTable();
    private final ChatBanTable chatBanTable = new ChatBanTable();
    private final VoiceBanTable voiceBanTable = new VoiceBanTable();
    private final HasJoinedTable hasJoinedTable = new HasJoinedTable();
    private final List<CustomCommand> commands = new ArrayList<>();
    private final List<CustomCommand> voiceCommands = new ArrayList<>();
    private final Set<UUID> voiceDisabled = ConcurrentHashMap.newKeySet();

    private boolean enabled;
    private boolean tablesRegistered;
    private boolean voiceCommandsRegistered;

    private Role minimumRole = Role.SUPPORTER_PLUS;
    private String webhookUrl;

    private BetterBanMessages messages = new BetterBanMessages(null);
    private BetterBanListener listener;
    private VoiceChatHook voiceChatHook;

    public BetterBanManager(ProjectServer server) {
        this.server = server;
    }

    @Override
    public BetterBanManager enable() {
        if (enabled) {
            return this;
        }

        enabled = true;

        registerTables();
        messages = new BetterBanMessages(prefix());

        registerCommand(new BanCommand(this));
        registerCommand(new TempBanCommand(this));
        registerCommand(new UnbanCommand(this));
        registerCommand(new KickCommand(this));
        registerCommand(new BanListCommand(this));
        registerCommand(new ChatBanCommand(this));
        registerCommand(new ChatBanListCommand(this));
        registerCommand(new ChatUnbanCommand(this));

        listener = new BetterBanListener(this);
        listener.register(server.plugin());

        tryInstallVoiceChat();

        server.plugin().getLogger().info(
                "BetterBan aktiviert (Mindestrolle: " + minimumRole.name() + ")."
        );

        return this;
    }

    /**
     * Versucht die optionale Voicechat-Integration zu installieren.
     *
     * <p>Wird auch vom {@link BetterBanListener} aufgerufen, wenn voicechat
     * erst nach ProjectPlugin aktiviert wird. Ohne diesen Nachzieher waeren
     * die Voice-Befehle bei einer spaeteren Lade-Reihenfolge nie verfuegbar.</p>
     */
    public void tryInstallVoiceChat() {
        if (!enabled || voiceChatHook != null) {
            return;
        }

        voiceChatHook = VoiceChatHook.install(this, server.plugin());

        if (voiceChatHook != null && voiceChatHook.available()) {
            onVoiceChatReady();
        }
    }

    /** Wird gerufen, wenn voicechat deaktiviert wird. */
    public void onVoiceChatUnavailable() {
        voiceChatHook = null;
        voiceDisabled.clear();

        // Die Voice-Befehle gehoeren zur Voicechat-Integration. Ohne diesen
        // Schritt wuerden sie beim naechsten Aktivieren doppelt registriert.
        for (CustomCommand command : voiceCommands) {
            command.unregister(server.plugin());
            commands.remove(command);
        }

        voiceCommands.clear();
        voiceCommandsRegistered = false;
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }

        enabled = false;

        if (listener != null) {
            listener.unregister();
            listener = null;
        }

        for (CustomCommand command : commands) {
            command.unregister(server.plugin());
        }

        voiceChatHook = null;
        voiceCommandsRegistered = false;
        voiceDisabled.clear();
        voiceCommands.clear();
        commands.clear();

        flushAll();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean defaultState() {
        return true;
    }

    public ProjectServer server() {
        return server;
    }

    public BetterBanMessages messages() {
        return messages;
    }

    public VoiceChatHook voiceChatHook() {
        return voiceChatHook;
    }

    /** Wird vom {@link VoiceChatHook} aufgerufen, sobald voicechat bereit ist. */
    public void onVoiceChatReady() {
        enableVoiceCommands();

        if (voiceChatHook == null) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshVoiceDisabled(player.getUniqueId());
        }
    }

    private void enableVoiceCommands() {
        if (!enabled || voiceCommandsRegistered) {
            return;
        }

        voiceCommandsRegistered = true;

        VoiceBanCommand voiceBan = new VoiceBanCommand(this);
        VoiceBanListCommand voiceBanList = new VoiceBanListCommand(this);
        VoiceUnbanCommand voiceUnban = new VoiceUnbanCommand(this);

        registerCommand(voiceBan);
        registerCommand(voiceBanList);
        registerCommand(voiceUnban);

        voiceCommands.add(voiceBan);
        voiceCommands.add(voiceBanList);
        voiceCommands.add(voiceUnban);

        server.plugin().getLogger().info("BetterBan: Voicechat-Befehle registriert.");
    }

    private void registerCommand(CustomCommand command) {
        command.register(server.plugin());
        commands.add(command);
    }

    private Component prefix() {
        Component prefix = server.plugin().getPrefix();

        if (prefix == null) {
            return Component.text("[BetterBan] ", NamedTextColor.DARK_AQUA);
        }

        return prefix;
    }

    private void registerTables() {
        if (tablesRegistered) {
            return;
        }

        tablesRegistered = true;

        banTable.register(server.plugin());
        chatBanTable.register(server.plugin());
        voiceBanTable.register(server.plugin());
        hasJoinedTable.register(server.plugin());
    }

    // ---------------------------------------------------------------------
    // Bans
    // ---------------------------------------------------------------------

    public BanEntry banEntry(UUID uuid) {
        return active(banTable, uuid);
    }

    public boolean isBanned(UUID uuid) {
        return banEntry(uuid) != null;
    }

    public void ban(UUID uuid, long endTimestamp, String reason) {
        if (uuid == null) {
            return;
        }

        banTable.put(uuid, new BanEntry(endTimestamp, reason));
        banTable.flushNow();
    }

    public void permanentBan(UUID uuid, String reason) {
        ban(uuid, BanEntry.PERMANENT, reason);
    }

    public void tempBan(UUID uuid, long durationMillis, String reason) {
        ban(uuid, System.currentTimeMillis() + Math.max(durationMillis, 0L), reason);
    }

    public void unban(UUID uuid) {
        if (uuid == null) {
            return;
        }

        banTable.remove(uuid);
        banTable.flushNow();
    }

    public Map<UUID, BanEntry> allBans() {
        return activeEntries(banTable);
    }

    public void clearExpiredBans() {
        clearExpired(banTable);
    }

    // ---------------------------------------------------------------------
    // Chat-Bans
    // ---------------------------------------------------------------------

    public BanEntry chatBanEntry(UUID uuid) {
        return active(chatBanTable, uuid);
    }

    public boolean isChatBanned(UUID uuid) {
        return chatBanEntry(uuid) != null;
    }

    public void chatBan(UUID uuid, long endTimestamp, String reason) {
        if (uuid == null) {
            return;
        }

        chatBanTable.put(uuid, new BanEntry(endTimestamp, reason));
        chatBanTable.flushNow();
    }

    public void chatPermanentBan(UUID uuid, String reason) {
        chatBan(uuid, BanEntry.PERMANENT, reason);
    }

    public void chatTempBan(UUID uuid, long durationMillis, String reason) {
        chatBan(uuid, System.currentTimeMillis() + Math.max(durationMillis, 0L), reason);
    }

    public void chatUnban(UUID uuid) {
        if (uuid == null) {
            return;
        }

        chatBanTable.remove(uuid);
        chatBanTable.flushNow();
    }

    public Map<UUID, BanEntry> allChatBans() {
        return activeEntries(chatBanTable);
    }

    public void clearExpiredChatBans() {
        clearExpired(chatBanTable);
    }

    // ---------------------------------------------------------------------
    // Voice-Bans
    // ---------------------------------------------------------------------

    public BanEntry voiceBanEntry(UUID uuid) {
        BanEntry entry = active(voiceBanTable, uuid);

        if (entry == null) {
            reenableVoice(uuid);
        }

        return entry;
    }

    public boolean isVoiceBanned(UUID uuid) {
        return voiceBanEntry(uuid) != null;
    }

    public void voiceBan(UUID uuid, long endTimestamp, String reason) {
        if (uuid == null) {
            return;
        }

        voiceBanTable.put(uuid, new BanEntry(endTimestamp, reason));
        voiceBanTable.flushNow();

        if (endTimestamp != BanEntry.PERMANENT) {
            long delayTicks = Math.max((endTimestamp - System.currentTimeMillis()) / 50L, 1L);
            server.scheduler().runLater(() -> refreshVoiceDisabled(uuid), delayTicks);
        }
    }

    public void voicePermanentBan(UUID uuid, String reason) {
        voiceBan(uuid, BanEntry.PERMANENT, reason);
    }

    public void voiceTempBan(UUID uuid, long durationMillis, String reason) {
        voiceBan(uuid, System.currentTimeMillis() + Math.max(durationMillis, 0L), reason);
    }

    public void voiceUnban(UUID uuid) {
        if (uuid == null) {
            return;
        }

        voiceBanTable.remove(uuid);
        voiceBanTable.flushNow();
    }

    public Map<UUID, BanEntry> allVoiceBans() {
        return activeEntries(voiceBanTable);
    }

    public void clearExpiredVoiceBans() {
        clearExpired(voiceBanTable);
    }

    /** Schaltet die Voicechat-Verbindung passend zum aktuellen Voice-Ban. */
    public void refreshVoiceDisabled(UUID uuid) {
        if (voiceChatHook == null || uuid == null) {
            return;
        }

        if (isVoiceBanned(uuid)) {
            voiceDisabled.add(uuid);
            voiceChatHook.setDisabled(uuid, true);
        } else {
            reenableVoice(uuid);
        }
    }

    private void reenableVoice(UUID uuid) {
        if (voiceChatHook == null || uuid == null) {
            return;
        }

        if (voiceDisabled.remove(uuid)) {
            voiceChatHook.setDisabled(uuid, false);
        }
    }

    // ---------------------------------------------------------------------
    // Spieler-Tracking
    // ---------------------------------------------------------------------

    public boolean hasJoined(UUID uuid) {
        return uuid != null && hasJoinedTable.containsKey(uuid);
    }

    /**
     * Loest einen Spielernamen ohne blockierende Mojang-Abfrage auf.
     *
     * <p>Reihenfolge: bereits verbundene Spieler ({@code has_joined}), dann
     * online Spieler und zuletzt der lokale Mojang-Cache von Paper. Nur wenn
     * all das fehlschlaegt, wird die Eingabe als UUID interpretiert. Dadurch
     * blockiert kein Befehl den Server-Thread.</p>
     *
     * @return UUID oder {@code null}, wenn der Name unbekannt ist
     */
    public UUID uuidOf(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String trimmed = name.trim();

        for (Map.Entry<UUID, String> entry : hasJoinedTable.entrySet()) {
            if (trimmed.equalsIgnoreCase(entry.getValue())) {
                return entry.getKey();
            }
        }

        Player online = Bukkit.getPlayerExact(trimmed);

        if (online != null) {
            // Nach einem Reload koennen Spieler online sein, ohne dass sie in
            // dieser Server-Session bereits erfasst wurden.
            if (!hasJoinedTable.containsKey(online.getUniqueId())) {
                markJoined(online.getUniqueId(), online.getName());
            }

            return online.getUniqueId();
        }

        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(trimmed);

        if (cached != null) {
            // Der lokale Mojang-Cache enthaelt nur Spieler, die den Server
            // bereits betreten haben. Damit ist ein Ban auch direkt nach der
            // Integration moeglich, ohne dass der Spieler erneut joinen muss.
            if (!hasJoinedTable.containsKey(cached.getUniqueId())) {
                markJoined(cached.getUniqueId(), trimmed);
            }

            return cached.getUniqueId();
        }

        try {
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public void markJoined(UUID uuid, String name) {
        if (uuid == null || name == null || name.isBlank()) {
            return;
        }

        String previous = hasJoinedTable.get(uuid);

        if (name.equals(previous)) {
            return;
        }

        hasJoinedTable.put(uuid, name);
        hasJoinedTable.flushAsync();
    }

    public String nameOf(UUID uuid) {
        if (uuid == null) {
            return "Unbekannt";
        }

        String name = hasJoinedTable.get(uuid);

        if (name != null && !name.isBlank()) {
            return name;
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String offlineName = player.getName();

        if (offlineName != null && !offlineName.isBlank()) {
            return offlineName;
        }

        return uuid.toString();
    }

    public List<String> joinedNames() {
        List<String> names = new ArrayList<>(hasJoinedTable.values());

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!names.contains(player.getName())) {
                names.add(player.getName());
            }
        }

        return names;
    }

    // ---------------------------------------------------------------------
    // Freigabe und Webhook
    // ---------------------------------------------------------------------

    /** Konsole immer, Spieler ab der konfigurierten Rolle. */
    public boolean canModerate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        return server.permissions().hasRole(player, minimumRole);
    }

    public BetterBanManager minimumRole(Role role) {
        this.minimumRole = role == null ? Role.SUPPORTER_PLUS : role;
        return this;
    }

    public Role minimumRole() {
        return minimumRole;
    }

    public String executorName(CommandSender sender) {
        return sender instanceof Player player ? player.getName() : "Konsole";
    }

    /** Setzt den Discord-Webhook; {@code null}/leer deaktiviert den Versand. */
    public BetterBanManager webhook(String webhookUrl) {
        this.webhookUrl = webhookUrl == null || webhookUrl.isBlank() ? null : webhookUrl;
        return this;
    }

    public String webhook() {
        return webhookUrl;
    }

    public boolean hasWebhook() {
        return webhookUrl != null;
    }

    public void sendBanWebhook(String player, String executor, String duration, String reason, boolean permanent) {
        StringBuilder description = new StringBuilder();
        description.append(permanent ? "**Permanenter Ban**" : "**Temporaerer Ban**").append('\n');
        description.append("**Spieler:** ").append(player).append('\n');
        description.append("**Von:** ").append(executor);

        if (!permanent && duration != null) {
            description.append('\n').append("**Dauer:** ").append(duration);
        }

        appendReason(description, reason);
        sendWebhook(description.toString(), 0xFF0000);
    }

    public void sendUnbanWebhook(String player, String executor) {
        sendWebhook("**Entbannt**\n**Spieler:** " + player + "\n**Von:** " + executor, 0x00FF00);
    }

    public void sendKickWebhook(String player, String executor, String reason) {
        StringBuilder description = new StringBuilder();
        description.append("**Kick**").append('\n');
        description.append("**Spieler:** ").append(player).append('\n');
        description.append("**Von:** ").append(executor);
        appendReason(description, reason);
        sendWebhook(description.toString(), 0xFFA500);
    }

    public void sendChatBanWebhook(String player, String executor, String duration, String reason, boolean permanent) {
        StringBuilder description = new StringBuilder();
        description.append(permanent ? "**Permanenter Chat-Ban**" : "**Temporaerer Chat-Ban**").append('\n');
        description.append("**Spieler:** ").append(player).append('\n');
        description.append("**Von:** ").append(executor);

        if (!permanent && duration != null) {
            description.append('\n').append("**Dauer:** ").append(duration);
        }

        appendReason(description, reason);
        sendWebhook(description.toString(), 0xFFA500);
    }

    public void sendChatUnbanWebhook(String player, String executor) {
        sendWebhook("**Chat-Entbannung**\n**Spieler:** " + player + "\n**Von:** " + executor, 0x00FF00);
    }

    public void sendVoiceBanWebhook(String player, String executor, String duration, String reason, boolean permanent) {
        StringBuilder description = new StringBuilder();
        description.append(permanent ? "**Permanenter Voice-Ban**" : "**Temporaerer Voice-Ban**").append('\n');
        description.append("**Spieler:** ").append(player).append('\n');
        description.append("**Von:** ").append(executor);

        if (!permanent && duration != null) {
            description.append('\n').append("**Dauer:** ").append(duration);
        }

        appendReason(description, reason);
        sendWebhook(description.toString(), 0xFFA500);
    }

    public void sendVoiceUnbanWebhook(String player, String executor) {
        sendWebhook("**Voice-Entbannung**\n**Spieler:** " + player + "\n**Von:** " + executor, 0x00FF00);
    }

    private static void appendReason(StringBuilder description, String reason) {
        if (reason != null && !reason.isBlank()) {
            description.append('\n').append("**Grund:** ").append(reason);
        }
    }

    private void sendWebhook(String description, int color) {
        if (webhookUrl == null || description == null || description.isBlank()) {
            return;
        }

        String json = "{\"embeds\":[{\"description\":\""
                + escapeJson(description)
                + "\",\"color\":" + color + "}]}";

        server.scheduler().runAsync(() -> post(json));
    }

    private void post(String json) {
        try {
            HttpURLConnection connection = (HttpURLConnection) java.net.URI.create(webhookUrl)
                    .toURL()
                    .openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(5_000);

            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }

            connection.getResponseCode();
            connection.disconnect();
        } catch (Exception exception) {
            server.plugin().getLogger().warning("BetterBan-Webhook fehlgeschlagen: " + exception.getMessage());
        }
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ---------------------------------------------------------------------
    // Interne Helfer
    // ---------------------------------------------------------------------

    private BanEntry active(AbstractBanTable table, UUID uuid) {
        if (uuid == null) {
            return null;
        }

        BanEntry entry = table.get(uuid);

        if (entry != null && entry.expired(System.currentTimeMillis())) {
            table.remove(uuid);
            table.flushAsync();
            return null;
        }

        return entry;
    }

    private Map<UUID, BanEntry> activeEntries(AbstractBanTable table) {
        long now = System.currentTimeMillis();
        Map<UUID, BanEntry> result = new LinkedHashMap<>();

        for (Map.Entry<UUID, BanEntry> entry : table.entrySet()) {
            if (!entry.getValue().expired(now)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    private void clearExpired(AbstractBanTable table) {
        long now = System.currentTimeMillis();
        boolean changed = false;

        for (UUID key : table.keys()) {
            BanEntry entry = table.get(key);

            if (entry != null && entry.expired(now)) {
                table.remove(key);
                changed = true;
            }
        }

        if (changed) {
            table.flushNow();
        }
    }

    private void flushAll() {
        if (!tablesRegistered) {
            return;
        }

        banTable.flushNow();
        chatBanTable.flushNow();
        voiceBanTable.flushNow();
        hasJoinedTable.flushNow();
    }
}
