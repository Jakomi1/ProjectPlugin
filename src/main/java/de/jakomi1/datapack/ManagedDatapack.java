package de.jakomi1.datapack;

import io.papermc.paper.datapack.Datapack;
import io.papermc.paper.datapack.DatapackManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ManagedDatapack {

    private final Plugin plugin;
    private final String packName;

    public ManagedDatapack(Plugin plugin, String packName) {
        this.plugin = plugin;
        this.packName = packName;
    }

    public Path update(DatapackContentWriter writer, Path worldFolder) throws IOException {
        if (writer == null) {
            throw new IllegalArgumentException("writer darf nicht null sein.");
        }
        if (worldFolder == null) {
            throw new IllegalArgumentException("worldFolder darf nicht null sein.");
        }

        Path target = worldRoot(worldFolder).resolve("datapacks").resolve(packName);
        Path staging = sibling(target, ".staging");

        deleteRecursively(staging);
        Files.createDirectories(staging);
        writer.write(staging);

        syncDirectory(target, staging);
        deleteRecursively(staging);
        return target;
    }

    public void load() {
        try {
            DatapackManager manager = Bukkit.getServer().getDatapackManager();
            manager.refreshPacks();

            Datapack pack = manager.getPack("file/" + packName);
            if (pack == null) {
                for (Datapack candidate : manager.getPacks()) {
                    if (candidate.getName().equals(packName) || candidate.getName().equals("file/" + packName)) {
                        pack = candidate;
                        break;
                    }
                }
            }

            if (pack == null) {
                plugin.getLogger().warning("Datapack '" + packName + "' wurde nach refreshPacks() nicht gefunden.");
                return;
            }

            if (!pack.isEnabled()) {
                pack.setEnabled(true);
                plugin.getLogger().info("Datapack '" + packName + "' wurde geladen.");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Datapack '" + packName + "' konnte nicht geladen werden: " + t.getMessage());
        }
    }

    private void syncDirectory(Path target, Path source) throws IOException {
        Files.createDirectories(target);

        Set<Path> expected = new HashSet<>();

        try (var stream = Files.walk(source)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (!Files.isRegularFile(path)) continue;

                Path relative = source.relativize(path);
                expected.add(relative);
                Path destination = target.resolve(relative);

                if (sameContent(path, destination)) {
                    continue;
                }

                Files.createDirectories(destination.getParent());
                try {
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("Datapack '" + packName + "': " + pathName(relative) + " aktualisiert.");
                } catch (IOException e) {
                    plugin.getLogger().warning("Datapack '" + packName + "': " + pathName(relative) + " konnte nicht aktualisiert werden: " + e.getMessage());
                }
            }
        }

        try (var stream = Files.walk(target)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (!Files.isRegularFile(path)) continue;

                Path relative = target.relativize(path);
                if (expected.contains(relative)) continue;

                try {
                    Files.deleteIfExists(path);
                    plugin.getLogger().info("Datapack '" + packName + "': " + pathName(relative) + " entfernt.");
                } catch (IOException e) {
                    plugin.getLogger().warning("Datapack '" + packName + "': " + pathName(relative) + " konnte nicht entfernt werden: " + e.getMessage());
                }
            }
        }

        try (var stream = Files.walk(target)) {
            List<Path> directories = stream.filter(Files::isDirectory)
                    .sorted(Comparator.reverseOrder())
                    .toList();
            for (Path directory : directories) {
                if (directory.equals(target)) continue;
                try (var entries = Files.list(directory)) {
                    if (!entries.findFirst().isPresent()) {
                        try {
                            Files.deleteIfExists(directory);
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }
    }

    private static boolean sameContent(Path source, Path target) throws IOException {
        if (!Files.isRegularFile(target)) return false;
        if (Files.size(source) != Files.size(target)) return false;

        try (InputStream inSource = Files.newInputStream(source);
             InputStream inTarget = Files.newInputStream(target)) {
            byte[] sourceBuffer = new byte[8192];
            byte[] targetBuffer = new byte[8192];
            while (true) {
                int sourceRead = inSource.read(sourceBuffer);
                int targetRead = inTarget.read(targetBuffer);
                if (sourceRead != targetRead) return false;
                if (sourceRead < 0) return true;
                if (!Arrays.equals(sourceBuffer, 0, sourceRead, targetBuffer, 0, targetRead)) return false;
            }
        }
    }

    private static String pathName(Path relative) {
        return relative.toString().replace('\\', '/');
    }

    private static Path worldRoot(Path folder) {
        Path current = folder.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("level.dat"))) {
                return current;
            }
            current = current.getParent();
        }
        return folder;
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
}
