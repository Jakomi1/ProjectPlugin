package de.jakomi1.supabase;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import de.jakomi1.project.ProjectPlugin;
import de.jakomi1.project.scheduler.Scheduler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

public final class Supabase {

    public static final String BASE_URL = "https://juhmwedneayoqfetimvy.supabase.co";
    
    public static final String API_KEY = "sb_publishable_iU01w5tDCOETeYJDxGuzcA_X4b06ZET";

    private static final String REST_PATH = "/rest/v1/";
    private static final Gson GSON = new Gson();
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10_000;

    private final ProjectPlugin plugin;
    private final Scheduler scheduler;
    private final String baseUrl;

    public Supabase(ProjectPlugin plugin) {
        this(plugin, BASE_URL);
    }

    Supabase(ProjectPlugin plugin, String baseUrl) {
        this.plugin = plugin;
        this.scheduler = new Scheduler(plugin);
        this.baseUrl = baseUrl == null || baseUrl.isBlank()
                ? BASE_URL
                : baseUrl.replaceAll("/+$", "");
    }

    public JsonArray select(String table, String select, Map<String, String> filters) {
        if (!isConfigured() || table == null || table.isBlank()) return new JsonArray();

        try {
            String query = "select=" + encode(select == null || select.isBlank() ? "*" : select);
            query += filterQuery(filters);

            HttpURLConnection connection = open(table, query, "GET");
            int code = connection.getResponseCode();

            if (code != 200) {
                logError("select", table, code, readBody(connection.getErrorStream()));
                connection.disconnect();
                return new JsonArray();
            }

            String body = readBody(connection.getInputStream());
            connection.disconnect();

            JsonElement element = GSON.fromJson(body, JsonElement.class);
            return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
        } catch (Exception e) {
            logException("select", table, e);
            return new JsonArray();
        }
    }

    public JsonArray select(String table, String select) {
        return select(table, select, Map.of());
    }

    public void selectAsync(String table, String select, Map<String, String> filters, Consumer<JsonArray> callback) {
        if (!isConfigured() || callback == null) return;

        scheduler.runAsync(() -> {
            JsonArray result = select(table, select, filters);
            scheduler.runGlobal(() -> callback.accept(result));
        });
    }

    public void selectAsync(String table, String select, Consumer<JsonArray> callback) {
        selectAsync(table, select, Map.of(), callback);
    }

    public boolean isConfigured() {
        return !baseUrl.isBlank() && !API_KEY.isBlank();
    }

    private String filterQuery(Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) return "";

        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            query.append('&')
                    .append(encode(entry.getKey()))
                    .append("=eq.")
                    .append(encode(entry.getValue()));
        }
        return query.toString();
    }

    private HttpURLConnection open(String table, String query, String method) throws Exception {
        String url = baseUrl + REST_PATH + encodePath(table);
        if (query != null && !query.isEmpty()) {
            url += "?" + query;
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("apikey", API_KEY);
        connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    private void logError(String operation, String table, int code, String body) {
        plugin.getLogger().warning(
                "Supabase " + operation + " auf '" + table + "' fehlgeschlagen (HTTP " + code + "): " + body
        );
    }

    private void logException(String operation, String table, Exception e) {
        plugin.getLogger().warning(
                "Supabase " + operation + " auf '" + table + "' fehlgeschlagen: " + e.getMessage()
        );
    }

    private static String readBody(InputStream stream) {
        if (stream == null) return "";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
