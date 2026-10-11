package de.jakomi1.project.report;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Verwaltet Spieler-Meldungen ueber {@code /report}.
 *
 * <p>Der Befehl wird erst mit {@link #enable()} registriert. Kategorien,
 * Cooldown und ein optionaler Discord-Webhook sind konfigurierbar. Ohne
 * Webhook bleibt eine Meldung rein lokal - der Spieler erhaelt trotzdem eine
 * Fall-Nummer, die hochgezaehlt wird.
 *
 * <p>Der Manager ist opt-in ({@link #defaultState()} ist {@code false}):
 * Eine Library-Nutzung ohne Meldungen registriert keinen Befehl.
 */
public final class ReportManager implements Manager {

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "Griefing",
            "Hacking",
            "Bug",
            "Beleidigung",
            "Sonstiges"
    );

    private static final long DEFAULT_COOLDOWN_MILLIS = 5L * 60L * 1000L;

    private final ProjectServer server;
    private final AtomicInteger caseCounter = new AtomicInteger(0);
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    private List<String> categories = DEFAULT_CATEGORIES;
    private long cooldownMillis = DEFAULT_COOLDOWN_MILLIS;
    private String webhookUrl;

    private ReportCommand command;
    private boolean enabled;

    public ReportManager(ProjectServer server) {
        this.server = server;
    }

    @Override
    public ReportManager enable() {
        if (enabled) {
            return this;
        }

        enabled = true;

        command = new ReportCommand(this);
        command.register(server.plugin());

        return this;
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }

        enabled = false;
        command = null;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean defaultState() {
        return false;
    }

    public ProjectServer server() {
        return server;
    }

    public ReportManager categories(List<String> categories) {
        this.categories = categories == null || categories.isEmpty()
                ? DEFAULT_CATEGORIES
                : List.copyOf(categories);
        return this;
    }

    public ReportManager categories(String... categories) {
        return categories(categories == null ? null : List.of(categories));
    }

    public List<String> categories() {
        return categories;
    }

    public ReportManager cooldownMillis(long cooldownMillis) {
        this.cooldownMillis = Math.max(cooldownMillis, 0L);
        return this;
    }

    public long cooldownMillis() {
        return cooldownMillis;
    }

    /** Setzt den Discord-Webhook; {@code null}/leer deaktiviert den Versand. */
    public ReportManager webhook(String webhookUrl) {
        this.webhookUrl = webhookUrl == null || webhookUrl.isBlank() ? null : webhookUrl;
        return this;
    }

    public String webhook() {
        return webhookUrl;
    }

    public boolean hasWebhook() {
        return webhookUrl != null;
    }

    /** Liefert die Kategorie in der Schreibweise der Liste oder {@code null}. */
    public String resolveCategory(String input) {
        if (input == null) {
            return null;
        }

        for (String category : categories) {
            if (category.equalsIgnoreCase(input)) {
                return category;
            }
        }

        return null;
    }

    /** Verbleibende Cooldown-Zeit in Millisekunden, {@code 0} wenn frei. */
    public long remainingCooldown(UUID uniqueId) {
        Long last = uniqueId == null ? null : cooldowns.get(uniqueId);

        if (last == null) {
            return 0L;
        }

        long remaining = cooldownMillis - (System.currentTimeMillis() - last);
        return Math.max(remaining, 0L);
    }

    public void record(UUID uniqueId) {
        if (uniqueId != null) {
            cooldowns.put(uniqueId, System.currentTimeMillis());
        }
    }

    public int nextCaseId() {
        return caseCounter.incrementAndGet();
    }

    /**
     * Verschickt eine Meldung und liefert die vergebene Fall-Nummer.
     */
    public int submit(String reporter, String category, String target, String text) {
        int caseId = nextCaseId();

        sendWebhook(caseId, reporter, category, target, text);

        return caseId;
    }

    private void sendWebhook(int caseId, String reporter, String category, String target, String text) {
        if (webhookUrl == null) {
            return;
        }

        StringBuilder description = new StringBuilder();
        description.append("**Reporter:** ").append(reporter).append("\n");
        description.append("**Kategorie:** ").append(category).append("\n");

        if (target != null && !target.isEmpty()) {
            description.append("**Ziel:** ").append(target).append("\n");
        }

        description.append("**Text:** ").append(text);

        String json = "{\"embeds\":[{\"title\":\"Report #" + caseId
                + "\",\"color\":16733525,\"description\":\""
                + escapeJson(description.toString()) + "\"}]}";

        sendWebhookRequest(webhookUrl, json);
    }

    private void sendWebhookRequest(String url, String json) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(bytes);
                }

                connection.getResponseCode();
                connection.disconnect();
            } catch (Exception exception) {
                server.plugin().getLogger().warning("Discord Webhook Fehler: " + exception.getMessage());
            }
        });
    }

    private static String escapeJson(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
