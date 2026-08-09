package de.jakomi1.util;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;

/**
 * Ermittelt das aktuelle Data-Pack-Format der laufenden Serverversion.
 * <p>
 * Die Datapack-Formatnummer hat sich von Version zu Version geändert (z.B.
 * 61 in 1.20.5/1.20.6, 107 in 1.21.5, ...). Statt einen festen Wert zu
 * schreiben, wird die Nummer zur Laufzeit aus der Server-API abgeleitet und
 * nur bei einem Fehler auf einen bekannten Fallback zurückgegriffen.
 */
public final class PackFormat {

    private static final int FALLBACK = 107;

    private PackFormat() {
    }

    public static int current() {
        Integer fromApi = fromNms();
        if (fromApi != null) {
            return fromApi;
        }

        Integer fromVersion = fromVersionString(Bukkit.getBukkitVersion());
        return fromVersion != null ? fromVersion : FALLBACK;
    }

    private static Integer fromNms() {
        try {
            Class<?> sharedConstants = Class.forName("net.minecraft.SharedConstants");
            Object version = sharedConstants.getMethod("getCurrentVersion").invoke(null);

            if (version == null) {
                return null;
            }

            Method packVersion = version.getClass().getMethod("getPackVersion");
            return (Integer) packVersion.invoke(version);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Integer fromVersionString(String bukkitVersion) {
        if (bukkitVersion == null) {
            return null;
        }

        int dash = bukkitVersion.indexOf('-');
        String mcVersion = dash > 0 ? bukkitVersion.substring(0, dash) : bukkitVersion;

        return switch (mcVersion) {
            case "1.20.5", "1.20.6" -> 61;
            case "1.21", "1.21.1" -> 70;
            case "1.21.2", "1.21.3" -> 81;
            case "1.21.4" -> 76;
            case "1.21.5" -> 107;
            case "1.21.6" -> 117;
            case "1.21.7" -> 134;
            default -> null;
        };
    }
}
