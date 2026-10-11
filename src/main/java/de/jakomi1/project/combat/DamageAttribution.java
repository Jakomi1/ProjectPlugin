package de.jakomi1.project.combat;

import org.bukkit.Bukkit;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Findet heraus, welcher Spieler einen Schaden tatsaechlich verursacht hat -
 * auch wenn der direkte Angreifer eine Entitaet ist.
 *
 * <p>Abgedeckt sind damit unter anderem normale Treffer, Pfeile, Traenke
 * (Wurf- und Schwebe-), TNT, Ketten-TNT, TNT-Minecarts, Feuerwerke, zaehme
 * Tiere sowie alles, was ueber eine {@link AreaEffectCloud} wirkt.
 *
 * <p>Zusaetzlich merkt sich die Klasse den zuletzt zugestellten Angreifer
 * kurzzeitig. Damit laesst sich auch Schaden abfangen, der erst sekunden-
 * spaeter als Folge einer PvP-Aktion entsteht (Feuer, Lava, Sturz, Gift),
 * ohne dass der Spieler mit deaktiviertem PvP darunter leidet.
 */
public final class DamageAttribution {

    /** Wie lange ein Angreifer fuer Folgeschaden als Verursacher gilt. */
    private static final long RECENT_WINDOW_MS = 5_000L;

    /** Schutz gegen Zyklen bei verketteten TNT. */
    private static final int MAX_DEPTH = 4;

    private record RecentAttack(UUID attacker, long until) {
    }

    private record Igniter(UUID player, long until) {
    }

    private static final Map<UUID, RecentAttack> recentAttacks = new ConcurrentHashMap<>();
    private static final Map<UUID, Igniter> minecartIgniters = new ConcurrentHashMap<>();

    private DamageAttribution() {
    }

    /**
     * Prueft ein Schadensereignis auf einen verantwortlichen Spieler.
     *
     * @return der verantwortliche Spieler oder null, wenn kein Spieler
     *         beteiligt ist (Mob, Umwelt, ...)
     */
    public static Player resolve(EntityDamageEvent event) {
        if (event == null) return null;

        var source = event.getDamageSource();

        Player direct = fromEntity(source.getDirectEntity(), 0);
        if (direct != null) return direct;

        Player causing = fromEntity(source.getCausingEntity(), 0);
        if (causing != null) return causing;

        if (event instanceof EntityDamageByEntityEvent byEntity) {
            return fromEntity(byEntity.getDamager(), 0);
        }

        return null;
    }

    /**
     * Merkt sich einen Angreifer fuer moegliche Folgeschaden. Wird nur
     * aufgerufen, wenn der Treffer durch die PvP-Regeln tatsaechlich erlaubt
     * wurde.
     */
    public static void remember(Player attacker, Player victim) {
        if (attacker == null || victim == null || attacker.equals(victim)) return;

        recentAttacks.put(victim.getUniqueId(), new RecentAttack(attacker.getUniqueId(),
                System.currentTimeMillis() + RECENT_WINDOW_MS));
    }

    /**
     * Letzter Angreifer innerhalb des Zeitfensters.
     *
     * @return null wenn keiner gemerkt wurde oder der Spieler offline ist
     */
    public static Player recentAttacker(Player victim) {
        if (victim == null) return null;

        RecentAttack attack = recentAttacks.get(victim.getUniqueId());
        if (attack == null) return null;

        if (attack.until() <= System.currentTimeMillis()) {
            recentAttacks.remove(victim.getUniqueId());
            return null;
        }

        return Bukkit.getPlayer(attack.attacker());
    }

    public static void forget(Player player) {
        if (player == null) return;

        recentAttacks.remove(player.getUniqueId());
    }

    /**
     * Eine TNT-Minecart liefert ueber die API keine Quelle. Wer sie gezuendet
     * hat, wird deshalb hier festgehalten.
     */
    public static void rememberMinecartIgniter(Entity minecart, Player player) {
        if (minecart == null || player == null) return;

        minecartIgniters.put(minecart.getUniqueId(), new Igniter(player.getUniqueId(),
                System.currentTimeMillis() + 30_000L));
    }

    private static Player fromEntity(Entity entity, int depth) {
        if (entity == null || depth > MAX_DEPTH) return null;

        if (entity instanceof Player player) return player;

        if (entity instanceof Tameable tameable && tameable.getOwner() instanceof Player owner) return owner;

        if (entity instanceof Projectile projectile) return fromSource(projectile.getShooter(), depth + 1);

        if (entity instanceof TNTPrimed tnt) return fromEntity(tnt.getSource(), depth + 1);

        if (entity instanceof ExplosiveMinecart cart) return fromMinecart(cart, depth);

        if (entity instanceof AreaEffectCloud cloud) return fromSource(cloud.getSource(), depth + 1);

        return null;
    }

    private static Player fromMinecart(ExplosiveMinecart cart, int depth) {
        for (Entity passenger : cart.getPassengers()) {
            if (passenger instanceof Player player) return player;
        }

        Igniter igniter = minecartIgniters.get(cart.getUniqueId());
        if (igniter == null) return null;

        if (igniter.until() <= System.currentTimeMillis()) {
            minecartIgniters.remove(cart.getUniqueId());
            return null;
        }

        return Bukkit.getPlayer(igniter.player());
    }

    private static Player fromSource(ProjectileSource source, int depth) {
        if (source == null || depth > MAX_DEPTH) return null;

        if (source instanceof Player player) return player;
        if (source instanceof Entity entity) return fromEntity(entity, depth + 1);

        return null;
    }
}
