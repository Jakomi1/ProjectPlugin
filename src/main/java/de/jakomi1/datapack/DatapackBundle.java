package de.jakomi1.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Fasst mehrere {@link DatapackContentWriter} zu einem einzigen Datapack
 * zusammen.
 *
 * <p>Die einzelnen Writer duerfen keine eigene {@code pack.mcmeta} mehr
 * schreiben; diese wird zentral vom Bundle erzeugt. So koennen Biome,
 * Dimensionen und Enchantments in einem gemeinsamen Pack liegen, statt
 * mehrere konkurrierende Packs zu erzeugen.</p>
 */
public final class DatapackBundle implements DatapackContentWriter {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final String description;
    private final List<DatapackContentWriter> contents = new ArrayList<>();

    public DatapackBundle(String description) {
        this.description = Objects.requireNonNull(description, "description");
    }

    public String description() {
        return description;
    }

    public DatapackBundle add(DatapackContentWriter content) {
        contents.add(Objects.requireNonNull(content, "content"));
        return this;
    }

    public DatapackBundle addAll(Collection<? extends DatapackContentWriter> contents) {
        if (contents != null) {
            contents.forEach(this::add);
        }
        return this;
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    public int size() {
        return contents.size();
    }

    @Override
    public void write(Path packRoot) throws IOException {
        Objects.requireNonNull(packRoot, "packRoot");

        Files.createDirectories(packRoot);
        writeJson(packRoot.resolve("pack.mcmeta"), DatapackMcmeta.create(description));

        for (DatapackContentWriter content : contents) {
            content.write(packRoot);
        }
    }

    private static void writeJson(Path file, JsonObject json) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(
                file,
                GSON.toJson(json),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }
}
