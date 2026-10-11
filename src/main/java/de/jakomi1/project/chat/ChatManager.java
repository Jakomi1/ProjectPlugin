package de.jakomi1.project.chat;

import de.jakomi1.project.Manager;
import de.jakomi1.project.ProjectServer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Chat- sowie Beitritts- und Verlassensnachrichten.
 *
 * <p>Die Library faengt die Chat-Nachricht ab, verschickt sie neu und filtert
 * die Empfaenger; beim Join und Quit setzt sie die Meldung. Das konkrete
 * Aussehen kommt als Resolver von aussen - analog zu {@code TabManager} und
 * {@code NametagManager}. Der Manager ist standardmaessig <b>deaktiviert</b>
 * ({@link #defaultState()}) und muss bewusst eingeschaltet werden.
 */
public final class ChatManager implements Manager {

    private final ProjectServer server;

    private boolean enabled;
    private ChatListener listener;

    private BiFunction<Player, Component, Component> formatter = (player, message) -> message;
    private Predicate<Player> receiverFilter = player -> true;
    private Function<Player, Component> joinResolver;
    private Function<Player, Component> quitResolver;
    private String silentTag;

    public ChatManager(ProjectServer server) {
        this.server = server;
    }

    @Override
    public ChatManager enable() {
        if (enabled) return this;
        enabled = true;

        listener = new ChatListener(this);
        listener.register(server.plugin());

        return this;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        enabled = false;

        if (listener != null) {
            listener.unregister();
            listener = null;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean defaultState() {
        return false;
    }

    /** Baut die endgueltige Chat-Zeile aus Spieler und Original-Nachricht. */
    public ChatManager format(BiFunction<Player, Component, Component> formatter) {
        this.formatter = formatter != null ? formatter : (player, message) -> message;
        return this;
    }

    /**
     * Filter fuer die Empfaenger: {@code true} heisst, der Spieler sieht die
     * Nachricht. Der Absender sieht seine eigene Nachricht immer.
     */
    public ChatManager receiver(Predicate<Player> filter) {
        this.receiverFilter = filter != null ? filter : player -> true;
        return this;
    }

    /**
     * Traeger dieses Scoreboard-Tags sehen keine fremden Chat-Nachrichten -
     * das uebliche Verhalten des Silent-Modus. Die eigene Nachricht sieht der
     * Absender weiterhin.
     */
    public ChatManager silentTag(String tag) {
        this.silentTag = tag == null || tag.isBlank() ? null : tag;
        return this;
    }

    /** Meldung beim Betreten. {@code null} laesst die Vanilla-Meldung stehen. */
    public ChatManager join(Function<Player, Component> resolver) {
        this.joinResolver = resolver;
        return this;
    }

    /** Meldung beim Verlassen. {@code null} laesst die Vanilla-Meldung stehen. */
    public ChatManager quit(Function<Player, Component> resolver) {
        this.quitResolver = resolver;
        return this;
    }

    public Component format(Player player, Component message) {
        return formatter.apply(player, message);
    }

    public boolean visible(Player receiver) {
        if (silentTag != null && receiver != null && receiver.getScoreboardTags().contains(silentTag)) {
            return false;
        }

        return receiverFilter.test(receiver);
    }

    public Component joinMessage(Player player) {
        return joinResolver == null ? null : joinResolver.apply(player);
    }

    public Component quitMessage(Player player) {
        return quitResolver == null ? null : quitResolver.apply(player);
    }

    public ProjectServer server() {
        return server;
    }
}
