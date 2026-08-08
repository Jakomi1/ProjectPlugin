/*
 * InvseeSession – Port aus dem Plugin "Invsee".
 * Original: at.noahb.invsee (Autor: MCmitNoah), GNU General Public License v3.0 (GPL-3.0).
 * Siehe LICENSE.md in diesem Repository.
 *
 * Geändert von Jakomi1 (08.08.2026) für ProjectPlugin:
 *  - an die Library-Architektur (ProjectServer/Manager) angepasst,
 *  - auf Paper 26.2 aktualisiert,
 *  - Guava CacheBuilder durch PlayerCache, MaterialTags durch ItemCategories ersetzt.
 */
package de.jakomi1.project.invsee.session;

import de.jakomi1.project.invsee.ItemCategories;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

/**
 * Eine Invsee-Session: zeigt ein 45-Slot-Inventar (36 Hotbar/Main + 4 Rüstung
 * + Offhand + Cursor + 3 Leerplätze) des beobachteten Spielers.
 */
public class InvseeSession implements Session {

    private final Plugin plugin;
    private final UUID uuid;
    private final Set<UUID> subscribers;
    private final Inventory inventory;
    private final ReentrantLock lock = new ReentrantLock();
    private final PlayerCache playerCache = new PlayerCache();

    public InvseeSession(Plugin plugin, OfflinePlayer offlinePlayer, UUID subscriber) {
        this.plugin = plugin;
        this.uuid = offlinePlayer.getUniqueId();
        this.subscribers = new HashSet<>();

        if (offlinePlayer instanceof Player player) {
            this.inventory = Bukkit.createInventory(player, 45, player.name().append(text("'s Inventar")));
        } else {
            String name = offlinePlayer.getName() == null ? "Unbekannt" : offlinePlayer.getName();
            this.inventory = Bukkit.getServer().createInventory(null, 45, text(name).append(text("'s Inventar")));
        }

        updateSubscriberInventory();
        addSubscriber(subscriber);
    }

    @Override
    public Plugin plugin() {
        return this.plugin;
    }

    @Override
    public UUID getUniqueIdOfObservedPlayer() {
        return this.uuid;
    }

    @Override
    public Set<UUID> getSubscribers() {
        return this.subscribers;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    private PlayerInventory getPlayerInventory(OfflinePlayer offlinePlayer) {
        if (offlinePlayer instanceof Player player) {
            return player.getInventory();
        }

        Optional<Player> player = getPlayerOffline(offlinePlayer);

        return player.map(Player::getInventory).orElse(null);
    }

    @Override
    public void removeSubscriber(UUID subscriber) {
        this.subscribers.remove(subscriber);
    }

    @Override
    public void updateSubscriberInventory() {
        update(() -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(this.uuid);
            PlayerInventory playerInv = getPlayerInventory(offlinePlayer);

            if (playerInv == null) {
                return;
            }

            for (int i = 0; i < 41; i++) {
                this.inventory.setItem(i, playerInv.getItem(i));
            }
            if (offlinePlayer instanceof Player player) {
                this.inventory.setItem(41, player.getItemOnCursor());
            }
            replaceEmptyPlaceholderSpots();
        });
    }

    private void replaceEmptyPlaceholderSpots() {
        if (this.inventory.getItem(36) == null) this.inventory.setItem(36, Placeholders.BOOTS);
        if (this.inventory.getItem(37) == null) this.inventory.setItem(37, Placeholders.LEGGINGS);
        if (this.inventory.getItem(38) == null) this.inventory.setItem(38, Placeholders.CHESTPLATE);
        if (this.inventory.getItem(39) == null) this.inventory.setItem(39, Placeholders.HELMET);
        if (this.inventory.getItem(40) == null) this.inventory.setItem(40, Placeholders.OFF_HAND);
        if (this.inventory.getItem(41) == null) this.inventory.setItem(41, Placeholders.CURSOR);
        for (int i = 42; i < 45; i++) {
            if (this.inventory.getItem(i) == null) this.inventory.setItem(i, Placeholders.NO_USAGE);
        }
    }

    @Override
    public boolean hasSubscriber(UUID uuid) {
        return this.subscribers.contains(uuid);
    }

