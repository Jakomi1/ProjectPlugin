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

    public static void playBreakSound(Player player) {
        playSound(player, Sound.BLOCK_VAULT_BREAK, 1.0f, 1);
    }

    public static void playCrushSound(Player player) {
        playSound(player, Sound.UI_STONECUTTER_TAKE_RESULT, 1.0f, 1f);
    }

    public static void playPlingSound(Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1f);
    }

    public static void playPingSound(Player player) {
        playPingSound(player, 1f);
    }

    public static void playPingSound(Player player, float pitch) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, pitch);
    }

    public static void playMagierSound(Player player) {
        playSound(player, Sound.ENTITY_ILLUSIONER_AMBIENT, 1.0f, 1f);
    }

    public static void playSaturationSound(Player player) {
        playSound(player, Sound.ENTITY_PLAYER_BURP, 1.0f, 1f);
    }

    public static void playBasicWarpSound(Player player) {
        playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    public static void playBasicBundleSound(Player player) {
        playSound(player, Sound.ITEM_BUNDLE_DROP_CONTENTS, 1.0f, 1.0f);
    }

    public static void playBasicTPSound(Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1.0f, 1.0f);
    }

    public static void playCancelSound(Player player) {
        playSound(player, Sound.ENTITY_ENDERMAN_HURT, 1.0f, 0.8f);
    }

    public static void playStrongCancelSound(Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 2.0f, 0.5f);
    }

    public static void playDeathSound(Player player) {
        playSound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.8f);
    }

    public static void playBoostSound(Player player) {
        playSound(player, Sound.ENTITY_BREEZE_WIND_BURST, 1.0f, 1f);
    }

    public static void playOpenSound(Player player) {
        playSound(player, Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1f);
    }

    public static void playBurnSoundAmbient(Location location) {
        playSoundForNearbyPlayers(location, Sound.ENTITY_GENERIC_BURN, 2.0f, 1f);
    }

    public static void playCloseSound(Player player) {
        playSound(player, Sound.BLOCK_ENDER_CHEST_CLOSE, 1.0f, 1f);
    }

    public static void playPlopSound(Player player) {
        playSound(player, Sound.BLOCK_SNIFFER_EGG_PLOP, 1.0f, (float) getRandomInt(7, 12) / 10);
    }

    public static int getRandomInt(int start, int end) {
        return start + RANDOM.nextInt(end - start + 1);
    }

    public static void playSoundForNearbyPlayers(Location loc, Sound sound, float volume, float pitch, double radius) {
        if (loc == null || sound == null) return;
        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(loc) <= radius * radius) {
                player.playSound(loc, sound, SoundCategory.MASTER, volume, pitch);
            }
        }
    }

    public static void playSoundForNearbyPlayers(Location loc, Sound sound, float volume, float pitch) {
        playSoundForNearbyPlayers(loc, sound, volume, pitch, 12.0);
    }
}
