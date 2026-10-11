package de.jakomi1.project.combat;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import de.jakomi1.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Zentraler Kampf-Manager: Kampf-Tag, PvP-Schalter, Schadensregeln und
 * geschuetzte Zonen an einer Stelle.
 *
 * <p>Der Kampf-Tag ({@link #mark(Player)}) haelt fest, wer gerade kaempft, und
 * dient der Kampf-Log-Erkennung. Der PvP-Schalter entspricht der Mechanik aus
 * CrackedAttack: ein Tag {@code pvp} zeigt an, ob der Spieler kaempfen darf,
 * und bleibt ueber Neustarts erhalten. Die 20 Minuten sind keine Kampfsperre,
 * sondern eine Aenderungshinderung - wer PvP einschaltet oder kaempft, kann es
 * 20 Minuten lang nicht mehr abschalten.
 *
 * <p>Zonen und Texte bleiben dem Aufrufer ueberlassen:
 * {@link #safeZone(Predicate)} (kein Schaden),
 * {@link #protectedZone(Predicate)} (kein PvP) und
 * {@link #messages(BiFunction)}.
 */
public final class CombatManager implements Manager {

    private static final long DEFAULT_DURATION_MS = 20 * 60 * 1000L;
    private static final long CHECK_PERIOD_TICKS = 20L;

    private final ProjectServer server;
    private final Map<UUID, Long> combatUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> pvpUntil = new ConcurrentHashMap<>();

    private final CombatListener listener;
    private final PvpListener pvpListener;

    private boolean enabled;
    private boolean commandEnabled = true;
    private PvpCommand command;

    private long durationMs = DEFAULT_DURATION_MS;
    private long pvpLockMs = DEFAULT_DURATION_MS;

    private Scheduler.Task expiryTask;
    private Predicate<Player> combatLogBan = player -> false;

    private String pvpTag = "pvp";
    private Predicate<Location> protectedZone = location -> false;
    private Predicate<Location> safeZone = location -> false;
    private BiFunction<PvpMessage, Player, Component> messageResolver = this::defaultMessage;

    public CombatManager(ProjectServer server) {
        this.server = server;
        this.listener = new CombatListener(this);
        this.pvpListener = new PvpListener(this);
    }

    @Override
    public CombatManager enable() {
        if (enabled) return this;
        enabled = true;

        SelfOnlyExplosions.namespace(server.plugin().getId());

        listener.register(server.plugin());
        new PvpListener(this).register(server.plugin());
        new ExplosionGuardListener().register(server.plugin());

        if (commandEnabled) {
            new PvpCommand(this).register(server.plugin());
        }

        expiryTask = server.scheduler().runTimer(this::checkExpiry, CHECK_PERIOD_TICKS, CHECK_PERIOD_TICKS);
        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;

        listener.unregister();
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }
        endAll(CombatEndEvent.Reason.DISABLED);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public ProjectServer server() {
        return server;
    }

    // ------------------------------------------------------------------
    // Kampf-Tag
    // ------------------------------------------------------------------

    public CombatManager duration(Duration duration) {
        if (duration != null && !duration.isZero() && !duration.isNegative()) {
            this.durationMs = duration.toMillis();
        }
        return this;
    }

    public CombatManager durationMillis(long millis) {
        if (millis > 0) {
            this.durationMs = millis;
        }
        return this;
    }

    public long durationMillis() {
        return durationMs;
    }

    public CombatManager combatLogBan(Predicate<Player> predicate) {
        this.combatLogBan = predicate == null ? player -> false : predicate;
        return this;
    }

    public boolean combatLogBans(Player player) {
        return combatLogBan.test(player);
    }

    public CombatManager mark(Player player) {
        return mark(player, durationMs);
    }

    public CombatManager mark(Player player, long durationMillis) {
        if (player != null && durationMillis > 0) {
            combatUntil.put(player.getUniqueId(), System.currentTimeMillis() + durationMillis);
        }
        return this;
    }

    public CombatManager refresh(Player player) {
        if (player != null && isInCombat(player)) {
            mark(player);
        }
        return this;
    }

    public boolean isInCombat(Player player) {
        return player != null && isInCombat(player.getUniqueId());
    }

    public boolean isInCombat(UUID uuid) {
        if (uuid == null) return false;

        long until = combatUntil.getOrDefault(uuid, 0L);
        long remaining = until - System.currentTimeMillis();

        if (remaining <= 0) {
            if (until != 0L) combatUntil.remove(uuid);
            return false;
        }

        return true;
    }

    public long remainingMillis(Player player) {
        return remainingMillis(player == null ? null : player.getUniqueId());
    }

    public long remainingMillis(UUID uuid) {
        if (uuid == null) return 0L;

        long until = combatUntil.getOrDefault(uuid, 0L);
        long remaining = until - System.currentTimeMillis();

        if (remaining <= 0) {
            if (until != 0L) combatUntil.remove(uuid);
            return 0L;
        }

        return remaining;
    }

    public long expiresAt(UUID uuid) {
        return uuid == null ? 0L : combatUntil.getOrDefault(uuid, 0L);
    }

    public CombatManager end(Player player, CombatEndEvent.Reason reason) {
        if (player == null) return this;

        if (combatUntil.remove(player.getUniqueId()) != null) {
            Bukkit.getPluginManager().callEvent(new CombatEndEvent(player, reason));
        }
        return this;
    }

    public void endAll(CombatEndEvent.Reason reason) {
        for (UUID uuid : new ArrayList<>(combatUntil.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                end(player, reason);
            } else {
                combatUntil.remove(uuid);
            }
        }
    }

    public void handleQuit(Player player) {
        if (player == null || !enabled) return;
        if (!isInCombat(player)) return;

        combatUntil.remove(player.getUniqueId());
        Bukkit.getPluginManager().callEvent(new CombatEndEvent(player, CombatEndEvent.Reason.COMBAT_LOG));

        if (combatLogBan.test(player)) {
            dropInventory(player);
        }
    }

    private void dropInventory(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) return;

        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();
        ItemStack[] extra = player.getInventory().getExtraContents();

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[armor.length]);
        player.getInventory().setExtraContents(new ItemStack[extra.length]);

        server.scheduler().runRegion(location, () -> {
            dropAll(world, location, contents);
            dropAll(world, location, armor);
            dropAll(world, location, extra);
        });
    }

    private void dropAll(World world, Location location, ItemStack[] items) {
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;
            world.dropItemNaturally(location, item);
        }
    }

    public void clear() {
        combatUntil.clear();
    }

    private void checkExpiry() {
        long now = System.currentTimeMillis();

        for (UUID uuid : new ArrayList<>(combatUntil.keySet())) {
            if (combatUntil.get(uuid) > now) continue;

            Player player = Bukkit.getPlayer(uuid);
            combatUntil.remove(uuid);

            if (player != null) {
                Bukkit.getPluginManager().callEvent(new CombatEndEvent(player, CombatEndEvent.Reason.EXPIRED));
            }
        }
    }

    // ------------------------------------------------------------------
    // PvP-Schalter
    // ------------------------------------------------------------------

    /** Tag, das anzeigt, ob ein Spieler PvP aktiviert hat. */
    public CombatManager pvpTag(String tag) {
        if (tag != null && !tag.isBlank()) {
            this.pvpTag = tag;
        }
        return this;
    }

    public String pvpTag() {
        return pvpTag;
    }

    /** Dauer der Aenderungshinderung nach einem PvP-Kontakt. */
    public CombatManager pvpLock(Duration duration) {
        if (duration != null && !duration.isZero() && !duration.isNegative()) {
            this.pvpLockMs = duration.toMillis();
        }
        return this;
    }

    public long pvpLockMillis() {
        return pvpLockMs;
    }

    /** Steht der PvP-Schalter des Spielers auf an? */
    public boolean pvpEnabled(Player player) {
        return player != null && player.getScoreboardTags().contains(pvpTag);
    }

    /** Setzt das Tag und startet die Aenderungshinderung. */
    public CombatManager activatePvp(Player player) {
        if (player == null) return this;

        player.getScoreboardTags().add(pvpTag);
        markPvp(player);
        return this;
    }

    /** Nimmt das Tag und loescht eine laufende Aenderungshinderung. */
    public CombatManager deactivatePvp(Player player) {
        if (player == null) return this;

        player.getScoreboardTags().remove(pvpTag);
        clearPvp(player);
        return this;
    }

    /** Kehrt den PvP-Zustand um. */
    public boolean togglePvp(Player player) {
        if (player == null) return false;

        if (pvpEnabled(player)) {
            deactivatePvp(player);
            return false;
        }

        activatePvp(player);
        return true;
    }

    /**
     * Nach einem Tod wird PvP abgeschaltet und die Aenderungshinderung
     * geloescht.
     *
     * @return true, wenn PvP davor aktiv war
     */
    public boolean resetAfterDeath(Player player) {
        if (player == null) return false;

        boolean wasActive = player.getScoreboardTags().remove(pvpTag);
        clearPvp(player);

        return wasActive;
    }

    /** Setzt bzw. erneuert die Aenderungshinderung. */
    public CombatManager markPvp(Player player) {
        if (player == null) return this;

        long now = System.currentTimeMillis();
        pvpUntil.put(player.getUniqueId(), now + pvpLockMs);
        player.getPersistentDataContainer().set(lastPvpKey(), PersistentDataType.LONG_ARRAY, new long[]{now});

        return this;
    }

    /** Laeuft gerade die Aenderungshinderung? */
    public boolean pvpLocked(Player player) {
        return remainingPvpLock(player) > 0L;
    }

    /** Restzeit der Aenderungshinderung in Millisekunden, 0 wenn frei. */
    public long remainingPvpLock(Player player) {
        if (player == null) return 0L;

        UUID uuid = player.getUniqueId();
        Long until = pvpUntil.get(uuid);

        if (until == null) {
            long[] stored = player.getPersistentDataContainer().get(lastPvpKey(), PersistentDataType.LONG_ARRAY);
            if (stored == null || stored.length == 0) return 0L;

            until = stored[0] + pvpLockMs;
            pvpUntil.put(uuid, until);
        }

        long remaining = until - System.currentTimeMillis();

        if (remaining <= 0L) {
            clearPvp(player);
            return 0L;
        }

        return remaining;
    }

    /** Restzeit als lesbarer Text, oder null wenn frei. */
    public String remainingPvpLockText(Player player) {
        long remaining = remainingPvpLock(player);
        if (remaining <= 0L) return null;

        long seconds = remaining / 1000L;
        long minutes = seconds / 60L;

        if (seconds < 60L) return "in wenigen Sekunden";
        if (minutes == 1L) return "1 Minute";

        return minutes + " Minuten";
    }

    /** Uebernimmt den gespeicherten Zeitstempel nach einem Join. */
    public void handlePvpJoin(Player player) {
        if (player == null) return;

        long[] stored = player.getPersistentDataContainer().get(lastPvpKey(), PersistentDataType.LONG_ARRAY);
        if (stored == null || stored.length == 0) return;

        pvpUntil.put(player.getUniqueId(), stored[0] + pvpLockMs);
    }

    /** Vergisst die Laufzeitdaten, etwa beim Quit. */
    public void forgetPvp(Player player) {
        if (player != null) {
            pvpUntil.remove(player.getUniqueId());
        }
    }

    private void clearPvp(Player player) {
        if (player == null) return;

        pvpUntil.remove(player.getUniqueId());
        player.getPersistentDataContainer().remove(lastPvpKey());
    }

    private NamespacedKey lastPvpKey() {
        String id = server.plugin().getId();

        return new NamespacedKey(id != null && !id.isBlank() ? id : "projectplugin", "last_pvp_time");
    }

    // ------------------------------------------------------------------
    // Zonen
    // ------------------------------------------------------------------

    /** Zone, in der gar kein Schaden durchkommt. */
    public CombatManager safeZone(Predicate<Location> predicate) {
        this.safeZone = predicate == null ? location -> false : predicate;
        return this;
    }

    public boolean isSafeZone(Location location) {
        return location != null && safeZone.test(location);
    }

    /** Zone, in der nicht gekaempft werden darf. */
    public CombatManager protectedZone(Predicate<Location> predicate) {
        this.protectedZone = predicate == null ? location -> false : predicate;
        return this;
    }

    public boolean isProtectedZone(Location location) {
        return location != null && protectedZone.test(location);
    }

    public boolean isProtectedZone(org.bukkit.entity.Entity entity) {
        return entity != null && isProtectedZone(entity.getLocation());
    }

    // ------------------------------------------------------------------
    // Explosionen
    // ------------------------------------------------------------------

    /** Merkt sich, wer einen Respawn Anchor benutzt hat. */
    public void trackAnchor(Player player, Block block) {
        SelfOnlyExplosions.trackAnchor(player, block);
    }

    // ------------------------------------------------------------------
    // Texte
    // ------------------------------------------------------------------

    public CombatManager messages(BiFunction<PvpMessage, Player, Component> resolver) {
        if (resolver != null) {
            this.messageResolver = resolver;
        }
        return this;
    }

    /** Fertiger Text fuer einen Baustein, mit Rueckfall auf die Standardtexte. */
    public Component message(PvpMessage type, Player player) {
        Component result = messageResolver.apply(type, player);
        return result != null ? result : defaultMessage(type, player);
    }

    private Component defaultMessage(PvpMessage type, Player player) {
        return switch (type) {
            case SPAWN_PROTECTED -> Component.text("Du kannst am Spawn nicht kämpfen", NamedTextColor.RED);
            case SELF_PVP_OFF -> Component.text("Du hast aktuell PvP deaktiviert", NamedTextColor.RED);
            case TARGET_PVP_OFF -> Component.text("Dein Ziel hat aktuell PvP deaktiviert", NamedTextColor.RED);
            case DEATH_DISABLED -> Component.text("Du bist gestorben, PvP wurde deaktiviert!", NamedTextColor.GRAY);
            case TOGGLE_ENABLED -> Component.text("Du hast PvP für dich aktiviert!", NamedTextColor.GRAY);
            case TOGGLE_DISABLED -> Component.text("Du hast PvP für dich deaktiviert!", NamedTextColor.GRAY);
            case TOGGLE_LOCKED -> {
                String remaining = remainingPvpLockText(player);
                yield Component.text("Du kannst PvP erst wieder "
                        + (remaining != null ? remaining : "")
                        + " ändern!", NamedTextColor.RED);
            }
            case PLAYERS_ONLY -> Component.text("Nur Spieler können diesen Befehl nutzen!", NamedTextColor.RED);
            case USAGE -> Component.text("Nutze nur \"/pvp\", um PvP aus/einzuschalten!", NamedTextColor.RED);
        };
    }

    public Component message(PvpMessage type, Player player, Component fallback) {
        Component result = messageResolver.apply(type, player);
        return result != null ? result : fallback;
    }

    /** Actionbar-Hinweis aus dem Textbaustein. */
    public void actionBar(Player player, PvpMessage type) {
        if (player == null) return;

        Component text = message(type, player);
        if (text != null) {
            player.sendActionBar(text);
        }
    }

    /** Chat-Nachricht mit Plugin-Praefix aus dem Textbaustein. */
    public void chatMessage(Player player, PvpMessage type) {
        if (player == null) return;

        Component text = message(type, player);
        if (text == null) return;

        Component prefix = server.plugin().getPrefix();
        player.sendMessage((prefix != null ? prefix : Component.empty()).append(text));
    }
}