    @Override
    public void updateObservedInventory() {
        update(() -> {
            OfflinePlayer offlinePlayer = Bukkit.getServer().getOfflinePlayer(uuid);
            PlayerInventory playerInventory = getPlayerInventory(offlinePlayer);
            if (playerInventory == null) {
                return;
            }
            for (int i = 0; i < playerInventory.getSize(); i++) {
                if (Placeholders.isPlaceholder(this.inventory.getItem(i))) continue;
                playerInventory.setItem(i, this.inventory.getItem(i));
            }

            if (!Placeholders.isPlaceholder(this.inventory.getItem(41)) && offlinePlayer instanceof Player player) {
                player.setItemOnCursor(this.inventory.getItem(41));
            }

            replaceEmptyPlaceholderSpots();
        });
    }

    @Override
    public ReentrantLock getLock() {
        return this.lock;
    }

    @Override
    public void cache(Player player) {
        this.playerCache.put(this.uuid, player);
    }

    @Override
    public Player getCachedPlayer() {
        return this.playerCache.getIfPresent(this.uuid);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        InvseeSession session = (InvseeSession) object;
        return Objects.equals(uuid, session.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    public enum ArmorSlot {
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS;

        public boolean checkIfItemFitsSlot(ItemStack itemStack) {
            return switch (this) {
                case HELMET -> ItemCategories.isHelmet(itemStack);
                case CHESTPLATE -> ItemCategories.isChestplate(itemStack);
                case LEGGINGS -> ItemCategories.isLeggings(itemStack);
                case BOOTS -> ItemCategories.isBoots(itemStack);
            };
        }
    }

    public static final class Placeholders {

        public static final NamespacedKey OFF_HAND_KEY = new NamespacedKey("invsee", "offhand");
        public static final NamespacedKey CURSOR_KEY = new NamespacedKey("invsee", "cursor");
        public static final NamespacedKey INVSEE_KEY = new NamespacedKey("invsee", "invsee");

        static final ItemStack HELMET = armorPlaceholder("Helmet slot");
        static final ItemStack CHESTPLATE = armorPlaceholder("Chestplate slot");
        static final ItemStack LEGGINGS = armorPlaceholder("Leggings slot");
        static final ItemStack BOOTS = armorPlaceholder("Boots slot");
        static final ItemStack OFF_HAND = barrierPlaceholder("Off Hand", OFF_HAND_KEY);
        static final ItemStack CURSOR = barrierPlaceholder("Cursor", CURSOR_KEY);
        static final ItemStack NO_USAGE = emptyPlaceholder();

        private Placeholders() {
        }

        private static ItemStack armorPlaceholder(String displayName) {
            return edit(new ItemStack(Material.RED_STAINED_GLASS_PANE), displayName);
        }

        private static ItemStack barrierPlaceholder(String displayName, NamespacedKey extraKey) {
            ItemStack itemStack = edit(new ItemStack(Material.BARRIER), displayName);
            itemStack.editMeta(itemMeta ->
                    itemMeta.getPersistentDataContainer().set(extraKey, PersistentDataType.BOOLEAN, true));
            return itemStack;
        }

        private static ItemStack emptyPlaceholder() {
            ItemStack itemStack = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            itemStack.editMeta(itemMeta -> {
                itemMeta.displayName(Component.empty());
                itemMeta.getPersistentDataContainer().set(INVSEE_KEY, PersistentDataType.BOOLEAN, true);
            });
            return itemStack;
        }

        private static ItemStack edit(ItemStack itemStack, String displayName) {
            itemStack.editMeta(itemMeta -> {
                itemMeta.displayName(text(displayName, GOLD).decoration(ITALIC, false));
                itemMeta.lore(java.util.List.of(text("empty", RED).decoration(ITALIC, false)));
                itemMeta.getPersistentDataContainer().set(INVSEE_KEY, PersistentDataType.BOOLEAN, true);
            });
            return itemStack;
        }

        public static boolean isOffHandPlaceholder(ItemStack itemStack) {
            return itemStack != null && itemStack.hasItemMeta()
                    && itemStack.getItemMeta().getPersistentDataContainer().has(OFF_HAND_KEY, PersistentDataType.BOOLEAN);
        }

        public static boolean isCursorPlaceholder(ItemStack itemStack) {
            return itemStack != null && itemStack.hasItemMeta()
                    && itemStack.getItemMeta().getPersistentDataContainer().has(CURSOR_KEY, PersistentDataType.BOOLEAN);
        }

        public static boolean isPlaceholder(ItemStack itemStack) {
            return itemStack != null && itemStack.hasItemMeta()
                    && itemStack.getItemMeta().getPersistentDataContainer().has(INVSEE_KEY, PersistentDataType.BOOLEAN);
        }
    }
}
