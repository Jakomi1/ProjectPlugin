package de.jakomi1.datapack;

import io.papermc.paper.datapack.Datapack;
import io.papermc.paper.datapack.DatapackManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ManagedDatapack {

    private static final String MANIFEST = ".projectplugin-datapack.sha256";

    private final Plugin plugin;
    private final String packName;

    public ManagedDatapack(Plugin plugin, String packName) {
        this.plugin = plugin;
        this.packName = packName;
    }

    public Result update(DatapackContentWriter writer, Path worldFolder) throws IOException {
        if (writer == null) {
            throw new IllegalArgumentException("writer darf nicht null sein.");
        }
        if (worldFolder == null) {
            throw new IllegalArgumentException("worldFolder darf nicht null sein.");
        }

        Path pluginCopy = pluginCopy();
        Path staging = sibling(pluginCopy, ".staging");

        deleteRecursively(staging);
        Files.createDirectories(staging);
        writer.write(staging);

        String hash = hashDirectory(staging);
        boolean pluginUpdated = !hash.equals(readManifest(pluginCopy));
        writeManifest(staging, hash);

        if (pluginUpdated) {
            replaceDirectory(pluginCopy, staging);
        } else {
            deleteRecursively(staging);
        }

        Path worldCopy = worldFolder.resolve("datapacks").resolve(packName);
        boolean worldUpdated = syncWorldCopy(pluginCopy, worldCopy, hash);

        return new Result(pluginCopy, worldCopy, hash, pluginUpdated, worldUpdated);
    }

    public Path pluginCopy() {
        return plugin.getDataFolder().toPath()
                .resolve("datapacks")
                .resolve(packName);
    }

    public void apply(Result result, String updatedLogMessage, String currentLogMessage) {
        DatapackManager manager;
        try {
            manager = Bukkit.getServer().getDatapackManager();
            manager.refreshPacks();
        } catch (Throwable t) {
            plugin.getLogger().warning("Datapack '" + packName + "': DatapackManager nicht verfügbar: " + t.getMessage());
            return;
        }

        boolean needsReload = result.changed();
        boolean reloadAlreadyScheduled = false;

        try {
            Datapack pack = findPack(manager);
            if (pack != null) {
                boolean wasEnabled = pack.isEnabled();
                if (!wasEnabled) {
                    needsReload = true;
                }
                // setEnabled löst selbst einen Ressourcen-Reload aus, wenn sich der Zustand ändert.
                pack.setEnabled(true);
                reloadAlreadyScheduled = !wasEnabled;
            } else {
                plugin.getLogger().warning("Datapack '" + packName + "' wurde nach refreshPacks() nicht gefunden.");
                needsReload = true;
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Datapack '" + packName + "' konnte nicht aktiviert werden: " + t.getMessage());
            needsReload = true;
        }

        if (needsReload) {
            if (!reloadAlreadyScheduled) {
                reloadResources();
            }
            plugin.getLogger().info(updatedLogMessage);
        } else {
            plugin.getLogger().info(currentLogMessage);
        }
    }

    private Datapack findPack(DatapackManager manager) {
        String prefixed = "file/" + packName;

        Datapack pack = manager.getPack(packName);
        if (pack != null) {
            return pack;
        }

        pack = manager.getPack(prefixed);
        if (pack != null) {
            return pack;
        }

        for (Datapack candidate : manager.getPacks()) {
            String name = candidate.getName();
            if (name.equals(packName) || name.equals(prefixed)) {
                return candidate;
            }
        }

        return null;
    }

    private void reloadResources() {
        try {
            Object server = minecraftServer();
            Method method = reloadResourcesMethod(server.getClass());
            if (method == null) {
                plugin.getLogger().warning("Datapack '" + packName + "': MinecraftServer.reloadResources nicht gefunden, Reload übersprungen.");
                return;
            }

            Object repository = invokeMethod(server, "getPackRepository");
            Collection<?> selected = (Collection<?>) invokeMethod(repository, "getSelectedIds");

            Object result;
            if (method.getParameterCount() == 2) {
                result = method.invoke(server, selected, pluginCause());
            } else {
                result = method.invoke(server, selected);
            }

            if (result instanceof CompletableFuture<?> future) {
                future.exceptionally(throwable -> {
                    plugin.getLogger().warning("Datapack '" + packName + "': Reload fehlgeschlagen: " + throwable.getMessage());
                    return null;
                });
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Datapack '" + packName + "': Reload fehlgeschlagen: " + t.getMessage());
        }
    }

    private static Object minecraftServer() throws Exception {
        return Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
    }

    private static Method reloadResourcesMethod(Class<?> serverClass) {
        Method fallback = null;

        for (Method method : serverClass.getMethods()) {
            if (!method.getName().equals("reloadResources")) {
                continue;
            }

            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 1 && Collection.class.isAssignableFrom(parameters[0])) {
                fallback = method;
            }
            if (parameters.length == 2 && Collection.class.isAssignableFrom(parameters[0])) {
                return method;
            }
        }

        return fallback;
    }

    private static Object pluginCause() throws Exception {
        Class<?> causeClass = Class.forName("io.papermc.paper.event.server.ServerResourcesReloadedEvent$Cause");
        return causeClass.getMethod("valueOf", String.class).invoke(null, "PLUGIN");
    }

    private static Object invokeMethod(Object target, String name) throws Exception {
        return target.getClass().getMethod(name).invoke(target);
    }

    private boolean syncWorldCopy(Path pluginCopy, Path worldCopy, String hash) throws IOException {
        if (hash.equals(readManifest(worldCopy))) {
            return false;
        }

        Path staging = sibling(worldCopy, ".staging");
        deleteRecursively(staging);
        copyDirectory(pluginCopy, staging);
        replaceDirectory(worldCopy, staging);
        return true;
    }

    private static void replaceDirectory(Path target, Path source) throws IOException {
        Files.createDirectories(target.getParent());

        Path backup = sibling(target, ".old");
        deleteRecursively(backup);

        if (Files.exists(target)) {
            move(target, backup);
        }

        try {
            move(source, target);
        } catch (IOException e) {
            if (Files.exists(backup) && !Files.exists(target)) {
                move(backup, target);
            }
            throw e;
        }

        deleteRecursively(backup);
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);

        try (var stream = Files.walk(source)) {
            stream.forEach(path -> {
                try {
                    Path relative = source.relativize(path);
                    Path destination = target.resolve(relative);

                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destination);
                    } else {
                        Files.createDirectories(destination.getParent());
                        Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String hashDirectory(Path root) throws IOException {
        MessageDigest digest = sha256();
        List<Path> files = new ArrayList<>();

        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().equals(MANIFEST))
                    .sorted(Comparator.comparing(path -> root.relativize(path).toString()))
                    .forEach(files::add);
        }

        for (Path file : files) {
            String relative = root.relativize(file).toString().replace('\\', '/');
            digest.update(relative.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(Files.readAllBytes(file));
            digest.update((byte) 0);
        }

        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 ist nicht verfügbar.", e);
        }
    }

    private static String readManifest(Path root) throws IOException {
        Path manifest = root.resolve(MANIFEST);
        if (!Files.isRegularFile(manifest)) {
            return null;
        }

        return Files.readString(manifest, StandardCharsets.UTF_8).trim();
    }

    private static void writeManifest(Path root, String hash) throws IOException {
        Files.writeString(root.resolve(MANIFEST), hash + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static Path sibling(Path path, String suffix) {
        return path.resolveSibling(path.getFileName() + suffix);
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path)) return;

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public record Result(Path pluginCopy, Path worldCopy, String hash, boolean pluginUpdated, boolean worldUpdated) {

        public boolean changed() {
            return pluginUpdated || worldUpdated;
        }
    }
}
