package de.jakomi1.datapack;

import io.papermc.paper.datapack.Datapack;
import io.papermc.paper.datapack.DatapackManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

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

        replaceDirectory(target, staging);
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

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
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
