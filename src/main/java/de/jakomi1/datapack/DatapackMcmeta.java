package de.jakomi1.datapack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.jakomi1.util.PackFormat;

public final class DatapackMcmeta {

    private DatapackMcmeta() {
    }

    public static JsonObject create(String description) {
        int format = PackFormat.current();

        JsonObject pack = new JsonObject();
        pack.addProperty("description", description);

        JsonArray minFormat = new JsonArray();
        minFormat.add(format);
        minFormat.add(1);
        pack.add("min_format", minFormat);

        JsonArray maxFormat = new JsonArray();
        maxFormat.add(format);
        maxFormat.add(1);
        pack.add("max_format", maxFormat);

        JsonObject root = new JsonObject();
        root.add("pack", pack);
        return root;
    }
}
