package de.jakomi1.util;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.Random;

public final class SoundUtils {

    private static final Random RANDOM = new Random();

    private static final double DEFAULT_NEARBY_RADIUS = 12.0;

    private SoundUtils() {
    }

    public static void playOpenEnchanterSound(Player player) {
        playSound(player, Sound.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, 1.0f, 1.0f);
    }

    public static void playChallengeCompleteSound(Player player) {
        playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    public static void playCloseEnchanterSound(Player player) {
        playSound(player, Sound.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, 1.0f, 0.6f);
    }

    public static void playBasicGuiSound(Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1.0f, 1.0f);
    }

    public static void playVillagerSound(Player player) {
        playSound(player, Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f);
    }

    public static void playVillagerNoSound(Player player) {
        playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    public static void playSelectionSound(Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 2.0f);
    }

    public static void playClickSound(Player player) {
        playSound(player, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    public static void playSuccessSound(Player player) {
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    public static void playErrorSound(Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
    }

    public static void playLevelUpSound(Player player) {
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    public static void playNearbySound(Player player, Sound sound) {
        playNearbySound(player, sound, DEFAULT_NEARBY_RADIUS, 1.0f, 1.0f);
    }

    public static void playNearbySound(Player player, Sound sound, double radius, float volume, float pitch) {
        if (player == null || sound == null || radius <= 0.0) return;

        Location location = player.getLocation();
        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.getLocation().distanceSquared(location) <= radius * radius) {
                nearby.playSound(nearby.getLocation(), sound, SoundCategory.PLAYERS, volume, pitch);
            }
        }
    }

    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        if (player == null || sound == null) return;

        player.playSound(player.getLocation(), sound, SoundCategory.PLAYERS, volume, pitch);
    }

    public static int getRandomInt(int bound) {
        return bound <= 0 ? 0 : RANDOM.nextInt(bound);
    }
}
